@echo off
chcp 65001 >nul
title Fix MySQL80 service D drive

REM 注意：echo 行尾不要用中文逗号，CMD 会当成命令分隔符导致乱报错
echo ============================================
echo 删除错误 MySQL80 服务并用 D 盘 mysqld 重装
echo 请右键本文件 - 以管理员身份运行
echo.
echo 【重要】请先关闭 services.msc 和所有「服务」窗口
echo 否则会出现 1072 已标记为删除 且无法重装
echo ============================================
echo.

set "MYSQL_HOME=D:\MySQL\MySQL Server 8.0"
set "MysqldExe=%MYSQL_HOME%\bin\mysqld.exe"
set "MyIni=%MYSQL_HOME%\my.ini"

if not exist "%MysqldExe%" (
    echo [ERR] Not found: %MysqldExe%
    echo Edit MYSQL_HOME in this bat if your path differs.
    pause
    exit /b 1
)
if not exist "%MyIni%" (
    echo [ERR] Not found: %MyIni%
    pause
    exit /b 1
)

echo [0] End orphan mysqld if any...
taskkill /F /IM mysqld.exe 2>nul

echo [1] Stop service...
sc stop MySQL80 2>nul

echo [2] Windows remove service SCM...
sc delete MySQL80
echo (If "service not exist" that is OK)

echo [3] MySQL remove service (cleans install state)...
"%MysqldExe%" --remove MySQL80 2>nul
echo (If remove failed ignore if service already gone)

echo [4] Wait 5s for SCM to release name...
timeout /t 5 /nobreak >nul

sc query MySQL80 >nul 2>&1
if %errorlevel%==0 (
    echo [WARN] MySQL80 仍在系统中。若你刚看到 1072 或 install 说 already exists:
    echo   1 关闭所有 services.msc 窗口
    echo   2 重启电脑
    echo   3 重启后运行 install-mysql80-after-reboot.bat
    echo.
    pause
    exit /b 1
)

echo [5] Install service from D drive...
cd /d "%MYSQL_HOME%\bin"
"%MysqldExe%" --install MySQL80 --defaults-file="%MyIni%"
if errorlevel 1 (
    echo [ERR] mysqld --install 失败。若提示 The service already exists:
    echo   请重启电脑，勿打开 services.msc，再运行 install-mysql80-after-reboot.bat
    pause
    exit /b 1
)

echo [6] Set start type AUTO (fixes error 1058 disabled)...
sc config MySQL80 start= auto
if errorlevel 1 (
    echo [WARN] sc config start= auto failed
)

echo [7] Start MySQL80...
net start MySQL80
if errorlevel 1 (
    echo [ERR] net start failed. Check message above.
    pause
    exit /b 1
)

echo.
echo ========== DONE ==========
sc query MySQL80 | findstr STATE
echo.
pause
