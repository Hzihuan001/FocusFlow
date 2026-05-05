package com.focusflow.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.focusflow.server.common.Result;
import com.focusflow.server.dto.PlantDictRequest;
import com.focusflow.server.entity.PlantDict;
import com.focusflow.server.mapper.PlantDictMapper;
import com.focusflow.server.service.ImageOptimizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 植物图鉴管理控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * GET  /admin/plant           - 获取所有植物
 * GET  /admin/plant/{id}      - 获取单个植物
 * POST /admin/plant           - 新增植物
 * PUT  /admin/plant/{id}      - 更新植物
 * DELETE /admin/plant/{id}    - 删除植物
 * PUT  /admin/plant/{id}/status - 更新植物状态（上架/下架）
 * POST /admin/plant/upload    - 上传植物图片
 */
@Slf4j
@RestController
@RequestMapping("/admin/plant")
@RequiredArgsConstructor
public class AdminPlantController {

    private final PlantDictMapper plantDictMapper;
    private final ImageOptimizationService imageOptimizationService;
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * 获取所有植物（包括下架的）
     */
    @GetMapping
    public Result<List<PlantDict>> getAllPlants() {
        log.info("获取所有植物图鉴");
        
        LambdaQueryWrapper<PlantDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PlantDict::getStatus)
               .orderByAsc(PlantDict::getPlantId);
        
