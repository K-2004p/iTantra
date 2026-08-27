package com.itantra.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.domain.diagnostics.PerformanceMonitor
import com.itantra.app.ui.theme.*

@Composable
fun DiagnosticsScreen() {
    val metrics by PerformanceMonitor.metrics.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TechDarkBackground)
            .padding(16.dp)
    ) {
        // Header
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Speed, contentDescription = "Speed", tint = NeonCyan, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("SYSTEM PERFORMANCE DIAGNOSTICS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text("Measured On-Device Benchmark Metrics", fontSize = 14.sp, color = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { MetricCard("STT Latency", "${metrics.sttLatencyMs} ms", "Speech Recognition Time", NeonCyan) }
            item { MetricCard("TTS Latency", "${metrics.ttsLatencyMs} ms", "Audio Synthesis Time", NeonCyan) }
            item { MetricCard("End-to-End Latency", "${metrics.endToEndLatencyMs} ms", "Speech Start to Audio Playback", AmberAlert) }
            item { MetricCard("Real-Time Factor (RTF)", "%.2f".format(metrics.rtf), "Processing / Audio Duration Ratio", SuccessGreen) }
            item { MetricCard("RAM Usage", "${metrics.ramUsageMb} MB", "Device Heap Memory Footprint", NeonCyan) }
            item { MetricCard("CPU Usage", "%.1f%%".format(metrics.cpuUsagePercent), "Active Processing Load", SuccessGreen) }
            item { MetricCard("STT Model Size", "%.1f MB".format(metrics.sttModelSizeMb), "Optimized INT8 Quantized", TextPrimary) }
            item { MetricCard("TTS Model Size", "%.1f MB".format(metrics.ttsModelSizeMb), "Optimized Voice Model", TextPrimary) }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TechCardSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TechBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 10.sp, color = TextMuted)
        }
    }
}
