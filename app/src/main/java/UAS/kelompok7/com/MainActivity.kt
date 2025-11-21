package UAS.kelompok7.com

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import UAS.kelompok7.com.ui.theme.UASkelompok7IOTTheme
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.tooling.preview.Preview

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

data class BottomNavItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navItems = listOf(
        BottomNavItem("Home", Icons.Default.Home, "home"),
        BottomNavItem("Dashboard", Icons.Default.Info, "dashboard"),
        BottomNavItem("Grafik", Icons.Default.Menu, "grafik"),
        BottomNavItem("Profile", Icons.Default.Person, "profile")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("UAS Kelompok 7 IoT", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                navItems.forEach { item ->
                    NavigationBarItem(
                        label = { Text(item.label) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(innerPadding)) {
            composable("home") { PageContent("Home", "Selamat Datang di Aplikasi IoT") }
            composable("dashboard") { DashboardScreen() }

            // GrafikScreen sekarang dipanggil dari SensorChart.kt
            composable("grafik") { GrafikScreen() }

            composable("profile") { PageContent("Profile", "Anggota Kelompok 7") }
        }
    }
}

@Composable
fun DashboardScreen() {
    // --- STATES DASHBOARD ---
    var isLedOn by remember { mutableStateOf(false) }
    var isMonitoringActive by remember { mutableStateOf(false) }
    var distanceValue by remember { mutableStateOf("0.0") }
    var connectionStatus by remember { mutableStateOf("Menghubungkan...") }

    val database = Firebase.database("https://uaskelompok7-a8d53-default-rtdb.asia-southeast1.firebasedatabase.app")
    val ledRef = database.getReference("IOT/led")
    val controlRef = database.getReference("IOT/control/is_active")
    val sensorRef = database.getReference("IOT/sensor")

    LaunchedEffect(Unit) {
        // 1. Listener LED
        ledRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isLedOn = snapshot.getValue(Boolean::class.java) ?: false
                connectionStatus = "Terhubung"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. Listener Status Monitoring
        controlRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isMonitoringActive = snapshot.getValue(Boolean::class.java) ?: false
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 3. Listener Nilai Sensor Saat Ini
        sensorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dist = snapshot.child("distance").getValue(Double::class.java)
                val distInt = snapshot.child("distance").getValue(Long::class.java)
                distanceValue = dist?.toString() ?: distInt?.toString() ?: "0.0"
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- UI DASHBOARD ---
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dashboard Kontrol", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(connectionStatus, fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        // CARD KONTROL
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Card Lampu
            ControlCard(title = "LAMPU", isActive = isLedOn) {
                ledRef.setValue(it)
            }
            // Card Monitor Sensor
            ControlCard(title = "MONITOR JARAK", isActive = isMonitoringActive) {
                controlRef.setValue(it)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CARD INDIKATOR UTAMA
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth().height(100.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Jarak Saat Ini", fontSize = 14.sp)
                Text("$distanceValue cm", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ControlCard(title: String, isActive: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.width(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = if(isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Switch(checked = isActive, onCheckedChange = onToggle)
            Text(if (isActive) "ON" else "OFF", fontSize = 12.sp)
        }
    }
}

@Composable
fun PageContent(title: String, description: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(description)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    UASkelompok7IOTTheme {
        MainScreen()
    }
}