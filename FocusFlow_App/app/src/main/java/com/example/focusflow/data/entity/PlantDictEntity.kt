package com.example.focusflow.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 植物字典实体
 * 表名: app_plant_dict
 * 定义所有可用的植物类型及其属性
 */
@Entity(tableName = "app_plant_dict")
data class PlantDictEntity(
    @PrimaryKey
    val plantId: Int,            // 植物 ID (Int 类型主键)
    val plantName: String,       // 植物名称 (如 "仙人掌", "向日葵")
    val maxGrowth: Int,          // 最大生长值 (达到此值即成熟)
    val resourceCode: String,    // 资源代码 (用于引用图片资源)
    val description: String,     // 植物描述
    val width: Int,              // 植物占用区域：1=1×1, 2=2×2, 3=3×3
    val purifyRange: Int = 1,    // 净化辐射半径（格数）
    val dropWeight: Int = 1,     // 掉落权重（权重越高越容易获得）
    val rarity: Int = 0,         // 稀有度: 0=N, 1=R, 2=SR, 3=SSR
    val imageUrl: String? = null, // 植物图片URL（从服务端获取）
    val scale: Int = 100        // 显示缩放比例（百分比）：100=原大小, 50=缩小一半, 200=放大两倍
)
