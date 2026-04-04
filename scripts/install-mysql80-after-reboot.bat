@echo off
chcp 65001 >nul
title Install MySQL80 from D (after reboot)

echo =====================================================
echo  请先重启电脑；重启后不要打开 services.msc 再运行本脚本
echo  必须以管理员身份运行
echo =====================================================
echo.

set "MYSQL_HOME=D:\MySQL\MySQL Server 8.0"
set "MysqldExe=%MYSQL_HOME%\bin\mysqld.exe"
set "MyIni=%MYSQL_HOME%\my.ini"

if not exist "%MysqldExe%" (
    echo [ERR] Not found: %MysqldExe%
    pause
    exit /b 1
)
if not exist "%MyIni%" (
    echo [ERR] Not found: %MyIni%
    pause
    exit /b 1
)

sc query MySQL80 >nul 2>&1
if %errorlevel%==0 (
    echo MySQL80 仍存在，尝试设为自动并启动...
    sc config MySQL80 start= auto 2>nul
    net start MySQL80 2>nul
    if not errorlevel 1 (
        echo 启动成功。
        sc query MySQL80 | findstr STATE
        pause
        exit /b 0
    )
    echo 启动失败。请执行 sc qc MySQL80 查看路径是否为 D 盘。
    echo 若仍为 C 盘，请关闭所有服务窗口后再次运行 fix-mysql80-service-管理员运行.bat
    pause
    exit /b 1
)

echo [1] 从 D 盘注册服务 MySQL80...
cd /d "%MYSQL_HOME%\bin"
"%MysqldExe%" --install MySQL80 --defaults-file="%MyIni%"
if errorlevel 1 (
    echo [ERR] mysqld --install 失败。若提示 service exists 请再重启一次并勿打开 services.msc。
    pause
    exit /b 1
)

echo [2] 设为自动启动...
sc config MySQL80 start= auto

echo [3] 启动服务...
net start MySQL80
if errorlevel 1 (
    echo [ERR] net start 失败。
    pause
    exit /b 1
)

echo.
echo 完成。
sc query MySQL80 | findstr STATE
pause
