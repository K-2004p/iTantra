package com.itantra.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.data.models.ModelInfo
import com.itantra.app.data.models.ModelManager
import com.itantra.app.ui.theme.*

@Composable
fun LanguageScreen(
    inputLanguage: String,
    outputLanguage: String,
    onInputLanguageSelected: (String) -> Unit,
    onOutputLanguageSelected: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Input (I Speak), 1: Output (Target/Hear)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TechDarkBackground)
            .padding(14.dp)
    ) {
        // ── Header Card ────────────────────────────────────────────────────────
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
                        Icon(Icons.Default.Translate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MULTILINGUAL MATRIX",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Dual Language Routing",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
                Badge(containerColor = TechCardSurfaceElevated, contentColor = NeonCyan) {
                    Text("10 LANGUAGES", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Active Route Summary Bar ───────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardSurfaceElevated),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, TechBorder, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("I SPEAK (STT)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ModelManager.getLanguageName(inputLanguage),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAlert
                    )
                }

                Text("➔", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonCyan)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Hearing, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("I HEAR / TARGET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ModelManager.getLanguageName(outputLanguage),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Modern Tab Selector (Input vs Output) ──────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TechCardSurface, RoundedCornerShape(10.dp))
                .border(1.dp, TechBorder, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 1: Input Language
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedTab == 0) AmberAlert else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (selectedTab == 0) TechDarkBackground else AmberAlert
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "1. Input (${inputLanguage.uppercase()})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedTab == 0) TechDarkBackground else TextPrimary
                    )
                }
            }

            // Tab 2: Output Language
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedTab == 1) NeonCyan else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Hearing,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (selectedTab == 1) TechDarkBackground else NeonCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "2. Output (${outputLanguage.uppercase()})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedTab == 1) TechDarkBackground else TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val activeLang = if (selectedTab == 0) inputLanguage else outputLanguage
        val activeColor = if (selectedTab == 0) AmberAlert else NeonCyan

        // ── Language Grid ──────────────────────────────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(ModelManager.supportedLanguages) { model ->
                LanguageCard(
                    model = model,
                    isSelected = activeLang.equals(model.languageCode, ignoreCase = true),
                    accentColor = activeColor,
                    onClick = {
                        if (selectedTab == 0) {
                            onInputLanguageSelected(model.languageCode)
                        } else {
                            onOutputLanguageSelected(model.languageCode)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LanguageCard(
    model: ModelInfo,
    isSelected: Boolean,
    accentColor: Color = NeonCyan,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.12f) else TechCardSurface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else TechBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.nativeName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) accentColor else TextPrimary
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = model.languageName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Badge(containerColor = TechDarkBackground, contentColor = SuccessGreen) {
                    Text("STT ✓", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Badge(containerColor = TechDarkBackground, contentColor = NeonCyan) {
                    Text("TTS ✓", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
        }
    }
}