        // 管理后台需要看到所有植物，包括下架的
        List<PlantDict> plants = plantDictMapper.selectList(wrapper);
        return Result.success(plants);
    }

    /**
     * 获取单个植物
     */
    @GetMapping("/{plantId}")
    public Result<PlantDict> getPlant(@PathVariable Integer plantId) {
        log.info("获取植物详情: plantId={}", plantId);
        
        PlantDict plant = plantDictMapper.selectById(plantId);
        if (plant == null) {
            return Result.error("植物不存在");
        }
        return Result.success(plant);
    }

    /**
     * 上传植物图片（自动优化）
     * @param file 图片文件
     * @param resourceCode 可选，植物资源编码（用于命名文件，便于管理）
     */
    @PostMapping("/upload")
    public Result<String> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "resourceCode", required = false) String resourceCode) {
        log.info("上传植物图片: {}, 大小: {}KB, resourceCode: {}", 
                file.getOriginalFilename(), file.getSize() / 1024, resourceCode);
        
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }
        
        try {
            // 获取上传目录的绝对路径
            // 优先使用配置的 uploadDir，如果是相对路径则转换为绝对路径
            Path uploadPath;
            if (uploadDir.startsWith("/")) {
                // 已经是绝对路径
                uploadPath = Paths.get(uploadDir, "plants");
            } else {
                // 相对路径，转换为绝对路径（相对于当前工作目录）
                uploadPath = Paths.get(uploadDir, "plants").toAbsolutePath();
            }
            
            log.info("上传目录绝对路径: {}", uploadPath);
            
            // 创建上传目录
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("创建上传目录: {}", uploadPath);
            }
            
            // 检查目录是否可写
            if (!Files.isWritable(uploadPath)) {
                log.error("上传目录不可写: {}", uploadPath);
                return Result.error("上传目录不可写，请检查权限");
            }
            
            // 生成文件名：优先使用 resourceCode，否则使用 UUID
            String filename;
            if (resourceCode != null && !resourceCode.trim().isEmpty()) {
                // 使用 resourceCode 作为文件名，便于识别和管理
                filename = resourceCode.trim() + ".png";
            } else {
                filename = UUID.randomUUID().toString() + ".png";
            }
            Path targetPath = uploadPath.resolve(filename);
            
            // 保存原始文件到临时位置
            Path tempPath = uploadPath.resolve("temp_" + filename);
            file.transferTo(tempPath.toFile());
            
            // 使用图片优化服务处理
            try {
                imageOptimizationService.optimizeImage(tempPath.toFile(), targetPath);
                log.info("图片优化成功: {}", imageOptimizationService.getOptimizationInfo());
            } catch (Exception e) {
                log.warn("图片优化失败，使用原图: {}", e.getMessage());
                Files.copy(tempPath, targetPath);
            } finally {
                Files.deleteIfExists(tempPath);
            }
            
            // 返回访问URL
            String imageUrl = "/api/static/plants/" + filename;
            log.info("图片上传成功: {}, 大小: {}KB", imageUrl, targetPath.toFile().length() / 1024);
            
            return Result.success("上传成功", imageUrl);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 新增植物
     */
    @PostMapping
    public Result<PlantDict> addPlant(@RequestBody PlantDictRequest request) {
        log.info("新增植物: name={}", request.getPlantName());
        
        PlantDict plant = new PlantDict();
        plant.setPlantName(request.getPlantName());
        plant.setDescription(request.getDescription());
        plant.setDropWeight(request.getDropWeight());
        plant.setResourceCode(request.getResourceCode());
        plant.setImageUrl(request.getImageUrl());
        plant.setColorHex(request.getColorHex());
        plant.setPurifyRange(request.getPurifyRange());
        plant.setWidth(request.getWidth());
        plant.setScale(request.getScale() != null ? request.getScale() : 100); // 默认100为原大小
        plant.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        
        long currentTime = System.currentTimeMillis();
        plant.setCreatedAt(currentTime);
        plant.setUpdatedAt(currentTime);
        
        plantDictMapper.insert(plant);
        log.info("植物新增成功: plantId={}", plant.getPlantId());
        
        return Result.success("植物新增成功", plant);
    }

    /**
     * 更新植物
     */
    @PutMapping("/{plantId}")
    public Result<PlantDict> updatePlant(
            @PathVariable Integer plantId,
            @RequestBody PlantDictRequest request) {
        log.info("更新植物: plantId={}", plantId);
        
        PlantDict existing = plantDictMapper.selectById(plantId);
        if (existing == null) {
            return Result.error("植物不存在");
        }
        
        existing.setPlantName(request.getPlantName());
        existing.setDescription(request.getDescription());
        existing.setDropWeight(request.getDropWeight());
        existing.setResourceCode(request.getResourceCode());
        if (request.getImageUrl() != null) {
            existing.setImageUrl(request.getImageUrl());
        }
        existing.setColorHex(request.getColorHex());
        existing.setPurifyRange(request.getPurifyRange());
        existing.setWidth(request.getWidth());
        if (request.getScale() != null) {
            existing.setScale(request.getScale());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        existing.setUpdatedAt(System.currentTimeMillis());
        
        plantDictMapper.updateById(existing);
        log.info("植物更新成功: plantId={}", plantId);
        
        return Result.success("植物更新成功", existing);
    }

    /**
     * 删除植物
     */
    @DeleteMapping("/{plantId}")
    public Result<Boolean> deletePlant(@PathVariable Integer plantId) {
        log.info("删除植物: plantId={}", plantId);
        
        int rows = plantDictMapper.deleteById(plantId);
        return rows > 0 
            ? Result.success("植物删除成功", true)
            : Result.error("植物不存在");
    }

    /**
     * 更新植物状态（上架/下架）
     */
    @PutMapping("/{plantId}/status")
    public Result<Boolean> updateStatus(
            @PathVariable Integer plantId,
            @RequestParam Integer status) {
        log.info("更新植物状态: plantId={}, status={}", plantId, status);
        
        LambdaUpdateWrapper<PlantDict> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PlantDict::getPlantId, plantId)
               .set(PlantDict::getStatus, status)
               .set(PlantDict::getUpdatedAt, System.currentTimeMillis());
        
        int rows = plantDictMapper.update(null, wrapper);
        return rows > 0 
            ? Result.success(status == 1 ? "已上架" : "已下架", true)
            : Result.error("植物不存在");
    }
}
