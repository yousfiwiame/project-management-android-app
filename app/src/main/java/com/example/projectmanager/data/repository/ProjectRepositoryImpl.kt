package com.example.projectmanager.data.repository

import com.example.projectmanager.data.model.Project
import com.example.projectmanager.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProjectRepository {
    
    private val projectsCollection = firestore.collection("projects")

    override suspend fun create(item: Project): Resource<Project> = try {
        val documentRef = if (item.id.isNotEmpty()) {
            projectsCollection.document(item.id)
        } else {
            projectsCollection.document()
        }
        val project = item.copy(id = documentRef.id)
        documentRef.set(project).await()
        Resource.success(project)
    } catch (e: Exception) {
        Resource.error("Failed to create project: ${e.message}", e)
    }

    override suspend fun update(item: Project): Resource<Project> = try {
        projectsCollection.document(item.id).set(item).await()
        Resource.success(item)
    } catch (e: Exception) {
        Resource.error("Failed to update project: ${e.message}", e)
    }

    override suspend fun delete(id: String): Resource<Boolean> = try {
        projectsCollection.document(id).delete().await()
        Resource.success(true)
    } catch (e: Exception) {
        Resource.error("Failed to delete project: ${e.message}", e)
    }

    override suspend fun get(id: String): Resource<Project> = try {
        val document = projectsCollection.document(id).get().await()
        document.toObject(Project::class.java)?.let {
            Resource.success(it)
        } ?: Resource.error("Project not found")
    } catch (e: Exception) {
        Resource.error("Failed to get project: ${e.message}", e)
    }

    override fun getAll(): Flow<Resource<List<Project>>> = callbackFlow {
        trySend(Resource.loading())
        
        val subscription = projectsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.error("Failed to get projects: ${error.message}", error))
                    return@addSnapshotListener
                }

                val projects = snapshot?.documents?.mapNotNull {
                    it.toObject(Project::class.java)
                } ?: emptyList()
                
                trySend(Resource.success(projects))
            }

        awaitClose { subscription.remove() }
    }

    override fun getStream(id: String): Flow<Resource<Project>> = callbackFlow {
        trySend(Resource.loading())
        
        val subscription = projectsCollection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.error("Failed to get project: ${error.message}", error))
                    return@addSnapshotListener
                }

                val project = snapshot?.toObject(Project::class.java)
                if (project != null) {
                    trySend(Resource.success(project))
                } else {
                    trySend(Resource.error("Project not found"))
                }
            }

        awaitClose { subscription.remove() }
    }

    override fun getProjectsByUser(userId: String): Flow<Resource<List<Project>>> = callbackFlow {
        trySend(Resource.loading())
        
        val subscription = projectsCollection
            .whereArrayContains("members", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.error("Failed to get user projects: ${error.message}", error))
                    return@addSnapshotListener
                }

                val projects = snapshot?.documents?.mapNotNull {
                    it.toObject(Project::class.java)
                } ?: emptyList()
                
                trySend(Resource.success(projects))
            }

        awaitClose { subscription.remove() }
    }
} 