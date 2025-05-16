package com.example.projectmanager.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.projectmanager.ui.auth.ForgotPasswordScreen
import com.example.projectmanager.ui.auth.SignInScreen
import com.example.projectmanager.ui.auth.SignUpScreen

const val AUTH_GRAPH_ROUTE = "auth"
const val SIGN_IN_ROUTE = "sign_in"
const val SIGN_UP_ROUTE = "sign_up"
const val FORGOT_PASSWORD_ROUTE = "forgot_password"

fun NavGraphBuilder.authNavigation(
    navController: NavController,
    onAuthSuccess: () -> Unit
) {
    navigation(
        startDestination = SIGN_IN_ROUTE,
        route = AUTH_GRAPH_ROUTE
    ) {
        composable(SIGN_IN_ROUTE) {
            SignInScreen(
                onSignInSuccess = onAuthSuccess,
                onNavigateToSignUp = {
                    navController.navigate(SIGN_UP_ROUTE)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(FORGOT_PASSWORD_ROUTE)
                }
            )
        }

        composable(SIGN_UP_ROUTE) {
            SignUpScreen(
                onSignUpSuccess = onAuthSuccess,
                onNavigateToSignIn = {
                    navController.navigateUp()
                }
            )
        }

        composable(FORGOT_PASSWORD_ROUTE) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
} 