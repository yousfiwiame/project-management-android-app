package com.example.projectmanager.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmanager.data.model.Project
import com.example.projectmanager.data.repository.ProjectRepository
import com.example.projectmanager.data.repository.UserRepository
import com.example.projectmanager.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
    }

    fun refresh() {
        loadProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                userRepository.getCurrentUser().collectLatest { userResource ->
                    when (userResource) {
                        is Resource.Success -> {
                            userResource.data?.let { user ->
                                projectRepository.getProjectsByUser(user.id)
                                    .collect { projects ->
                                        _uiState.update { 
                                            it.copy(
                                                projects = projects,
                                                isLoading = false,
                                                error = null
                                            )
                                        }
                                    }
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = userResource.message
                                )
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load projects"
                    )
                }
            }
        }
    }

    fun createProject(project: Project) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                userRepository.getCurrentUser().collectLatest { userResource ->
                    when (userResource) {
                        is Resource.Success -> {
                            userResource.data?.let { user ->
                                val newProject = project.copy(
                                    ownerId = user.id,
                                    members = listOf(user.id)
                                )
                                
                                when (val result = projectRepository.createProject(newProject)) {
                                    is Resource.Success -> {
                                        loadProjects() // Refresh the projects list
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
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = userResource.message
                                )
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to create project"
                    )
                }
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                when (val result = projectRepository.deleteProject(projectId)) {
                    is Resource.Success -> {
                        loadProjects() // Refresh the projects list
                    }
                    is Resource.Error -> {
                        _uiState.update { 
                            it.copy(error = result.message)
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to delete project")
                }
            }
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            try {
                when (val result = projectRepository.updateProject(project)) {
                    is Resource.Success -> {
                        loadProjects() // Refresh the projects list
                    }
                    is Resource.Error -> {
                        _uiState.update { 
                            it.copy(error = result.message)
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to update project")
                }
            }
        }
    }
} 