@echo off
REM 실행 정책 우회 — 키스토어 1회 생성
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0generate-release-keystore.ps1"
if errorlevel 1 pause
