-- MySQL 8：创建业务库（utf8mb4）
-- 使用方式（在「以管理员身份运行」的 CMD 或 PowerShell 中，按你本机密码修改 -p）：
--   "D:\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < scripts\init-finresearch.sql
-- 或在 MySQL Workbench / 客户端中粘贴执行以下语句：

CREATE DATABASE IF NOT EXISTS finresearch
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 可选：查看
-- SHOW CREATE DATABASE finresearch;
