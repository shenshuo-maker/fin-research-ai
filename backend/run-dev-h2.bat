@echo off
REM ===================================================================
REM  本机演示：H2 内存库，无需 MySQL（已修复 H2 方言，可正常建表与登录）
REM  窗口请保持打开；停止按 Ctrl+C
REM ===================================================================
cd /d "%~dp0"
chcp 65001 >nul

echo 工作目录: %CD%
echo.

set SPRING_PROFILES_ACTIVE=h2
call mvn spring-boot:run "-Dspring-boot.run.profiles=h2"

echo.
echo 已退出。若失败请先: mvn -q -DskipTests package
pause
