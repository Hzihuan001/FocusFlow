package com.example.focusflow.ui.components

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusflow.ui.theme.CyberCardBg
import com.example.focusflow.ui.theme.CyberPrimary
import com.example.focusflow.ui.theme.CyberSecondary
import com.example.focusflow.ui.theme.CyberTextSub

/**
 * 头像/图腾模型
 */
data class TotemItem(
    val id: Int,
    val icon: ImageVector,
    val emoji: String,
    val rarityColor: Color
)

/**
 * 获取 Mock 头像列表
 */
val MockTotems = listOf(
    TotemItem(1, Icons.Default.AcUnit, "💠", Color(0xFF00FF9D)),
    TotemItem(2, Icons.Default.ChangeHistory, "🔺", Color(0xFFE91E63)),
    TotemItem(3, Icons.Default.Hexagon, "🔯", Color(0xFF9C27B0)),
    TotemItem(4, Icons.Default.Cyclone, "🌀", Color(0xFF2196F3)),
    TotemItem(5, Icons.Default.Diamond, "💎", Color(0xFF00BCD4)),
    TotemItem(6, Icons.Default.Hub, "⚛️", Color(0xFFFFEB3B)),
    TotemItem(7, Icons.Default.Adjust, "🧿", Color(0xFF4CAF50)),
    TotemItem(8, Icons.Default.AutoAwesome, "🌠", Color(0xFFFF9800)),
    TotemItem(9, Icons.Default.BlurCircular, "🪐", Color(0xFFFF5722)),
    TotemItem(10, Icons.Default.RocketLaunch, "🛸", Color(0xFF795548)),
    TotemItem(11, Icons.Default.WbIridescent, "🔮", Color(0xFF03A9F4)),
    TotemItem(12, Icons.Default.BrightnessLow, "🏮", Color(0xFFFFC107))
)

/**
 * 量子图腾选择器 BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerBottomSheet(
    selectedId: Int?,
    onAvatarSelect: (Int) -> Unit,
    onConfirm: () -> Unit,
    onEditNickname: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0A12).copy(alpha = 0.95f),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "量子图腾库",
                    color = CyberPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "QUANTUM TOTEM GALLERY",
                    color = CyberTextSub,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(MockTotems) { totem ->
                    TotemCard(
                        totem = totem,
                        isSelected = totem.id == selectedId,
                        onClick = { onAvatarSelect(totem.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 修改昵称 (Outlined Style)
                OutlinedButton(
                    onClick = onEditNickname,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Text("更改昵称", fontWeight = FontWeight.Bold)
                }

                // 确认覆写 (Neon Style)
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                ) {
                    Text("图腾覆写", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TotemCard(
    totem: TotemItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isSelected) listOf(totem.rarityColor.copy(alpha = 0.3f), Color.Transparent)
                    else listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
                )
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) totem.rarityColor else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(totem.emoji, fontSize = 32.sp)
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(totem.rarityColor, CircleShape)
                        .blur(4.dp)
                )
            }
        }
    }
}
