package com.example.focusflow.utils

import android.graphics.Bitmap
import kotlin.math.abs

/**
 * ═══════════════════════════════════════════════════════════════
 * 花园工具函数
 * ═══════════════════════════════════════════════════════════════
 */

/**
 * 自动检测贴图底部空白比例
 * 从图片底部向上扫描，找到第一个含有非透明/非黑色像素的行
 * 
 * @param bitmap 植物贴图
 * @return 底部空白比例 (0.0-1.0)
 */
fun calculateBottomPadding(bitmap: Bitmap): Float {
    val width = bitmap.width
    val height = bitmap.height
    
    // 从底部向上扫描，找到第一个有内容的行
    for (y in height - 1 downTo 0) {
        var hasContent = false
        for (x in 0 until width) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = (pixel shr 24) and 0xFF
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            
            // 检查是否为非透明、非纯黑的像素
            // 允许一定的容差，避免边缘锯齿误判
            val isTransparent = alpha < 10
            val isBlack = red < 10 && green < 10 && blue < 10
            
            if (!isTransparent && !isBlack) {
                hasContent = true
                break
            }
        }
        if (hasContent) {
            // 找到第一个有内容的行，计算底部空白
            val emptyRows = height - 1 - y
            return emptyRows.toFloat() / height.toFloat()
        }
    }
    
    // 整张图都是空白（不应该发生）
    return 0f
}

/**
 * ═══════════════════════════════════════════════════════════════
 * 等距投影坐标转换工具
 * ═══════════════════════════════════════════════════════════════
 */

/**
 * 网格坐标转等距投影屏幕坐标
 * 
 * @param col 网格列坐标
 * @param row 网格行坐标
 * @param tileWidth 地块宽度
 * @param tileHeight 地块高度
 * @return 等距投影后的屏幕坐标 (isoX, isoY)
 */
fun gridToIso(col: Int, row: Int, tileWidth: Float, tileHeight: Float): Pair<Float, Float> {
    val isoX = (col - row) * (tileWidth / 2f)
    val isoY = (col + row) * (tileHeight / 2f)
    return Pair(isoX, isoY)
}

/**
 * 等距投影屏幕坐标转网格坐标
 * 
 * @param isoX 等距投影 X 坐标
 * @param isoY 等距投影 Y 坐标
 * @param tileWidth 地块宽度
 * @param tileHeight 地块高度
 * @return 网格坐标 (col, row)
 */
fun isoToGrid(isoX: Float, isoY: Float, tileWidth: Float, tileHeight: Float): Pair<Int, Int> {
    val tw2 = tileWidth / 2f
    val th2 = tileHeight / 2f
    val col = ((isoX / tw2) + (isoY / th2)) / 2f
    val row = ((isoY / th2) - (isoX / tw2)) / 2f
    return Pair(kotlin.math.round(col).toInt(), kotlin.math.round(row).toInt())
}

/**
 * 屏幕坐标转网格坐标（考虑缩放和偏移）
 * 
 * @param screenX 屏幕 X 坐标
 * @param screenY 屏幕 Y 坐标
 * @param centerX Canvas 中心 X
 * @param centerY Canvas 中心 Y
 * @param offsetX X 偏移量
 * @param offsetY Y 偏移量
 * @param scale 缩放比例
 * @param tileWidth 地块宽度
 * @param tileHeight 地块高度
 * @return 网格坐标 (col, row)
 */
fun screenToGrid(
    screenX: Float, screenY: Float,
    centerX: Float, centerY: Float,
    offsetX: Float, offsetY: Float,
    scale: Float,
    tileWidth: Float, tileHeight: Float
): Pair<Int, Int> {
    // 1. 逆向计算：从屏幕坐标转换回 Canvas 本地绝对坐标
    val canvasX = (screenX - centerX - offsetX) / scale
    val canvasY = (screenY - centerY - offsetY) / scale
    
    // 2. 逆向投影
    return isoToGrid(canvasX, canvasY, tileWidth, tileHeight)
}

/**
 * 判断点是否在菱形地块内
 * 
 * @param canvasX Canvas 本地 X 坐标
 * @param canvasY Canvas 本地 Y 坐标
 * @param isoX 地块等距投影中心 X
 * @param isoY 地块等距投影中心 Y
 * @param tileWidth 地块宽度
 * @param tileHeight 地块高度
 * @return 是否在菱形内
 */
fun isPointInDiamond(
    canvasX: Float, canvasY: Float,
    isoX: Float, isoY: Float,
    tileWidth: Float, tileHeight: Float
): Boolean {
    val dx = abs(canvasX - isoX)
    val dy = abs(canvasY - isoY)
    // 判定是否在菱形内：dx/(tw/2) + dy/(th/2) <= 1
    return dx / (tileWidth / 2f) + dy / (tileHeight / 2f) <= 1.1f
}
