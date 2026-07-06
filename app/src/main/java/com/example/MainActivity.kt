package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.MainTabScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ChatViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SQLite Room database, DAO, and Repository pattern
        val database = AppDatabase.getDatabase(this)
        val repository = ChatRepository(database.chatDao())
        
        // Instantiate the Viewmodel using the Factory
        val viewModelFactory = ChatViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[ChatViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val selectedChatId by viewModel.selectedChatId.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // High-fidelity state-based screen navigator with an iOS-style slide transition
                    AnimatedContent(
                        targetState = selectedChatId,
                        transitionSpec = {
                            if (targetState != null) {
                                // Slide in from right (forward iOS transition)
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width / 3 } + fadeOut()
                            } else {
                                // Slide out to right (back iOS transition)
                                slideInHorizontally { width -> -width / 3 } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }
                        },
                        label = "ScreenTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { activeChatId ->
                        if (activeChatId == null) {
                            MainTabScreen(
                                viewModel = viewModel,
                                onChatClicked = { id -> viewModel.selectChat(id) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // BackHandler ensures pressing system back button returns to the home tabs cleanly
                            BackHandler {
                                viewModel.selectChat(null)
                            }
                            
                            ChatDetailScreen(
                                viewModel = viewModel,
                                chatId = activeChatId,
                                onBackClicked = { viewModel.selectChat(null) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
