package UAS.kelompok7.com

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import java.util.Calendar

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

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navItems = listOf(
        BottomNavItem("Home", Icons.Default.Home, "home"),
        BottomNavItem("Dashboard", Icons.Default.Info, "dashboard"),
        BottomNavItem("Grafik", Icons.Default.DateRange, "grafik"), // Icon diganti agar lebih sesuai
        BottomNavItem("Profile", Icons.Default.Person, "profile")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Smart Pet Feeder", fontWeight = FontWeight.Bold) },
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
            composable("home") { PageContent("Home", "Selamat Datang di Aplikasi Smart Pet Feeder") }
            composable("dashboard") { DashboardScreen() }
            composable("grafik") { GrafikScreen() } // Memanggil dari SensorChart.kt
            composable("profile") { PageContent("Profile", "Anggota Kelompok 7") }
        }
    }
}

@Composable
fun DashboardScreen() {
    val context = LocalContext.current

    // --- STATES ---
    var isLedOn by remember { mutableStateOf(false) }
    var isMonitoringActive by remember { mutableStateOf(false) }
    var distanceValue by remember { mutableStateOf("0.0") }
    var weightValue by remember { mutableStateOf("0.0") }
    var scheduledTime by remember { mutableStateOf("--:--") }
    var connectionStatus by remember { mutableStateOf("Menghubungkan...") }

    // --- FIREBASE REFS ---
    // Pastikan URL Database sesuai dengan milik Anda
    val database = Firebase.database("https://uaskelompok7-a8d53-default-rtdb.asia-southeast1.firebasedatabase.app")
    val rootRef = database.reference.child("IOT")

    val ledRef = rootRef.child("led")
    val controlRef = rootRef.child("control").child("is_active")
    val feedNowRef = rootRef.child("control").child("feed_now")
    val sensorRef = rootRef.child("sensor")
    val scheduleRef = rootRef.child("schedule")

    LaunchedEffect(Unit) {
        // 1. LED Listener
        ledRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isLedOn = snapshot.getValue(Boolean::class.java) ?: false
                connectionStatus = "Terhubung"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. Monitoring Active Listener
        controlRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isMonitoringActive = snapshot.getValue(Boolean::class.java) ?: false
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 3. Sensor Listener (Jarak & Berat)
        sensorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Ambil Distance
                val dVal = snapshot.child("distance").getValue(Double::class.java)
                val dLong = snapshot.child("distance").getValue(Long::class.java)
                distanceValue = dVal?.toString() ?: dLong?.toString() ?: "0.0"

                // Ambil Weight (Load Cell)
                val wVal = snapshot.child("weight").getValue(Double::class.java)
                val wLong = snapshot.child("weight").getValue(Long::class.java)
                weightValue = wVal?.toString() ?: wLong?.toString() ?: "0.0"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 4. Schedule Listener
        scheduleRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val h = snapshot.child("hour").getValue(Int::class.java)
                val m = snapshot.child("minute").getValue(Int::class.java)
                if (h != null && m != null) {
                    scheduledTime = String.format("%02d:%02d", h, m)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- TIME PICKER DIALOG ---
    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour: Int, minute: Int ->
            scheduleRef.child("hour").setValue(hour)
            scheduleRef.child("minute").setValue(minute)
            Toast.makeText(context, "Jadwal diatur ke $hour:$minute", Toast.LENGTH_SHORT).show()
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    // --- UI DASHBOARD ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Status: $connectionStatus", fontSize = 12.sp, color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- MONITORING CARDS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Kedalaman (Ultrasonic)
            InfoCard(
                title = "Kedalaman",
                value = "$distanceValue cm",
                icon = Icons.Default.Info,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.secondaryContainer
            )
            // Card Berat (Load Cell)
            InfoCard(
                title = "Sisa Pakan",
                value = "$weightValue gr",
                icon = Icons.Default.ShoppingCart, // Icon keranjang/makan
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.tertiaryContainer
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- CONTROL SWITCHES ---
        Text("Kontrol Sistem", fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ControlSwitch(title = "Lampu LED", isActive = isLedOn) { ledRef.setValue(it) }
            ControlSwitch(title = "Sistem Aktif", isActive = isMonitoringActive) { controlRef.setValue(it) }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(24.dp))

        // --- FEEDING CONTROL ---
        Text("Pengaturan Pakan", fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(12.dp))

        // 1. Jadwal Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { timePickerDialog.show() }, // Klik untuk buka jam
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Jadwal Otomatis", fontWeight = FontWeight.Bold)
                    Text("Ketuk untuk ubah jam", fontSize = 12.sp, color = Color.Gray)
                }
                Text(
                    text = scheduledTime,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Manual Feed Button
        Button(
            onClick = {
                feedNowRef.setValue(true)
                Toast.makeText(context, "Perintah memberi makan dikirim!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("BERI MAKAN SEKARANG", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- COMPOSABLE HELPERS ---

@Composable
fun InfoCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, color: Color) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 12.sp)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ControlSwitch(title: String, isActive: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(160.dp)
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(if (isActive) "ON" else "OFF", fontSize = 12.sp, color = if(isActive) Color.Green else Color.Gray)
        }
        Switch(checked = isActive, onCheckedChange = onToggle)
    }
}

@Composable
fun PageContent(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
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