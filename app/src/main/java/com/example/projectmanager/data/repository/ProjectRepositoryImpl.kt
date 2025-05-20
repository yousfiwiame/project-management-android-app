package com.example.projectmanager.data.repository

import com.example.projectmanager.data.local.dao.ProjectDao
import com.example.projectmanager.data.local.entity.ProjectEntity
import com.example.projectmanager.data.model.Project
import com.example.projectmanager.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val firestore: FirebaseFirestore
) : ProjectRepository {
    
    private val projectsCollection = firestore.collection("projects")

    override fun getProjectById(projectId: String): Flow<Project?> = flow {
        try {
            val project = projectsCollection
                .document(projectId)
                .get()
                .await()
                .toObject(Project::class.java)
            
            project?.let {
                projectDao.insertProject(ProjectEntity.fromDomain(it))
            }
            
            emit(project)
        } catch (e: Exception) {
            Timber.e(e, "Error getting project by ID")
            emit(null)
        }
    }

    override suspend fun createProject(project: Project): Resource<Project> {
        return create(project)
    }

    override suspend fun updateProject(project: Project): Resource<Project> {
        return update(project)
    }

    override suspend fun deleteProject(projectId: String): Resource<Unit> = try {
        projectsCollection.document(projectId).delete().await()
        projectDao.deleteProjectById(projectId)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to delete project")
    }

    override fun getRecentProjects(limit: Int): Flow<List<Project>> = flow {
        try {
            val projects = projectsCollection
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Project::class.java) }
            emit(projects)
        } catch (e: Exception) {
            Timber.e(e, "Error getting recent projects")
            emit(emptyList())
        }
    }

    override suspend fun create(item: Project): Resource<Project> = try {
        val projectRef = if (item.id.isNotBlank()) {
            projectsCollection.document(item.id)
        } else {
            projectsCollection.document()
        }
        val newProject = item.copy(id = projectRef.id)
        projectRef.set(newProject).await()
        projectDao.insertProject(ProjectEntity.fromDomain(newProject))
        Resource.Success(newProject)
    } catch (e: Exception) {
        Resource.Error("Failed to create project: ${e.message}")
    }

    override suspend fun update(item: Project): Resource<Project> = try {
        projectsCollection.document(item.id).set(item).await()
        projectDao.insertProject(ProjectEntity.fromDomain(item))
        Resource.Success(item)
    } catch (e: Exception) {
        Resource.Error("Failed to update project: ${e.message}")
    }

    override suspend fun delete(id: String): Resource<Boolean> = try {
        projectsCollection.document(id).delete().await()
        projectDao.deleteProjectById(id)
        Resource.Success(true)
    } catch (e: Exception) {
        Resource.Error("Failed to delete project: ${e.message}")
    }

    override suspend fun get(id: String): Resource<Project> = try {
        val document = projectsCollection.document(id).get().await()
        document.toObject(Project::class.java)?.let {
            Resource.Success(it)
        } ?: Resource.Error("Project not found")
    } catch (e: Exception) {
        Resource.Error("Failed to get project: ${e.message}")
    }

    override fun getAll(): Flow<Resource<List<Project>>> = callbackFlow {
        trySend(Resource.Loading)
        
        val subscription = projectsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error("Failed to get projects: ${error.message}"))
                    return@addSnapshotListener
                }

                val projects = snapshot?.documents?.mapNotNull {
                    it.toObject(Project::class.java)
                } ?: emptyList()
                
                trySend(Resource.Success(projects))
            }

        awaitClose { subscription.remove() }
    }

    override fun getProjectsByUser(userId: String): Flow<Resource<List<Project>>> = callbackFlow {
        trySend(Resource.Loading)
        
        val subscription = projectsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error("Failed to get user projects: ${error.message}"))
                    return@addSnapshotListener
                }

                val allProjects = snapshot?.documents?.mapNotNull {
                    it.toObject(Project::class.java)
                } ?: emptyList()
                
                // Filter projects where the user is a member
                val userProjects = allProjects.filter { project ->
                    project.members.any { member -> member.userId == userId }
                }
                
                // Update local cache
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        projectDao.insertProjects(userProjects.map { ProjectEntity.fromDomain(it) })
                    } catch (e: Exception) {
                        Timber.e(e, "Error updating local cache")
                    }
                }
                
                trySend(Resource.Success(userProjects))
            }

        awaitClose { subscription.remove() }
    }

    override fun getStream(id: String): Flow<Resource<Project>> = callbackFlow {
        trySend(Resource.Loading)
        
        val subscription = projectsCollection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error("Failed to get project: ${error.message}"))
                    return@addSnapshotListener
                }

                val project = snapshot?.toObject(Project::class.java)
                if (project != null) {
                    trySend(Resource.Success(project))
                } else {
                    trySend(Resource.Error("Project not found"))
                }
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun addMemberToProject(projectId: String, userId: String): Resource<Unit> {
        try {
            val project = projectsCollection.document(projectId).get().await().toObject(Project::class.java)
                ?: return Resource.Error("Project not found")
            
            // Check if user is already a member
            if (project.members.any { it.userId == userId }) {
                return Resource.Success(Unit) // User is already a member
            }
            
            // Create a new ProjectMember object
            val newMember = com.example.projectmanager.data.model.ProjectMember(
                userId = userId,
                role = com.example.projectmanager.data.model.ProjectRole.MEMBER,
                joinedAt = java.util.Date()
            )
            
            val updatedMembers = project.members.toMutableList().apply { add(newMember) }
            projectsCollection.document(projectId)
                .update("members", updatedMembers)
                .await()
            
            return Resource.Success(Unit)
        } catch (e: Exception) {
            return Resource.Error(e.message ?: "Failed to add member")
        }
    }

    override suspend fun removeMemberFromProject(projectId: String, userId: String): Resource<Unit> {
        try {
            val project = projectsCollection.document(projectId).get().await().toObject(Project::class.java)
                ?: return Resource.Error("Project not found")
            
            val updatedMembers = project.members.filterNot { it.userId == userId }
            projectsCollection.document(projectId)
                .update("members", updatedMembers)
                .await()
            
            return Resource.Success(Unit)
        } catch (e: Exception) {
            return Resource.Error(e.message ?: "Failed to remove member")
        }
    }

    override suspend fun syncProjects() {
        try {
            val projects = projectsCollection
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Project::class.java) }
            
            projectDao.insertProjects(projects.map { ProjectEntity.fromDomain(it) })
            Timber.d("Synced ${projects.size} projects")
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync projects")
        }
    }

    override suspend fun syncProject(projectId: String) {
        try {
            val project = projectsCollection
                .document(projectId)
                .get()
                .await()
                .toObject(Project::class.java)
            
            project?.let {
                projectDao.insertProject(ProjectEntity.fromDomain(it))
                Timber.d("Synced project ${it.id}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync project $projectId")
        }
    }
} 