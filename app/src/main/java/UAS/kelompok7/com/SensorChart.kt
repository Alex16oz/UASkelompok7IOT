package UAS.kelompok7.com

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

@Composable
fun GrafikScreen() {
    // --- STATES GRAFIK ---
    val historyList = remember { mutableStateListOf<Float>() }

    val database = Firebase.database("https://uaskelompok7-a8d53-default-rtdb.asia-southeast1.firebasedatabase.app")
    val historyRef = database.getReference("IOT/history")

    LaunchedEffect(Unit) {
        // Listener Riwayat Data (Grafik) - Ambil 20 data terakhir
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

    // --- UI GRAFIK ---
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Grafik Riwayat (Realtime)", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (historyList.isNotEmpty()) {
                // Memanggil komponen Chart yang sudah dipindahkan
                SensorChart(dataPoints = historyList)
            } else {
                Text("Belum ada data...", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun SensorChart(dataPoints: List<Float>) {
    if (dataPoints.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val maxVal = (dataPoints.maxOrNull() ?: 100f) * 1.2f // Skala atas + 20%

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