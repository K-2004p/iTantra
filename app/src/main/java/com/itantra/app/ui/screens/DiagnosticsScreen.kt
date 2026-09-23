package com.itantra.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            .padding(14.dp)
    ) {
        // ── Telemetry Header Card ──────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.6f), TechBorder)),
                    RoundedCornerShape(14.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NeonCyanSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = "Speed", tint = NeonCyan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("SYSTEM TELEMETRY & METRICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                        Text("On-Device Performance Benchmarks", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
                Badge(containerColor = SuccessGreenSubtle, contentColor = SuccessGreen) {
                    Text("LIVE", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Metrics Grid ───────────────────────────────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { MetricCard("STT Latency", "${metrics.sttLatencyMs} ms", "Speech Recognition Time", NeonCyan) }
            item { MetricCard("TTS Latency", "${metrics.ttsLatencyMs} ms", "Audio Synthesis Time", NeonCyan) }
            item { MetricCard("End-to-End Latency", "${metrics.endToEndLatencyMs} ms", "Speech Start to Playback", AmberAlert) }
            item { MetricCard("Real-Time Factor", "%.2f".format(metrics.rtf), "Process / Audio Duration", SuccessGreen) }
            item { MetricCard("RAM Usage", "${metrics.ramUsageMb} MB", "Device Heap Memory", NeonCyan) }
            item { MetricCard("CPU Processing", "%.1f%%".format(metrics.cpuUsagePercent), "Active Processing Load", SuccessGreen) }
            item { MetricCard("Model Architecture", "INT8 Quantized", "Optimized Mobile Footprint", TextPrimary) }
            item { MetricCard("Transport Throughput", "High-Speed P2P", "Direct Socket / RFCOMM", NeonCyan) }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color
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
            Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 10.sp, color = TextMuted)
        }
    }
}
