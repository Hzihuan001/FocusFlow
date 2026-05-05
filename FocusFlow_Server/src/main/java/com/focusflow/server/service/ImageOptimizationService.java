package com.focusflow.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 图片优化服务 - 自动压缩和缩放上传的图片
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【功能】
 * 1. 自动缩放到指定最大尺寸
 * 2. 压缩图片质量
 * 3. 支持 PNG 透明背景保留
 * 4. 生成标准化的输出格式
 *
 * 【配置】
 * - 最大宽度：512px（适合手机端显示）
 * - 最大高度：512px
 * - JPEG 质量：0.8
 * - PNG 压缩：默认
 */
@Slf4j
@Service
public class ImageOptimizationService {

    /**
     * 植物图片最大宽度（像素）
     */
    private static final int MAX_WIDTH = 512;

    /**
     * 植物图片最大高度（像素）
     */
    private static final int MAX_HEIGHT = 512;

    /**
     * 缩略图尺寸
     */
    private static final int THUMBNAIL_SIZE = 128;

    /**
     * JPEG 压缩质量 (0.0 - 1.0)
     */
    private static final float JPEG_QUALITY = 0.8f;

    /**
     * 优化图片并保存
     *
     * @param sourceFile 源文件
     * @param targetPath 目标路径
     * @return 优化后的文件路径
     */
    public Path optimizeImage(File sourceFile, Path targetPath) throws IOException {
        log.info("开始优化图片: {} -> {}", sourceFile.getName(), targetPath.getFileName());

        // 读取原始图片
        BufferedImage originalImage = ImageIO.read(sourceFile);
        if (originalImage == null) {
            throw new IOException("无法读取图片文件: " + sourceFile.getName());
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        log.info("原始图片尺寸: {}x{}", originalWidth, originalHeight);

        // 计算缩放后的尺寸（保持宽高比）
        int[] scaledSize = calculateScaledSize(originalWidth, originalHeight, MAX_WIDTH, MAX_HEIGHT);
        int scaledWidth = scaledSize[0];
        int scaledHeight = scaledSize[1];

        // 缩放图片
        BufferedImage scaledImage = scaleImage(originalImage, scaledWidth, scaledHeight);

        // 根据格式选择输出方式
        String filename = targetPath.getFileName().toString().toLowerCase();
        String format = filename.endsWith(".png") ? "PNG" : "JPEG";

        // 保存优化后的图片
        if ("PNG".equals(format)) {
            // PNG 格式，保留透明通道
            ImageIO.write(scaledImage, "PNG", targetPath.toFile());
        } else {
            // JPEG 格式，应用压缩质量
            saveAsJpeg(scaledImage, targetPath);
        }

        long originalSize = sourceFile.length();
        long optimizedSize = targetPath.toFile().length();
        double compressionRatio = (1 - (double) optimizedSize / originalSize) * 100;

        log.info("图片优化完成: {}x{} -> {}x{}, 大小: {}KB -> {}KB, 压缩率: {:.1f}%",
                originalWidth, originalHeight, scaledWidth, scaledHeight,
                originalSize / 1024, optimizedSize / 1024, compressionRatio);

        return targetPath;
    }

    /**
     * 生成缩略图
     *
     * @param sourceFile 源文件
     * @param thumbnailPath 缩略图路径
     * @return 缩略图路径
     */
    public Path generateThumbnail(File sourceFile, Path thumbnailPath) throws IOException {
        BufferedImage originalImage = ImageIO.read(sourceFile);
        if (originalImage == null) {
            throw new IOException("无法读取图片文件: " + sourceFile.getName());
        }

        // 缩放到缩略图尺寸
        int[] scaledSize = calculateScaledSize(
            originalImage.getWidth(), 
            originalImage.getHeight(), 
            THUMBNAIL_SIZE, 
            THUMBNAIL_SIZE
        );

        BufferedImage thumbnail = scaleImage(originalImage, scaledSize[0], scaledSize[1]);

        // 保存缩略图
        String filename = thumbnailPath.getFileName().toString().toLowerCase();
        if (filename.endsWith(".png")) {
            ImageIO.write(thumbnail, "PNG", thumbnailPath.toFile());
        } else {
            saveAsJpeg(thumbnail, thumbnailPath);
        }

        log.info("缩略图生成完成: {}x{}, 大小: {}KB",
                scaledSize[0], scaledSize[1], 
                thumbnailPath.toFile().length() / 1024);

        return thumbnailPath;
    }

    /**
     * 计算缩放后的尺寸（保持宽高比）
     */
    private int[] calculateScaledSize(int width, int height, int maxWidth, int maxHeight) {
        if (width <= maxWidth && height <= maxHeight) {
            return new int[]{width, height};
        }

        double widthRatio = (double) maxWidth / width;
        double heightRatio = (double) maxHeight / height;
        double ratio = Math.min(widthRatio, heightRatio);

        return new int[]{
            (int) Math.round(width * ratio),
            (int) Math.round(height * ratio)
        };
    }

    /**
     * 缩放图片（高质量）
     */
    private BufferedImage scaleImage(BufferedImage original, int width, int height) {
        // 创建目标图片（支持透明）
        int imageType = original.getTransparency() != BufferedImage.OPAQUE 
            ? BufferedImage.TYPE_INT_ARGB 
            : BufferedImage.TYPE_INT_RGB;
        
        BufferedImage scaled = new BufferedImage(width, height, imageType);

        // 使用高质量缩放
        Graphics2D g2d = scaled.createGraphics();
        try {
            // 设置渲染提示以提高质量
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
                RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制缩放后的图片
            g2d.drawImage(original, 0, 0, width, height, null);
        } finally {
            g2d.dispose();
        }

        return scaled;
    }

    /**
     * 保存为 JPEG 格式（带压缩质量控制）
     */
    private void saveAsJpeg(BufferedImage image, Path targetPath) throws IOException {
        // 如果原图有透明通道，需要转换为 RGB
        BufferedImage rgbImage = image;
        if (image.getTransparency() != BufferedImage.OPAQUE) {
            rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = rgbImage.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
        }

        // 使用 ImageIO 的 JPEG 编码器
        ImageIO.write(rgbImage, "JPEG", targetPath.toFile());
    }

    /**
     * 获取优化配置信息
     */
    public String getOptimizationInfo() {
        return String.format("图片优化配置: 最大尺寸=%dx%d, JPEG质量=%.0f%%, 缩略图=%dpx",
            MAX_WIDTH, MAX_HEIGHT, JPEG_QUALITY * 100, THUMBNAIL_SIZE);
    }
}
