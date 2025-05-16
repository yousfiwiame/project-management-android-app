package com.example.projectmanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import java.util.*
import kotlin.math.roundToInt

data class GanttTask(
    val id: String,
    val title: String,
    val startDate: Date,
    val endDate: Date,
    val progress: Float,
    val dependencies: List<String> = emptyList(),
    val color: Color? = null
)

data class GanttChartState(
    val tasks: List<GanttTask>,
    val startDate: Date,
    val endDate: Date,
    val daysToShow: Int,
    val rowHeight: Float = 40f,
    val dayWidth: Float = 30f
)

@Composable
fun GanttChart(
    state: GanttChartState,
    modifier: Modifier = Modifier,
    onTaskClick: (GanttTask) -> Unit = {}
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val minScale = 0.5f
    val maxScale = 2f

    BoxWithConstraints(modifier = modifier) {
        val chartWidth = maxWidth.toPx()
        val chartHeight = maxHeight.toPx()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(minScale, maxScale)
                        offset += pan
                    }
                }
        ) {
            // Draw background grid
            drawGrid(state, scale, offset)

            // Draw timeline
            drawTimeline(state, scale, offset)

            // Draw tasks
            state.tasks.forEachIndexed { index, task ->
                drawTask(
                    task = task,
                    index = index,
                    state = state,
                    scale = scale,
                    offset = offset
                )
            }

            // Draw dependencies
            drawDependencies(state, scale, offset)
        }
    }
}

private fun DrawScope.drawGrid(
    state: GanttChartState,
    scale: Float,
    offset: Offset
) {
    val gridColor = Color.LightGray.copy(alpha = 0.3f)
    
    // Vertical lines for days
    for (day in 0..state.daysToShow) {
        val x = day * state.dayWidth * scale + offset.x
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
    }

    // Horizontal lines for tasks
    for (row in 0..state.tasks.size) {
        val y = row * state.rowHeight * scale + offset.y
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawTimeline(
    state: GanttChartState,
    scale: Float,
    offset: Offset
) {
    val calendar = Calendar.getInstance()
    calendar.time = state.startDate

    for (day in 0..state.daysToShow) {
        val x = day * state.dayWidth * scale + offset.x
        val isFirstOfMonth = calendar.get(Calendar.DAY_OF_MONTH) == 1

        if (isFirstOfMonth) {
            drawRect(
                color = Color.LightGray.copy(alpha = 0.2f),
                topLeft = Offset(x, 0f),
                size = Size(state.dayWidth * scale, size.height)
            )
        }

        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }
}

private fun DrawScope.drawTask(
    task: GanttTask,
    index: Int,
    state: GanttChartState,
    scale: Float,
    offset: Offset
) {
    val calendar = Calendar.getInstance()
    calendar.time = state.startDate

    val daysBetween = ((task.startDate.time - state.startDate.time) / (1000 * 60 * 60 * 24)).toInt()
    val taskDuration = ((task.endDate.time - task.startDate.time) / (1000 * 60 * 60 * 24)).toInt()

    val x = daysBetween * state.dayWidth * scale + offset.x
    val y = index * state.rowHeight * scale + offset.y
    val width = taskDuration * state.dayWidth * scale
    val height = state.rowHeight * scale * 0.8f

    // Draw task background
    drawRoundRect(
        color = task.color ?: MaterialTheme.colorScheme.primary,
        topLeft = Offset(x, y + height * 0.1f),
        size = Size(width, height),
        cornerRadius = CornerRadius(4f, 4f),
        alpha = 0.2f
    )

    // Draw progress bar
    drawRoundRect(
        color = task.color ?: MaterialTheme.colorScheme.primary,
        topLeft = Offset(x, y + height * 0.1f),
        size = Size(width * task.progress, height),
        cornerRadius = CornerRadius(4f, 4f)
    )
}

private fun DrawScope.drawDependencies(
    state: GanttChartState,
    scale: Float,
    offset: Offset
) {
    val taskMap = state.tasks.associateBy { it.id }
    
    state.tasks.forEach { task ->
        task.dependencies.forEach { dependencyId ->
            taskMap[dependencyId]?.let { dependency ->
                val startX = ((dependency.endDate.time - state.startDate.time) / (1000 * 60 * 60 * 24)).toInt() * state.dayWidth * scale + offset.x
                val startY = state.tasks.indexOf(dependency) * state.rowHeight * scale + offset.y + (state.rowHeight * scale * 0.5f)
                
                val endX = ((task.startDate.time - state.startDate.time) / (1000 * 60 * 60 * 24)).toInt() * state.dayWidth * scale + offset.x
                val endY = state.tasks.indexOf(task) * state.rowHeight * scale + offset.y + (state.rowHeight * scale * 0.5f)

                // Draw arrow
                drawLine(
                    color = Color.Gray,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2f
                )

                // Draw arrowhead
                val arrowSize = 8f
                val angle = kotlin.math.atan2(endY - startY, endX - startX)
                val arrowPoint1X = endX - arrowSize * kotlin.math.cos(angle - Math.PI / 6)
                val arrowPoint1Y = endY - arrowSize * kotlin.math.sin(angle - Math.PI / 6)
                val arrowPoint2X = endX - arrowSize * kotlin.math.cos(angle + Math.PI / 6)
                val arrowPoint2Y = endY - arrowSize * kotlin.math.sin(angle + Math.PI / 6)

                drawLine(
                    color = Color.Gray,
                    start = Offset(endX.toFloat(), endY.toFloat()),
                    end = Offset(arrowPoint1X.toFloat(), arrowPoint1Y.toFloat()),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.Gray,
                    start = Offset(endX.toFloat(), endY.toFloat()),
                    end = Offset(arrowPoint2X.toFloat(), arrowPoint2Y.toFloat()),
                    strokeWidth = 2f
                )
            }
        }
    }
} 