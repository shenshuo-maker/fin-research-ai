MySQL80 报错 1072「已标记为删除」时怎么办
========================================

原因
----
执行 sc delete 时，若「服务」窗口 services.msc 仍打开，或其它程序正在访问该服务，
Windows 只会把服务「标记为删除」，在释放句柄前不能真正删掉。此时会出现：
  DeleteService FAILED 1072: The specified service has been marked for deletion.

正确做法（推荐）
--------------
1. 关闭所有「服务」窗口（services.msc）、计算机管理里的服务、任务管理器里若打开了服务页也关掉。
2. 重启电脑。（多数情况下重启后 MySQL80 会彻底消失）
3. 重启后先不要打开 services.msc。
4. 右键「以管理员身份运行」：
      install-mysql80-after-reboot.bat
   该脚本只做「从 D 盘注册服务并启动」，不再执行 delete。

若重启后仍提示服务已存在且仍是 C 盘路径
--------------------------------------
再以管理员运行一次 fix-mysql80-service-管理员运行.bat（先关 services.msc），
或把完整报错截图发给协助者。

你的 D 盘路径（默认）
--------------------
D:\MySQL\MySQL Server 8.0
若安装目录不同，请用记事本编辑 .bat 里的 MYSQL_HOME。
