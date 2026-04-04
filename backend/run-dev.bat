@echo off
cd /d "%~dp0"
chcp 65001 >nul

if exist "%~dp0local-env.bat" call "%~dp0local-env.bat"
if not defined MYSQL_PORT set "MYSQL_PORT=3306"
if not defined MYSQL_PASSWORD set "MYSQL_PASSWORD=root"
set "MYSQL_USER=root"

echo 工作目录: %CD%
echo 连接: localhost:%MYSQL_PORT% / finresearch
echo 日志同时显示在窗口并写入: last-backend-run.log
echo 密码与端口可放在 local-env.bat（复制 local-env.bat.example）
echo.

del "%~dp0last-backend-run.log" 2>nul

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "Set-Location -LiteralPath '%CD%'; $env:MYSQL_PORT='%MYSQL_PORT%'; $env:MYSQL_PASSWORD='%MYSQL_PASSWORD%'; $env:MYSQL_USER='%MYSQL_USER%'; mvn spring-boot:run *>&1 | Tee-Object -FilePath '%CD%\last-backend-run.log'"

set "EC=%ERRORLEVEL%"
echo.
echo ============================================
echo 已结束，退出码: %EC%
echo 完整日志: %CD%\last-backend-run.log
echo ============================================
findstr /i /c:"Started FinResearchApplication" /c:"APPLICATION FAILED" /c:"CommunicationsException" /c:"Access denied" /c:"refused" "%~dp0last-backend-run.log" 2>nul
echo.
pause
