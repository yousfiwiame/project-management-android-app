package com.example.projectmanager.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmanager.data.local.preferences.AppPreferences
import com.example.projectmanager.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppTheme(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark"),
    DYNAMIC("Material You")
}

data class SettingsUiState(
    val theme: String = "system",
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val defaultProjectView: String = "list",
    val language: String = "en"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.value = SettingsUiState(
            theme = appPreferences.theme,
            emailNotifications = appPreferences.emailNotifications,
            pushNotifications = appPreferences.pushNotifications,
            defaultProjectView = appPreferences.defaultProjectView,
            language = appPreferences.language
        )
    }

    fun updateTheme(theme: String) {
        appPreferences.theme = theme
        _uiState.value = _uiState.value.copy(theme = theme)
    }

    fun updateEmailNotifications(enabled: Boolean) {
        appPreferences.emailNotifications = enabled
        _uiState.value = _uiState.value.copy(emailNotifications = enabled)
    }

    fun updatePushNotifications(enabled: Boolean) {
        appPreferences.pushNotifications = enabled
        _uiState.value = _uiState.value.copy(pushNotifications = enabled)
    }

    fun updateDefaultProjectView(view: String) {
        appPreferences.defaultProjectView = view
        _uiState.value = _uiState.value.copy(defaultProjectView = view)
    }

    fun updateLanguage(language: String) {
        appPreferences.language = language
        _uiState.value = _uiState.value.copy(language = language)
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                userRepository.signOut()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to sign out")
                }
            }
        }
    }

    fun clearAppData() {
        viewModelScope.launch {
            try {
                appPreferences.clearAll()
                // Add any other data clearing operations here
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to clear app data")
                }
            }
        }
    }

    fun openPrivacyPolicy() {
        openUrl("https://example.com/privacy-policy")
    }

    fun openTermsOfService() {
        openUrl("https://example.com/terms-of-service")
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = e.message ?: "Failed to open URL")
            }
        }
    }
}