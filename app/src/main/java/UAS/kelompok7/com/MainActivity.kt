package UAS.kelompok7.com

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
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
            composable("profile") { PageContent("Profile", "Anggota Kelompok 7") }
        }
    }
}

@Composable
fun DashboardScreen() {
    // --- STATES ---
    var isLedOn by remember { mutableStateOf(false) }
    var isMonitoringActive by remember { mutableStateOf(false) } // State Tombol Monitor
    var distanceValue by remember { mutableStateOf("0.0") }
    var connectionStatus by remember { mutableStateOf("Menghubungkan...") }

    // List untuk menyimpan riwayat data (Float)
    val historyList = remember { mutableStateListOf<Float>() }

    val database = Firebase.database("https://uaskelompok7-a8d53-default-rtdb.asia-southeast1.firebasedatabase.app")
    val ledRef = database.getReference("IOT/led")
    val controlRef = database.getReference("IOT/control/is_active") // Ref Baru
    val sensorRef = database.getReference("IOT/sensor")
    val historyRef = database.getReference("IOT/history") // Ref Baru

    LaunchedEffect(Unit) {
        // 1. Listener LED
        ledRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isLedOn = snapshot.getValue(Boolean::class.java) ?: false
                connectionStatus = "Terhubung"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. Listener Status Monitoring (Tombol ON/OFF Sensor).
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

        // 4. Listener Riwayat Data (Grafik) - Ambil 20 data terakhir
        historyRef.limitToLast(20).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()
                for (child in snapshot.children) {
                    val `val` = child.child("val").getValue(Double::class.java)
                    val valLong = child.child("val").getValue(Long::class.java)
                    val finalVal = `val`?.toFloat() ?: valLong?.toFloat() ?: 0f
                    historyList.add(finalVal)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- UI ---
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dashboard & Grafik", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(connectionStatus, fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        // CARD KONTROL
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Card Lampu
            ControlCard(title = "LAMPU", isActive = isLedOn) {
                ledRef.setValue(it)
            }
            // Card Monitor Sensor (Tombol Baru)
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

        Spacer(modifier = Modifier.height(20.dp))

        // GRAFIK CANVAS
        Text("Grafik Riwayat (Realtime)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (historyList.isNotEmpty()) {
                SimpleLineChart(dataPoints = historyList)
            } else {
                Text("Belum ada data...", modifier = Modifier.align(Alignment.Center))
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
fun SimpleLineChart(dataPoints: List<Float>) {
    if (dataPoints.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val maxVal = (dataPoints.maxOrNull() ?: 100f) * 1.2f // Skala atas + 20%
        val minVal = 0f

        val path = Path()
        val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)

        dataPoints.forEachIndexed { index, value ->
            // Normalisasi Y (0 di bawah, height di atas canvas)
            val x = index * stepX
            val y = height - ((value / maxVal) * height)

            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

            // Gambar titik data
            drawCircle(
                color = Color.Blue,
                radius = 6f,
                center = Offset(x, y)
            )
        }

        // Gambar Garis
        drawPath(
            path = path,
            color = Color(0xFF6200EE),
            style = Stroke(width = 5f)
        )
    }
}

@Composable
fun PageContent(title: String, description: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(description)
    }
}