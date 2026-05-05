package com.focusflow.server.controller;

import com.focusflow.server.common.Result;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 验证码控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@RestController
@RequestMapping("/admin/captcha")
public class CaptchaController {

    private static final String CAPTCHA_SESSION_KEY = "ADMIN_CAPTCHA_CODE";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final Random random = new Random();

    /**
     * 生成验证码图片
     */
    @GetMapping("/image")
    public Result<Map<String, String>> getCaptcha(HttpSession session) {
        // 生成验证码文本
        String code = generateCode();
        
        // 存入Session
        session.setAttribute(CAPTCHA_SESSION_KEY, code.toLowerCase());
        log.debug("生成验证码: {}", code);
        
        // 创建图片
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        // 抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 背景色 - 深色赛博风格
        g.setColor(new Color(15, 15, 25));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        
        // 绘制干扰线
        drawInterferenceLines(g);
        
        // 绘制验证码字符
        drawCode(g, code);
        
        // 绘制噪点
        drawNoise(g);
        
        g.dispose();
        
        // 转Base64
        String base64Image = imageToBase64(image);
        
        Map<String, String> data = new HashMap<>();
        data.put("img", "data:image/png;base64," + base64Image);
        
        return Result.success(data);
    }

    /**
     * 验证验证码
     */
    public static boolean verifyCaptcha(HttpSession session, String inputCode) {
        if (inputCode == null || inputCode.isEmpty()) {
            return false;
        }
        
        String sessionCode = (String) session.getAttribute(CAPTCHA_SESSION_KEY);
        session.removeAttribute(CAPTCHA_SESSION_KEY); // 验证后立即删除
        
        return sessionCode != null && sessionCode.equalsIgnoreCase(inputCode.trim());
    }

    /**
     * 生成随机验证码
     */
    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 绘制干扰线
     */
    private void drawInterferenceLines(Graphics2D g) {
        for (int i = 0; i < 6; i++) {
            // 赛博风颜色
            g.setColor(new Color(
                random.nextInt(100) + 50,
                random.nextInt(150) + 100,
                random.nextInt(100) + 50
            ));
            g.setStroke(new BasicStroke(1.5f));
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * 绘制验证码字符
     */
    private void drawCode(Graphics2D g, String code) {
        int x = 15;
        for (int i = 0; i < code.length(); i++) {
            // 赛博绿色调
            g.setColor(new Color(
                random.nextInt(100) + 50,
                random.nextInt(155) + 100,
                random.nextInt(100) + 50
            ));
            
            // 随机字体
            String[] fonts = {"Arial", "Courier New", "Georgia"};
            g.setFont(new Font(fonts[random.nextInt(fonts.length)], Font.BOLD, 24 + random.nextInt(6)));
            
            // 随机旋转
            double angle = (random.nextDouble() - 0.5) * 0.3;
            g.rotate(angle, x + 12, HEIGHT / 2.0);
            
            g.drawString(String.valueOf(code.charAt(i)), x, HEIGHT - 8);
            
            g.rotate(-angle, x + 12, HEIGHT / 2.0);
            x += 25;
        }
    }

    /**
     * 绘制噪点
     */
    private void drawNoise(Graphics2D g) {
        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(
                random.nextInt(200) + 55,
                random.nextInt(200) + 55,
                random.nextInt(200) + 55
            ));
            g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
        }
    }

    /**
     * 图片转Base64
     */
    private String imageToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            log.error("验证码图片转换失败", e);
            return "";
        }
    }
}
