@echo off
chcp 65001 >nul
echo 查找占用 8080 端口的进程（多为上次没关干净的 java / Spring Boot）...
echo.

for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo 将结束 PID: %%a
    taskkill /F /PID %%a 2>nul
    if errorlevel 1 (
        echo 若提示拒绝访问，请右键本脚本「以管理员身份运行」
    ) else (
        echo 已结束。
    )
)

echo.
echo 完成后可重新运行 run-dev-h2.bat
pause
