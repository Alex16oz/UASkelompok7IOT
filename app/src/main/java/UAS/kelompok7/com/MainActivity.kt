package UAS.kelompok7.com

import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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

    val navItems = listOf(
        BottomNavItem("Home", Icons.Default.Home, "home"),
        BottomNavItem("Dashboard", Icons.Default.Info, "dashboard"),
        BottomNavItem("Profile", Icons.Default.Person, "profile")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                navItems.forEach { item ->
                    NavigationBarItem(
                        label = { Text(item.label) },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
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
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                PageContent(title = "Halaman Home", description = "Selamat datang di Aplikasi IoT Kelompok 7")
            }
            composable("dashboard") {
                DashboardScreen()
            }
            composable("profile") {
                PageContent(title = "Halaman Profile", description = "Informasi Anggota Kelompok")
            }
        }
    }
}

@Composable
fun DashboardScreen() {
    // State untuk status LED (Hidup/Mati)
    var isLedOn by remember { mutableStateOf(false) }

    // State untuk data Sensor Jarak
    var distanceValue by remember { mutableStateOf("0.0") }

    // State status koneksi
    var isDatabaseReady by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("Menghubungkan ke Asia-Southeast1...") }

    val database = Firebase.database("https://uaskelompok7-a8d53-default-rtdb.asia-southeast1.firebasedatabase.app")

    // Referensi untuk LED
    val ledRef = database.getReference("IOT/led")

    // Referensi untuk Sensor
    val sensorRef = database.getReference("IOT/sensor")

    LaunchedEffect(Unit) {
        // Listener untuk LED
        ledRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(Boolean::class.java)
                isLedOn = status ?: false
                isDatabaseReady = true
                connectionStatus = "Terhubung"
            }

            override fun onCancelled(error: DatabaseError) {
                isDatabaseReady = false
                connectionStatus = "Error LED: ${error.message}"
            }
        })

        // Listener untuk Sensor Jarak
        sensorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Mengambil nilai child "distance" dari JSON yang dikirim ESP32
                val dist = snapshot.child("distance").getValue(Double::class.java)
                val distInt = snapshot.child("distance").getValue(Long::class.java) // jaga-jaga jika integer

                distanceValue = dist?.toString() ?: distInt?.toString() ?: "0.0"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Error Sensor: ${error.message}")
            }
        })
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Dashboard Monitoring",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = connectionStatus,
            fontSize = 12.sp,
            color = if (isDatabaseReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(30.dp))

        // --- CARD KONTROL LED ---
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isLedOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "KONTROL LAMPU",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isLedOn) "MENYALA" else "MATI",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Switch(
                    checked = isLedOn,
                    onCheckedChange = { checked ->
                        isLedOn = checked
                        ledRef.setValue(checked)
                    },
                    enabled = isDatabaseReady
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- CARD MONITORING JARAK (BARU) ---
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SENSOR ULTRASONIC",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$distanceValue cm",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Jarak Terukur",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PageContent(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
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