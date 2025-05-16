package com.example.projectmanager.ui.project.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmanager.data.model.Task
import com.example.projectmanager.data.model.TaskStatus
import com.example.projectmanager.data.repository.ProjectRepository
import com.example.projectmanager.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class ProjectTimelineUiState(
    val tasks: List<Task> = emptyList(),
    val timelineStartDate: Date = Date(),
    val timelineEndDate: Date = Date(),
    val daysToShow: Int = 30,
    val zoomLevel: Float = 1f,
    val filter: TimelineFilter = TimelineFilter(),
    val sort: TimelineSort = TimelineSort(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TimelineFilter(
    val statuses: Set<TaskStatus> = TaskStatus.values().toSet(),
    val assignedToMe: Boolean = false,
    val dueDateRange: ClosedRange<Date>? = null
)

data class TimelineSort(
    val option: TimelineSortOption = TimelineSortOption.START_DATE,
    val ascending: Boolean = true
)

enum class TimelineSortOption(val displayName: String) {
    START_DATE("Start Date"),
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    STATUS("Status"),
    TITLE("Title")
}

@HiltViewModel
class ProjectTimelineViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectTimelineUiState())
    val uiState: StateFlow<ProjectTimelineUiState> = _uiState.asStateFlow()

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            projectRepository.getProjectWithTasks(projectId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val project = result.data
                        val tasks = project.tasks
                        val timelineStartDate = tasks.minOfOrNull { it.startDate } ?: Date()
                        val timelineEndDate = tasks.maxOfOrNull { it.dueDate } ?: Date()

                        _uiState.update {
                            it.copy(
                                tasks = applyFilterAndSort(tasks),
                                timelineStartDate = timelineStartDate,
                                timelineEndDate = timelineEndDate,
                                daysToShow = calculateDaysToShow(timelineStartDate, timelineEndDate),
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _uiState.update {
                            it.copy(isLoading = true)
                        }
                    }
                }
            }
        }
    }

    fun updateFilter(filter: TimelineFilter) {
        _uiState.update {
            it.copy(
                filter = filter,
                tasks = applyFilterAndSort(it.tasks)
            )
        }
    }

    fun updateSort(sort: TimelineSort) {
        _uiState.update {
            it.copy(
                sort = sort,
                tasks = applyFilterAndSort(it.tasks)
            )
        }
    }

    fun zoomIn() {
        _uiState.update {
            it.copy(
                zoomLevel = (it.zoomLevel * 1.2f).coerceAtMost(2f),
                daysToShow = (it.daysToShow / 1.2f).toInt().coerceAtLeast(7)
            )
        }
    }

    fun zoomOut() {
        _uiState.update {
            it.copy(
                zoomLevel = (it.zoomLevel / 1.2f).coerceAtLeast(0.5f),
                daysToShow = (it.daysToShow * 1.2f).toInt().coerceAtMost(90)
            )
        }
    }

    fun scrollToToday() {
        val today = Date()
        _uiState.update {
            it.copy(
                timelineStartDate = today,
                timelineEndDate = Calendar.getInstance().apply {
                    time = today
                    add(Calendar.DAY_OF_MONTH, it.daysToShow)
                }.time
            )
        }
    }

    private fun applyFilterAndSort(tasks: List<Task>): List<Task> {
        val filtered = tasks.filter { task ->
            val statusMatch = task.status in uiState.value.filter.statuses
            val assigneeMatch = if (uiState.value.filter.assignedToMe) {
                task.assignedTo.contains(getCurrentUserId())
            } else {
                true
            }
            val dateMatch = uiState.value.filter.dueDateRange?.let { range ->
                task.dueDate?.let { it in range } ?: false
            } ?: true

            statusMatch && assigneeMatch && dateMatch
        }

        return when (uiState.value.sort.option) {
            TimelineSortOption.START_DATE -> filtered.sortedBy { it.startDate }
            TimelineSortOption.DUE_DATE -> filtered.sortedBy { it.dueDate }
            TimelineSortOption.PRIORITY -> filtered.sortedBy { it.priority }
            TimelineSortOption.STATUS -> filtered.sortedBy { it.status }
            TimelineSortOption.TITLE -> filtered.sortedBy { it.title }
        }.let {
            if (uiState.value.sort.ascending) it else it.reversed()
        }
    }

    private fun calculateDaysToShow(start: Date, end: Date): Int {
        return ((end.time - start.time) / (1000 * 60 * 60 * 24)).toInt().coerceIn(7, 90)
    }

    private fun getCurrentUserId(): String {
        // Implement getting current user ID from UserRepository
        return ""
    }
} 