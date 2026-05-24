@echo off
setlocal

cd /d "%~dp0"

set "COMPOSE_FILE=docker-compose.local.yaml"
set "DUMP_FILE=db-dumps\dump.sql"

if not exist "%DUMP_FILE%" (
  echo Missing %DUMP_FILE%.
  echo Put your PostgreSQL dump at %CD%\%DUMP_FILE% and run this file again.
  exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
  echo Docker is not running. Start Docker Desktop, then run install.bat again.
  exit /b 1
)

echo [1/3] Resetting local containers and volumes...
docker compose -f "%COMPOSE_FILE%" down -v --remove-orphans
if errorlevel 1 exit /b 1

echo.
echo [2/3] Building and starting local stack...
echo Postgres will import %DUMP_FILE% during first startup.
docker compose -f "%COMPOSE_FILE%" up --build
if errorlevel 1 exit /b 1

echo.
echo [3/3] Done.
