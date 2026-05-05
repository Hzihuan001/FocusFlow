package com.focusflow.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 后台管理系统重定向控制器
 */
@Controller
public class AdminRedirectController {

    /**
     * 重定向到后台管理系统
     */
    @GetMapping("/admin")
    public String admin() {
        return "redirect:/api/admin/index.html";
    }

    /**
     * 重定向到后台管理系统（带斜杠）
     */
    @GetMapping("/admin/")
    public String adminSlash() {
        return "redirect:/api/admin/index.html";
    }
}
