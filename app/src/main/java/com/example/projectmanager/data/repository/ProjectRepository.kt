package com.example.projectmanager.data.repository

import com.example.projectmanager.data.local.dao.ProjectDao
import com.example.projectmanager.data.local.entity.ProjectEntity
import com.example.projectmanager.data.model.Project
import com.example.projectmanager.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface ProjectRepository : BaseRepository<Project> {
    fun getProjectsByUser(userId: String): Flow<Resource<List<Project>>>
    fun getProjectById(projectId: String): Flow<Project?>
    suspend fun createProject(project: Project): Resource<Project>
    suspend fun updateProject(project: Project): Resource<Project>
    suspend fun deleteProject(projectId: String): Resource<Unit>
    fun getRecentProjects(limit: Int): Flow<List<Project>>
    suspend fun addMemberToProject(projectId: String, userId: String): Resource<Unit>
    suspend fun removeMemberFromProject(projectId: String, userId: String): Resource<Unit>
}

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val firestore: FirebaseFirestore
) : ProjectRepository {

    private val projectsCollection = firestore.collection("projects")

    override fun getProjectsByUser(userId: String): Flow<Resource<List<Project>>> = flow {
        try {
            val projects = projectsCollection
                .whereArrayContains("members", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Project::class.java) }
            
            // Update local cache
            projectDao.insertProjects(projects.map { ProjectEntity.fromDomain(it) })
            
            // Emit from local database
            projectDao.getProjectsByUser(userId).collect { entities ->
                emit(Resource.success(entities.map { it.toDomain() }))
            }
        } catch (e: Exception) {
            // If remote fetch fails, emit from local cache
            projectDao.getProjectsByUser(userId).collect { entities ->
                emit(Resource.success(entities.map { it.toDomain() }))
            }
        }
    }

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
            
            projectDao.getProjectById(projectId).collect { entity ->
                emit(entity?.toDomain())
            }
        } catch (e: Exception) {
            projectDao.getProjectById(projectId).collect { entity ->
                emit(entity?.toDomain())
            }
        }
    }

    override suspend fun createProject(project: Project): Resource<Project> = try {
        val projectRef = projectsCollection.document()
        val newProject = project.copy(id = projectRef.id)
        projectRef.set(newProject).await()
        projectDao.insertProject(ProjectEntity.fromDomain(newProject))
        Resource.success(newProject)
    } catch (e: Exception) {
        Resource.error(e.message ?: "Failed to create project")
    }

    override suspend fun updateProject(project: Project): Resource<Project> = try {
        projectsCollection.document(project.id).set(project).await()
        projectDao.insertProject(ProjectEntity.fromDomain(project))
        Resource.success(project)
    } catch (e: Exception) {
        Resource.error(e.message ?: "Failed to update project")
    }

    override suspend fun deleteProject(projectId: String): Resource<Unit> = try {
        projectsCollection.document(projectId).delete().await()
        projectDao.deleteProjectById(projectId)
        Resource.success(Unit)
    } catch (e: Exception) {
        Resource.error(e.message ?: "Failed to delete project")
    }

    override fun getRecentProjects(limit: Int): Flow<List<Project>> = flow {
        try {
            val projects = projectsCollection
                .orderBy("updatedAt")
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Project::class.java) }
            
            projectDao.insertProjects(projects.map { ProjectEntity.fromDomain(it) })
            
            projectDao.getRecentProjects(limit).collect { entities ->
                emit(entities.map { it.toDomain() })
            }
        } catch (e: Exception) {
            projectDao.getRecentProjects(limit).collect { entities ->
                emit(entities.map { it.toDomain() })
            }
        }
    }

    override suspend fun addMemberToProject(projectId: String, userId: String): Resource<Unit> = try {
        val project = projectsCollection.document(projectId).get().await()
            .toObject(Project::class.java) ?: throw Exception("Project not found")
        
        if (!project.members.contains(userId)) {
            val updatedMembers = project.members + userId
            projectsCollection.document(projectId)
                .update("members", updatedMembers)
                .await()
        }
        Resource.success(Unit)
    } catch (e: Exception) {
        Resource.error(e.message ?: "Failed to add member to project")
    }

    override suspend fun removeMemberFromProject(projectId: String, userId: String): Resource<Unit> = try {
        val project = projectsCollection.document(projectId).get().await()
            .toObject(Project::class.java) ?: throw Exception("Project not found")
        
        if (project.members.contains(userId)) {
            val updatedMembers = project.members - userId
            projectsCollection.document(projectId)
                .update("members", updatedMembers)
                .await()
        }
        Resource.success(Unit)
    } catch (e: Exception) {
        Resource.error(e.message ?: "Failed to remove member from project")
    }
}