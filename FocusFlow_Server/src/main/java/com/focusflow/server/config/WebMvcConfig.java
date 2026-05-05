package com.focusflow.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置
 * 配置静态资源映射和管理员权限拦截器
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册管理员权限拦截器
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")           // 拦截所有 /admin/** 路径
                .excludePathPatterns(
                        "/admin/auth/login",            // 排除登录接口
                        "/admin/captcha/**",            // 排除验证码接口
                        "/admin/redirect/**"            // 排除重定向接口（如有）
                );
        
        log.info("管理员权限拦截器已启用: /admin/** (排除登录和验证码接口)");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取上传目录的绝对路径
        File uploadDirFile = new File(uploadDir);
        String uploadPath = uploadDirFile.getAbsolutePath();
        
        // 确保路径格式正确（使用正斜杠）
        uploadPath = uploadPath.replace("\\", "/");
        if (!uploadPath.endsWith("/")) {
            uploadPath = uploadPath + "/";
        }
        
        // 配置静态资源映射：/api/static/** -> uploads/
        // 同时支持 /static/** 和 /api/static/** 路径
        registry.addResourceHandler("/static/**", "/api/static/**")
                .addResourceLocations("file:" + uploadPath);
        
        log.info("静态资源映射: /static/**, /api/static/** -> file:{}", uploadPath);
    }
}