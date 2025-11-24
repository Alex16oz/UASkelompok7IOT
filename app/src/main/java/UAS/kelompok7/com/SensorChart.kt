package UAS.kelompok7.com

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
// Import Vico
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data Class untuk menampung Nilai dan Waktu
data class SensorData(
    val value: Float,
    val timestamp: Long
)

@Composable
fun GrafikScreen() {
    // --- STATES GRAFIK ---
    // Menggunakan List of SensorData (bukan lagi Float biasa)
    val historyList = remember { mutableStateListOf<SensorData>() }

    val database = Firebase.database("https://uaskelompok7-a8d53-default-rtdb.asia-southeast1.firebasedatabase.app")
    val historyRef = database.getReference("IOT/history")

    LaunchedEffect(Unit) {
        // Listener Riwayat Data - Ambil 10-20 data terakhir agar grafik rapi
        historyRef.limitToLast(10).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()
                for (child in snapshot.children) {
                    // 1. Ambil Nilai (Jarak)
                    val `val` = child.child("val").getValue(Double::class.java)
                    val valLong = child.child("val").getValue(Long::class.java)
                    val finalVal = `val`?.toFloat() ?: valLong?.toFloat() ?: 0f

                    // 2. Ambil Timestamp (Waktu)
                    // Python mengirim seconds, Java butuh milliseconds, jadi dikali 1000
                    val ts = child.child("ts").getValue(Double::class.java)
                    val tsLong = child.child("ts").getValue(Long::class.java)
                    val finalTs = ((ts?.toLong() ?: tsLong ?: 0L) * 1000)

                    historyList.add(SensorData(finalVal, finalTs))
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- UI GRAFIK ---
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Grafik Realtime (Waktu & Persen)", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(Color(0xFFFFFFFF), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (historyList.isNotEmpty()) {
                SensorChartVico(dataPoints = historyList)
            } else {
                Text("Menunggu data...", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun SensorChartVico(dataPoints: List<SensorData>) {
    // Konversi data ke format Entry Vico
    // x = index (urutan data 0, 1, 2...), y = nilai sensor
    val entries = dataPoints.mapIndexed { index, data ->
        FloatEntry(x = index.toFloat(), y = data.value)
    }
    val chartEntryModel = entryModelOf(entries)

    // Formatter Sumbu X (Bawah) - Mengubah Index menjadi Jam:Menit
    val bottomAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        val index = value.toInt()
        // Ambil timestamp dari list berdasarkan index
        val timestamp = dataPoints.getOrNull(index)?.timestamp ?: 0L
        if (timestamp > 0) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(Date(timestamp))
        } else {
            ""
        }
    }

    // Formatter Sumbu Y (Kiri) - Menambahkan simbol "%"
    val startAxisFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
        "${value.toInt()}%"
    }

    Chart(
        chart = lineChart(),
        model = chartEntryModel,

        // Konfigurasi Sumbu Y (Kiri)
        startAxis = rememberStartAxis(
            title = "Ketinggian (%)",
            valueFormatter = startAxisFormatter,
            // Opsional: Jika ingin memaksa visualisasi 0 sampai 100
            // itemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 6)
        ),

        // Konfigurasi Sumbu X (Bawah)
        bottomAxis = rememberBottomAxis(
            title = "Waktu",
            valueFormatter = bottomAxisFormatter,
            guideline = null // Menghilangkan garis grid vertikal agar lebih bersih
        ),

        modifier = Modifier.fillMaxSize()
    )
}