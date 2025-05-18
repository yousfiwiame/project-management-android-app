package com.example.projectmanager.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmanager.data.model.Project
import com.example.projectmanager.data.model.Task
import com.example.projectmanager.data.model.User
import com.example.projectmanager.data.repository.ProjectRepository
import com.example.projectmanager.data.repository.TaskRepository
import com.example.projectmanager.data.repository.UserRepository
import com.example.projectmanager.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectUiState(
    val project: Project? = null,
    val tasks: List<Task> = emptyList(),
    val members: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                projectRepository.getProjectById(projectId).collect { project ->
                    if (project != null) {
                        _uiState.update { it.copy(project = project) }
                        loadProjectTasks(project.id)
                        loadProjectMembers(project)
                    } else {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = "Project not found"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load project"
                    )
                }
            }
        }
    }

    private fun loadProjectTasks(projectId: String) {
        viewModelScope.launch {
            try {
                taskRepository.getTasksByProject(projectId).collect { tasks ->
                    _uiState.update {
                        it.copy(
                            tasks = tasks,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load project tasks"
                    )
                }
            }
        }
    }

    private fun loadProjectMembers(project: Project) {
        viewModelScope.launch {
            try {
                // Get users for the project members list
                val members = project.members.mapNotNull { memberId ->
                    userRepository.getCurrentUser().map { resource ->
                        when (resource) {
                            is Resource.Success -> resource.data
                            else -> null
                        }
                    }.firstOrNull()
                }
                
                _uiState.update {
                    it.copy(
                        members = members,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load project members"
                    )
                }
            }
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val result = projectRepository.updateProject(project)
                when (result) {
                    is Resource.Success -> {
                        loadProject(project.id)
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to update project"
                    )
                }
            }
        }
    }
} 