package com.example.projectmanager.navigation

interface AppNavigator {
    fun navigateBack()
    fun navigateToSignIn()
    fun navigateToSignUp()
    fun navigateToForgotPassword()
    fun navigateToHome()
    fun navigateToProjects()
    fun navigateToProject(projectId: String)
    fun navigateToTasks()
    fun navigateToTask(taskId: String)
    fun navigateToProfile()
    fun navigateToSettings()
    fun navigateToCreateProject()
    fun navigateToCreateTask(projectId: String? = null)
    fun navigateToEditProject(projectId: String)
    fun navigateToEditTask(taskId: String)
    fun isUserSignedIn(): Boolean
} 