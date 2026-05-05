package com.example.focusflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.focusflow.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 主题设置界面
 * 
 * 提供三种主题模式选择：
 * - 跟随系统
 * - 深色模式
 * - 浅色模式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 订阅当前主题模式
    val currentMode by ThemeManager.themeModeFlow
        .collectAsState(initial = ThemeMode.FOLLOW_SYSTEM)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "主题设置",
            style = MaterialTheme.typography.headlineMedium,
            color = LocalAppColors.current.textMain
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 使用 SegmentedButton 组
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            ThemeMode.values().forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = currentMode == mode,
                    onClick = {
                        scope.launch {
                            ThemeManager.setThemeMode(mode, context)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ThemeMode.values().size
                    )
                ) {
                    Text(
                        text = when (mode) {
                            ThemeMode.FOLLOW_SYSTEM -> "跟随系统"
                            ThemeMode.FORCE_DARK -> "深色模式"
                            ThemeMode.FORCE_LIGHT -> "浅色模式"
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 当前主题预览
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = LocalAppColors.current.cardBg
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "当前主题预览",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalAppColors.current.textMain
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // 主色调预览
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "主色调",
                        color = LocalAppColors.current.textSub
                    )
                    Text(
                        text = "示例文字",
                        color = LocalAppColors.current.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 主文字颜色预览
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "主文字颜色",
                        color = LocalAppColors.current.textSub
                    )
                    Text(
                        text = "示例文字",
                        color = LocalAppColors.current.textMain
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 副文字颜色预览
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "副文字颜色",
                        color = LocalAppColors.current.textSub
                    )
                    Text(
                        text = "示例文字",
                        color = LocalAppColors.current.textSub
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 图表颜色预览
                Text(
                    text = "图表颜色预览",
                    style = MaterialTheme.typography.titleSmall,
                    color = LocalAppColors.current.textMain
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val chartColors = PrintFriendlyColors.getChartColors()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    chartColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(2.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = color,
                                shape = MaterialTheme.shapes.small
                            ) {}
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 说明文字
        Text(
            text = when (currentMode) {
                ThemeMode.FOLLOW_SYSTEM -> "当前跟随系统主题设置，系统切换深色/浅色模式时应用会自动切换"
                ThemeMode.FORCE_DARK -> "当前强制使用深色主题，不受系统设置影响"
                ThemeMode.FORCE_LIGHT -> "当前强制使用浅色主题，适合打印和截图展示"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.textSub
        )
    }
}
