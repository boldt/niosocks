@set BIN=%~dp0

@set CLASSPATH="..\conf"

@for %%a in (..\lib\*.jar) do @call add2cp.bat %%a

@java -classpath %CLASSPATH% org.ineto.niosocks.SocksLauncher %BIN% %1

pause