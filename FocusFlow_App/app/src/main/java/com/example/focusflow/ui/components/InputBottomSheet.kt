package com.example.focusflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusflow.ui.screens.FocusMode
import com.example.focusflow.ui.theme.CyberPrimary
import com.example.focusflow.ui.theme.CyberTextSub

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputBottomSheet(
    isOpen: Boolean,
    initialMode: FocusMode,
    allModes: List<FocusMode>,
    onDismiss: () -> Unit,
    // 参数含义: (任务名, 总时长, 专注时长, 休息时长, 标签)
    onStartFocus: (String, Int, Int, Int, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 🟢 唯一的输入变量：标签/内容
    var tagInput by remember { mutableStateOf("") }

    var totalHours by remember { mutableStateOf("0") }
    var totalMinutes by remember { mutableStateOf("30") }

    var selectedMode by remember(initialMode) { mutableStateOf(initialMode) }
    var expanded by remember { mutableStateOf(false) }

    var customFocusMins by remember { mutableStateOf("25") }
    var customBreakMins by remember { mutableStateOf("5") }

    val finalTotalMinutes = (totalHours.toIntOrNull() ?: 0) * 60 + (totalMinutes.toIntOrNull() ?: 0)
    val finalFocusMins = if (selectedMode.id == "custom") (customFocusMins.toIntOrNull() ?: 25) else selectedMode.defaultFocusMinutes
    val finalBreakMins = if (selectedMode.id == "custom") (customBreakMins.toIntOrNull() ?: 5) else selectedMode.defaultBreakMinutes

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color(0xFF1E1E24),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 20.dp)
            ) {
                Text("创建专注任务", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                // 🟢 1. 唯一的输入框：任务标签/内容
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("你要专注做什么？", color = CyberTextSub) },
                    placeholder = { Text("例如: 背单词, 写代码, 健身", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberPrimary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CyberPrimary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. 总时长输入
                Text("设定总目标时长", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    TimeInputBox(
                        value = totalHours,
                        onValueChange = { if (it.length <= 2) totalHours = it.filter { char -> char.isDigit() } },
                        label = "小时"
                    )
                    Text(
                        ":",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp).offset(y = (-10).dp)
                    )
                    TimeInputBox(
                        value = totalMinutes,
                        onValueChange = { if (it.length <= 2) totalMinutes = it.filter { char -> char.isDigit() } },
                        label = "分钟"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. 模式选择
                Text("模式选择", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedMode.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = CyberPrimary
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        allModes.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(text = mode.name) },
                                onClick = {
                                    selectedMode = mode
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedMode.id == "custom") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text("自定义单轮规则", color = CyberPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SmallInputBox(label = "专注(分)", value = customFocusMins, onValueChange = { customFocusMins = it })
                            Spacer(modifier = Modifier.width(16.dp))
                            SmallInputBox(label = "休息(分)", value = customBreakMins, onValueChange = { customBreakMins = it })
                        }
                    }
                }

                if (selectedMode.id != "custom") {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 4. 开始按钮
                Button(
                    onClick = {
                        // 🟢 核心改动：只处理 Tag
                        val finalInput = if (tagInput.isBlank()) "默认" else tagInput

                        // 将输入的标签，既作为 TaskName 传显示，也作为 Tag 传统计
                        onStartFocus(finalInput, finalTotalMinutes, finalFocusMins, finalBreakMins, finalInput)
                    },
                    enabled = finalTotalMinutes > 0,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberPrimary,
                        disabledContainerColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (finalTotalMinutes > 0) "开始专注 ($finalTotalMinutes 分钟)" else "请设置时间",
                        color = if (finalTotalMinutes > 0) Color.Black else Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// 辅助组件保持不变
@Composable
fun RowScope.TimeInputBox(value: String, onValueChange: (String) -> Unit, label: String) {
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = CyberPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberPrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Text(label, color = CyberTextSub, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun RowScope.SmallInputBox(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .weight(1f)
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 3) onValueChange(it.filter { c -> c.isDigit() }) },
            textStyle = TextStyle(color = CyberPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.width(40.dp)
        )
    }
}