@echo off
setlocal
cd /d "%~dp0"
java -jar Scramble.jar %*
if errorlevel 1 pause
