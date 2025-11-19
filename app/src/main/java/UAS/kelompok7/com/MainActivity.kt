package UAS.kelompok7.com

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import UAS.kelompok7.com.ui.theme.UASkelompok7IOTTheme
import androidx.compose.ui.tooling.preview.Preview
import UAS.kelompok7.com.ui.theme.UASkelompok7IOTTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UASkelompok7IOTTheme {
                MainScreen()
            }
        }
    }
}

// Data class untuk item navigasi
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    // Daftar 3 Halaman
    val navItems = listOf(
        BottomNavItem("Home", Icons.Default.Home, "home"),
        BottomNavItem("Dashboard", Icons.Default.Info, "dashboard"),
        BottomNavItem("Profile", Icons.Default.Person, "profile")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // BAGIAN 1: TOP APP BAR
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "UAS Kelompok 7 IOT",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        // BAGIAN 2: BOTTOM NAVIGATION BAR
        bottomBar = {
            NavigationBar {
                // Mendapatkan route saat ini agar icon aktif bisa menyala
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                navItems.forEach { item ->
                    NavigationBarItem(
                        label = { Text(item.label) },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                // Agar tidak menumpuk halaman saat back button ditekan
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // BAGIAN 3: KONTEN HALAMAN (NAV HOST)
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                PageContent(title = "Halaman Home", description = "Selamat datang di Aplikasi IoT Kelompok 7")
            }
            composable("dashboard") {
                PageContent(title = "Halaman Dashboard", description = "Monitor data sensor IoT di sini")
            }
            composable("profile") {
                PageContent(title = "Halaman Profile", description = "Informasi Anggota Kelompok")
            }
        }
    }
}

// Komponen sederhana untuk isi setiap halaman
@Composable
fun PageContent(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = description)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    UASkelompok7IOTTheme {
        MainScreen()
    }
}