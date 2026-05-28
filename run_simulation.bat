@echo off
rem Build and run the aircraft simulation on Windows.
rem Pass any program args after the script name, e.g. run_simulation.bat --inject-failures
cd /d "%~dp0src"
javac *.java
if errorlevel 1 exit /b 1
java -Dswing.aatext=true -Dsun.java2d.opengl=true Main %*
