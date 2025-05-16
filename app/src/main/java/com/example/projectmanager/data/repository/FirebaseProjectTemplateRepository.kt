package com.example.projectmanager.data.repository

import com.example.projectmanager.data.model.ProjectTemplate
import com.example.projectmanager.data.model.Project
import com.example.projectmanager.data.model.TaskTemplate
import com.example.projectmanager.data.model.MilestoneTemplate
import com.example.projectmanager.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseProjectTemplateRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val projectRepository: ProjectRepository
) : ProjectTemplateRepository {

    private val templatesCollection = firestore.collection("templates")

    override suspend fun getTemplates(): Flow<Resource<List<ProjectTemplate>>> = flow {
        try {
            emit(Resource.Loading())
            val snapshot = templatesCollection.get().await()
            val templates = snapshot.documents.mapNotNull { it.toObject<ProjectTemplate>() }
            emit(Resource.Success(templates))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load templates"))
        }
    }

    override suspend fun getTemplate(id: String): Resource<ProjectTemplate> {
        return try {
            val document = templatesCollection.document(id).get().await()
            document.toObject<ProjectTemplate>()?.let {
                Resource.Success(it)
            } ?: Resource.Error("Template not found")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load template")
        }
    }

    override suspend fun createTemplate(template: ProjectTemplate): Resource<ProjectTemplate> {
        return try {
            val documentRef = templatesCollection.document()
            val templateWithId = template.copy(id = documentRef.id)
            documentRef.set(templateWithId).await()
            Resource.Success(templateWithId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create template")
        }
    }

    override suspend fun updateTemplate(template: ProjectTemplate): Resource<ProjectTemplate> {
        return try {
            templatesCollection.document(template.id).set(template).await()
            Resource.Success(template)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update template")
        }
    }

    override suspend fun deleteTemplate(id: String): Resource<Unit> {
        return try {
            templatesCollection.document(id).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete template")
        }
    }

    override suspend fun createTemplateFromProject(projectId: String): Resource<ProjectTemplate> {
        return try {
            // Get the project
            when (val projectResult = projectRepository.getProject(projectId)) {
                is Resource.Success -> {
                    val project = projectResult.data
                    
                    // Convert project tasks to task templates
                    val taskTemplates = project.tasks.map { task ->
                        TaskTemplate(
                            title = task.title,
                            description = task.description,
                            priority = task.priority,
                            estimatedHours = task.estimatedHours ?: 0f,
                            dependencies = task.dependencies.map { it.dependentTaskId },
                            order = task.order
                        )
                    }

                    // Convert project milestones to milestone templates
                    val milestoneTemplates = project.milestones.map { milestone ->
                        MilestoneTemplate(
                            title = milestone.title,
                            description = milestone.description,
                            relativeDeadline = 0 // Calculate based on project start date
                        )
                    }

                    // Create the template
                    val template = ProjectTemplate(
                        name = "${project.name} Template",
                        description = project.description,
                        category = project.tags.firstOrNull() ?: "",
                        tasks = taskTemplates,
                        milestones = milestoneTemplates,
                        estimatedDuration = project.estimatedHours.toInt() / 8 // Convert hours to days
                    )

                    createTemplate(template)
                }
                is Resource.Error -> Resource.Error(projectResult.message ?: "Failed to load project")
                else -> Resource.Error("Failed to load project")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create template from project")
        }
    }
} 