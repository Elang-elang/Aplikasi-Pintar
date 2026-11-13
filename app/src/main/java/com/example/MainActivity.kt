package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.CosmosBackground
import com.example.ui.theme.CosmosTeal
import com.example.ui.theme.CosmosTextLight
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
                    if (!isSessionLoaded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CosmosBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CosmosTeal)
                        }
                    } else {
                        val startDestination = remember {
                            val currentSession = viewModel.userSession.value
                            val configured = viewModel.isApiConfigured.value &&
                                viewModel.baseUrl.value.isNotBlank() &&
                                viewModel.customApiKey.value.isNotBlank()
                            when {
                                currentSession == null || !currentSession.isLogin -> "login"
                                !configured -> "api_config"
                                else -> "home"
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = startDestination
                        ) {
                            // 1. Non-code Profile Login page
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

                            // 2. Main Study Lobby & Historic Results
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

                            // 2b. AI Study Chatbot & Text/Document Explanation Screen
                            composable("study_chat") {
                                StudyChatScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            // 3. Paste curriculum / topics input page
                            composable("kisi_kisi") {
                                KisiKisiScreen(
                                    viewModel = viewModel,
                                    onGenerateStarted = { navController.navigate("generating_questions") },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            // 4. Circular loading + interactive tips
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

                            // 5. Live exam sheet layout (Pilihan Ganda & Essays)
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

                            // 6. Essay background AI grading view
                            composable("grading_loading") {
                                val gradingState by viewModel.gradingState.collectAsState()

                                LaunchedEffect(gradingState) {
                                    if (gradingState is UiState.Success) {
                                        val attempt = (gradingState as UiState.Success).data
                                        navController.navigate("result/${attempt.id}") {
                                            popUpTo("grading_loading") { inclusive = true }
                                        }
                                    } else if (gradingState is UiState.Error) {
                                        // Fallback to home if severe grading failure
                                        navController.navigate("home") {
                                            popUpTo("grading_loading") { inclusive = true }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(CosmosBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ThemeToggleButton(
                                        viewModel = viewModel,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(16.dp)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = CosmosTeal, strokeWidth = 5.dp)
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Text(
                                            text = "Guru AI sedang mengoreksi esai Anda...",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CosmosTextLight
                                        )
                                        Text(
                                            text = "Menganalisis kemiripan jawaban Anda dengan kunci esensi pelajaran...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            // 7. Marks scoreboard & Visual feedback reports
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

                            // 8. Settings & Base URL / API Key / Admin parameters setup
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
}
