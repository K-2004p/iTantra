package com.itantra.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.data.models.ModelInfo
import com.itantra.app.data.models.ModelManager
import com.itantra.app.ui.theme.*

@Composable
fun ModelManagerScreen() {
    val totalStorage = ModelManager.getTotalStorageMb()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TechDarkBackground)
            .padding(16.dp)
    ) {
        // Storage Header Card
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SdStorage, contentDescription = "Storage", tint = NeonCyan, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("ON-DEVICE AI MODEL STORAGE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text("Total Offline Assets Installed", fontSize = 14.sp, color = TextPrimary)
                    }
                }
                Text(
                    text = "${"%.1f".format(totalStorage)} MB",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "LANGUAGE MODEL INVENTORY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(ModelManager.supportedLanguages) { model ->
                ModelItemRow(model = model)
            }
        }
    }
}

@Composable
fun ModelItemRow(model: ModelInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TechCardSurface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TechBorder, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${model.languageName} (${model.nativeName})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text("STT: ${model.sttSizeMb} MB", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("TTS: ${model.ttsSizeMb} MB", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Badge(containerColor = SuccessGreen.copy(alpha = 0.2f), contentColor = SuccessGreen) {
                    Text("INSTALLED ✓", fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${"%.1f".format(model.sttSizeMb + model.ttsSizeMb)} MB",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        }
    }
}
