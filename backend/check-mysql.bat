@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ========== 本机谁在监听 3306 / 3307 ==========
netstat -an | findstr "LISTENING" | findstr ":3306 "
netstat -an | findstr "LISTENING" | findstr ":3307 "
echo.
echo 若只看到 3306，run-dev.bat 里应 set MYSQL_PORT=3306
echo 若只看到 3307，run-dev.bat 里应 set MYSQL_PORT=3307
echo 若都没有，请先启动 MySQL80 服务: net start MySQL80
echo.
pause
