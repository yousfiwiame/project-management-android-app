package com.example.projectmanager.data.repository

import com.example.projectmanager.data.local.dao.UserDao
import com.example.projectmanager.data.local.entity.UserEntity
import com.example.projectmanager.data.model.User
import com.example.projectmanager.data.remote.firebase.FirebaseAuthSource
import com.example.projectmanager.data.remote.firebase.FirestoreUserSource
import com.example.projectmanager.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    fun getCurrentUser(): Flow<Resource<User>>
    fun observeAuthState(): Flow<FirebaseUser?>
    suspend fun signIn(email: String, password: String): Resource<User>
    suspend fun signUp(email: String, password: String, displayName: String): Resource<User>
    suspend fun signOut()
    suspend fun resetPassword(email: String): Resource<Unit>
    suspend fun updateProfile(updates: Map<String, Any>): Resource<User>
    suspend fun deleteAccount(): Resource<Unit>
    suspend fun verifyEmail(): Resource<Unit>
    suspend fun updateUserPreferences(updates: Map<String, Any>): Resource<Unit>
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val firebaseAuthSource: FirebaseAuthSource,
    private val firestoreUserSource: FirestoreUserSource,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    val currentUser: User?
        get() {
            val firebaseUser = firebaseAuthSource.currentUser ?: return null
            return User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                photoUrl = firebaseUser.photoUrl?.toString()
            )
        }

    override fun getCurrentUser(): Flow<Resource<User>> = flow {
        emit(Resource.loading())
        try {
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                val userDoc = usersCollection.document(firebaseUser.uid).get().await()
                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    emit(Resource.success(user))
                } else {
                    emit(Resource.error("User data not found"))
                }
            } else {
                emit(Resource.error("No authenticated user"))
            }
        } catch (e: Exception) {
            emit(Resource.error(e.message ?: "Failed to get current user"))
        }
    }

    override fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    override suspend fun signIn(email: String, password: String): Resource<User> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user?.let { firebaseUser ->
            val userDoc = usersCollection.document(firebaseUser.uid).get().await()
            val user = userDoc.toObject(User::class.java)
            if (user != null) {
                // Update last login time
                usersCollection.document(firebaseUser.uid)
                    .update("lastLoginAt", com.google.firebase.Timestamp.now())
                    .await()
                Resource.success(user)
            } else {
                Resource.error("User data not found")
            }
        } ?: Resource.error("Sign in failed")
    } catch (e: Exception) {
        Resource.error(e.message ?: "Sign in failed")
    }

    override suspend fun signUp(
        email: String, 
        password: String, 
        displayName: String
    ): Resource<User> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.let { firebaseUser ->
            val user = User(
                id = firebaseUser.uid,
                email = email,
                displayName = displayName
            )
            usersCollection.document(firebaseUser.uid).set(user).await()
            Resource.success(user)
        } ?: Resource.error("Sign up failed")
    } catch (e: Exception) {
        Resource.error(e.message ?: "Sign up failed")
    }

    override suspend fun signOut() {
        firebaseAuthSource.signOut()
        // Clear local user data
        // This would be done in a transaction to ensure atomicity
    }

    fun getProjectMembers(projectId: String): Flow<List<User>> {
        return userDao.getUsersByProjectId(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun updateUserProfile(user: User) {
        firestoreUserSource.updateUser(user)
        userDao.insertUser(UserEntity.fromDomain(user))
    }

    suspend fun updateUserSkills(userId: String, skills: List<String>) {
        val user = firestoreUserSource.getUser(userId)
        val updatedUser = user.copy(skills = skills)
        firestoreUserSource.updateUser(updatedUser)
        userDao.insertUser(UserEntity.fromDomain(updatedUser))
    }

    override suspend fun resetPassword(email: String): Resource<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Resource.success(Unit)
    } catch (e: Exception) {
        Resource.error(e.message ?: "Failed to send reset password email")
    }

    override suspend fun updateProfile(updates: Map<String, Any>): Resource<User> = try {
        val currentUser = auth.currentUser ?: throw Exception("No authenticated user")
        usersCollection.document(currentUser.uid).update(updates).await()
        val updatedDoc = usersCollection.document(currentUser.uid).get().await()
        val updatedUser = updatedDoc.toObject(User::class.java)
        if (updatedUser != null) {
            Resource.success(updatedUser)
        } else {
            Resource.error("Failed to get updated user data")
        }
    } catch (e: Exception) {
        Resource.error(e.message ?: "Failed to update profile")
    }

    override suspend fun deleteAccount(): Resource<Unit> = try {
        val currentUser = auth.currentUser ?: throw Exception("No authenticated user")
        usersCollection.document(currentUser.uid).delete().await()
        currentUser.delete().await()
        Resource.success(Unit)
    } catch (e: Exception) {
        Resource.error(e.message ?: "Failed to delete account")
    }

    override suspend fun verifyEmail(): Resource<Unit> = try {
        val currentUser = auth.currentUser ?: throw Exception("No authenticated user")
        currentUser.sendEmailVerification().await()
        Resource.success(Unit)
    } catch (e: Exception) {
        Resource.error(e.message ?: "Failed to send verification email")
    }

    override suspend fun updateUserPreferences(updates: Map<String, Any>): Resource<Unit> = try {
        val currentUser = auth.currentUser ?: throw Exception("No authenticated user")
        usersCollection.document(currentUser.uid)
            .update(updates.mapKeys { "preferences.${it.key}" })
            .await()
        Resource.success(Unit)
    } catch (e: Exception) {
        Resource.error(e.message ?: "Failed to update preferences")
    }
}