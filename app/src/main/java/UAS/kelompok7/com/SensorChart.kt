package UAS.kelompok7.com

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
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

data class SensorData(
    val value: Float,
    val timestamp: Long
)

@Composable
fun GrafikScreen() {
    val historyList = remember { mutableStateListOf<SensorData>() }
    // Pastikan URL Database sama
    val database = Firebase.database("https://uaskelompok7-a8d53-default-rtdb.asia-southeast1.firebasedatabase.app")
    val historyRef = database.getReference("IOT/history")

    LaunchedEffect(Unit) {
        historyRef.limitToLast(15).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()
                for (child in snapshot.children) {
                    val `val` = child.child("val").getValue(Double::class.java)
                    val valLong = child.child("val").getValue(Long::class.java)
                    val finalVal = `val`?.toFloat() ?: valLong?.toFloat() ?: 0f

                    val ts = child.child("ts").getValue(Double::class.java)
                    val tsLong = child.child("ts").getValue(Long::class.java)
                    // Konversi detik ke milidetik jika perlu
                    val finalTs = ((ts?.toLong() ?: tsLong ?: 0L) * 1000)

                    historyList.add(SensorData(finalVal, finalTs))
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Grafik Riwayat Sensor",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(MaterialTheme.colorScheme.surface)
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
    val entries = dataPoints.mapIndexed { index, data ->
        FloatEntry(x = index.toFloat(), y = data.value)
    }
    val chartEntryModel = entryModelOf(entries)

    val bottomAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        val index = value.toInt()
        val timestamp = dataPoints.getOrNull(index)?.timestamp ?: 0L
        if (timestamp > 0) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        } else {
            ""
        }
    }

    val startAxisFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
        "${value.toInt()}"
    }

    Chart(
        chart = lineChart(),
        model = chartEntryModel,
        startAxis = rememberStartAxis(
            title = "Nilai",
            valueFormatter = startAxisFormatter,
        ),
        bottomAxis = rememberBottomAxis(
            title = "Waktu",
            valueFormatter = bottomAxisFormatter,
            guideline = null
        ),
        modifier = Modifier.fillMaxSize()
    )
}