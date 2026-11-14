package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.AcademicBookLoadingAnimation
import com.example.ui.components.AnimatedSplashScreen
import com.example.ui.screens.ApiConfigScreen
import com.example.ui.screens.ApiConnectionTestScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExamScreen
import com.example.ui.screens.KisiKisiScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.QuestionsGeneratingScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyChatScreen
import com.example.ui.screens.ThemeToggleButton
import com.example.ui.theme.AcademicBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ExamViewModel
import com.example.viewmodel.UiState

class MainActivity : ComponentActivity() {

    private val viewModel: ExamViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val isSessionLoaded by viewModel.isSessionLoaded.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        enterTransition = { fadeIn(animationSpec = tween(220)) },
                        exitTransition = { fadeOut(animationSpec = tween(180)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                        popExitTransition = { fadeOut(animationSpec = tween(180)) }
                    ) {
                        // 0. Smooth Animated Academic Launch Splash
                        composable("splash") {
                            AnimatedSplashScreen(
                                isSessionLoaded = isSessionLoaded,
                                onSplashFinished = {
                                    val currentSession = viewModel.userSession.value
                                    val configured = viewModel.isApiConfigured.value &&
                                        viewModel.baseUrl.value.isNotBlank() &&
                                        viewModel.customApiKey.value.isNotBlank()
                                    val targetRoute = when {
                                        currentSession == null || !currentSession.isLogin -> "login"
                                        !configured -> "api_config"
                                        else -> "home"
                                    }
                                    navController.navigate(targetRoute) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 1. Staged Profile Registration (No Code Auth)
                        composable("login") {
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = {
                                    navController.navigate("api_config") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 1b. Manual Base URL, Tipe Base URL & API Key Configuration Page
                        composable("api_config") {
                            ApiConfigScreen(
                                viewModel = viewModel,
                                onStartTest = {
                                    navController.navigate("api_test")
                                },
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 1c. API Connection Test Page (max 5 requests, 5 min timeout)
                        composable("api_test") {
                            ApiConnectionTestScreen(
                                viewModel = viewModel,
                                onTestSuccess = {
                                    navController.navigate("home") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onTestFailed = {
                                    navController.popBackStack()
                                },
                                onCancel = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 2. Main Study Lobby & History
                        composable("home") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToStudyChat = { navController.navigate("study_chat") },
                                onNavigateToKisiKisi = { navController.navigate("kisi_kisi") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onViewResult = { attemptId ->
                                    navController.navigate("result/$attemptId")
                                }
                            )
                        }

                        // 2b. Study Tutor & Text/Document Explanation Screen
                        composable("study_chat") {
                            StudyChatScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // 3. Practice Material / Kisi-Kisi Input Page
                        composable("kisi_kisi") {
                            KisiKisiScreen(
                                viewModel = viewModel,
                                onGenerateStarted = { navController.navigate("generating_questions") },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // 4. Animated Loading + Study Tips for Question Generation
                        composable("generating_questions") {
                            QuestionsGeneratingScreen(
                                viewModel = viewModel,
                                onSuccess = {
                                    navController.navigate("exam") {
                                        popUpTo("kisi_kisi") { inclusive = true }
                                    }
                                },
                                onCancel = { navController.popBackStack() }
                            )
                        }

                        // 5. Practice Quiz Sheet (Pilihan Ganda & Esai)
                        composable("exam") {
                            ExamScreen(
                                viewModel = viewModel,
                                onSubmitSuccess = {
                                    navController.navigate("grading_loading") {
                                        popUpTo("exam") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // 6. Animated Essay Evaluation Screen
                        composable("grading_loading") {
                            val gradingState by viewModel.gradingState.collectAsState()

                            LaunchedEffect(gradingState) {
                                if (gradingState is UiState.Success) {
                                    val attempt = (gradingState as UiState.Success).data
                                    navController.navigate("result/${attempt.id}") {
                                        popUpTo("grading_loading") { inclusive = true }
                                    }
                                } else if (gradingState is UiState.Error) {
                                    navController.navigate("home") {
                                        popUpTo("grading_loading") { inclusive = true }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AcademicBackground)
                                    .statusBarsPadding()
                                    .navigationBarsPadding(),
                                contentAlignment = Alignment.Center
                            ) {
                                AcademicBookLoadingAnimation(
                                    title = "Mengevaluasi Jawaban Esai",
                                    subtitle = "Membandingkan jawabanmu dengan poin utama materi...",
                                    tips = listOf(
                                        "Setiap jawaban dinilai berdasarkan ketepatan konsep inti, bukan hafalan kata per kata.",
                                        "Catatan perbaikan akan muncul pada halaman hasil agar mudah dipelajari ulang."
                                    ),
                                    modifier = Modifier.padding(horizontal = 28.dp)
                                )
                                ThemeToggleButton(
                                    viewModel = viewModel,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .zIndex(10f)
                                )
                            }
                        }

                        // 7. Score Report & Essay Feedback Screen
                        composable(
                            route = "result/{attemptId}",
                            arguments = listOf(navArgument("attemptId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val attemptId = backStackEntry.arguments?.getInt("attemptId") ?: -1
                            ResultScreen(
                                viewModel = viewModel,
                                attemptId = attemptId,
                                onNavigateBackToDashboard = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 8. Settings & Base URL / API Key Setup
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = {
                                    if (navController.previousBackStackEntry == null) {
                                        navController.navigate("login") {
                                            popUpTo("settings") { inclusive = true }
                                        }
                                    } else {
                                        navController.popBackStack()
                                    }
                                },
                                onStartTest = {
                                    navController.navigate("api_test")
                                },
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
