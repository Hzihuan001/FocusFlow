#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# FocusFlow Server 一键部署脚本 (Ubuntu 24.04 LTS)
# ═══════════════════════════════════════════════════════════════════════════════

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 配置变量
APP_NAME="focusflow"
APP_DIR="/var/www/focusflow"
JAR_NAME="focusflow-server-1.0.0.jar"
SERVICE_NAME="focusflow"

echo -e "${GREEN}═════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}FocusFlow Server 部署脚本${NC}"
echo -e "${GREEN}═════════════════════════════════════════════════════════════${NC}"

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then
    echo -e "${YELLOW}请使用 sudo 运行此脚本${NC}"
    exit 1
fi

# 检查必要的命令
check_dependencies() {
    echo -e "${YELLOW}检查依赖...${NC}"
    
    local missing=()
    
    command -v java >/dev/null 2>&1 || missing+=("openjdk-17-jdk")
    command -v mysql >/dev/null 2>&1 || missing+=("mysql-server")
    command -v nginx >/dev/null 2>&1 || missing+=("nginx")
    
    if [ ${#missing[@]} -gt 0 ]; then
        echo -e "${RED}缺少以下依赖: ${missing[*]}${NC}"
        echo -e "${YELLOW}请运行: sudo apt install ${missing[*]} -y${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ 依赖检查通过${NC}"
}

# 创建目录结构
setup_directories() {
    echo -e "${YELLOW}创建目录结构...${NC}"
    
    mkdir -p $APP_DIR
    mkdir -p $APP_DIR/uploads/plants
    mkdir -p $APP_DIR/logs
    
    echo -e "${GREEN}✓ 目录创建完成${NC}"
}

# 创建 systemd 服务文件
create_systemd_service() {
    echo -e "${YELLOW}创建 systemd 服务...${NC}"
    
    cat > /etc/systemd/system/$SERVICE_NAME.service << EOF
[Unit]
Description=FocusFlow Server
After=network.target mysql.service

[Service]
Type=simple
User=www-data
WorkingDirectory=$APP_DIR
EnvironmentFile=/etc/environment
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar $APP_DIR/$JAR_NAME
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

    systemctl daemon-reload
    echo -e "${GREEN}✓ systemd 服务创建完成${NC}"
}

# 配置环境变量
configure_environment() {
    echo -e "${YELLOW}配置环境变量...${NC}"
    
    # 检查是否已配置
    if grep -q "DB_PASSWORD" /etc/environment 2>/dev/null; then
        echo -e "${GREEN}✓ 环境变量已配置${NC}"
        return
    fi
    
    echo -e "${YELLOW}请输入数据库配置:${NC}"
    read -p "数据库主机 [localhost]: " db_host
    db_host=${db_host:-localhost}
    
    read -p "数据库端口 [3306]: " db_port
    db_port=${db_port:-3306}
    
    read -p "数据库名称 [focus_flow]: " db_name
    db_name=${db_name:-focus_flow}
    
    read -p "数据库用户名 [focusflow]: " db_user
    db_user=${db_user:-focusflow}
    
    read -s -p "数据库密码: " db_pass
    echo
    
    # 追加到 /etc/environment
    cat >> /etc/environment << EOF

# FocusFlow Configuration
DB_HOST=$db_host
DB_PORT=$db_port
DB_NAME=$db_name
DB_USERNAME=$db_user
DB_PASSWORD=$db_pass
UPLOAD_DIR=$APP_DIR/uploads
EOF
    
    echo -e "${GREEN}✓ 环境变量配置完成${NC}"
}

# 设置权限
set_permissions() {
    echo -e "${YELLOW}设置权限...${NC}"
    
    chown -R www-data:www-data $APP_DIR
    chmod -R 755 $APP_DIR
    
    echo -e "${GREEN}✓ 权限设置完成${NC}"
}

# 主流程
main() {
    check_dependencies
    setup_directories
    create_systemd_service
    configure_environment
    set_permissions
    
    echo -e "${GREEN}═════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}部署准备完成！${NC}"
    echo -e "${GREEN}═════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "后续步骤:"
    echo -e "  1. 上传 JAR 包: scp target/$JAR_NAME user@server:$APP_DIR/"
    echo -e "  2. 启动服务: sudo systemctl start $SERVICE_NAME"
    echo -e "  3. 查看状态: sudo systemctl status $SERVICE_NAME"
    echo -e "  4. 查看日志: sudo journalctl -u $SERVICE_NAME -f"
}

main "$@"
