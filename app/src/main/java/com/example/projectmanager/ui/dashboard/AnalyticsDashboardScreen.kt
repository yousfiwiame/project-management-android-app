package com.example.projectmanager.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AnalyticsDashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Analytics Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Task Completion Rate
        item {
            TaskCompletionRateChart(
                completedTasks = uiState.completedProjects,
                totalTasks = uiState.totalProjects
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Projects by Status
        item {
            ProjectStatusChart(
                statusCounts = uiState.projectStatusCounts
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Time to Completion
        item {
            if (uiState.timeToCompletionData.isNotEmpty()) {
                TimeToCompletionChart(
                    timeData = uiState.timeToCompletionData
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Team Productivity
        item {
            if (uiState.teamProductivityData.isNotEmpty()) {
                TeamProductivityChart(
                    productivityData = uiState.teamProductivityData
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Team Members
        item {
            TeamMembersSection(
                teamMembers = uiState.teamMembers
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TeamMembersSection(teamMembers: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Team Members",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (teamMembers.isEmpty()) {
                Text(
                    text = "No team members found",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column {
                    teamMembers.forEach { memberId ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Simple circle avatar placeholder
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = memberId.take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = memberId,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
