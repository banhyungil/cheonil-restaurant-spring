@echo off
chcp 65001 > nul

rem ============================================================
rem  Cheonil Restaurant - Deploy Script (Windows)
rem
rem  실행 순서:
rem    1) Frontend (cheonil-restaurant-next) git pull
rem    2) Backend  (cheonil-restaurant-spring) git pull
rem    3) docker compose build
rem    4) docker compose up -d
rem
rem  사전 조건:
rem    - 두 저장소가 sibling 폴더 구조로 위치
rem        <ROOT>\cheonil-restaurant-spring\
rem        <ROOT>\cheonil-restaurant-next\
rem    - Docker Desktop 실행 중
rem    - git, docker CLI PATH 등록
rem ============================================================

set "SCRIPT_DIR=%~dp0"
set "BACKEND_DIR=%SCRIPT_DIR%.."
set "FRONTEND_DIR=%SCRIPT_DIR%..\..\cheonil-restaurant-next"

echo.
echo === [1/4] Frontend git pull ===
echo path: %FRONTEND_DIR%
pushd "%FRONTEND_DIR%" || (
    echo [ERROR] Frontend 디렉터리를 찾을 수 없습니다.
    exit /b 1
)
git pull
if errorlevel 1 (
    echo [ERROR] Frontend git pull 실패
    popd
    exit /b 1
)
popd

echo.
echo === [2/4] Backend git pull ===
echo path: %BACKEND_DIR%
pushd "%BACKEND_DIR%" || (
    echo [ERROR] Backend 디렉터리를 찾을 수 없습니다.
    exit /b 1
)
git pull
if errorlevel 1 (
    echo [ERROR] Backend git pull 실패
    popd
    exit /b 1
)

echo.
echo === [3/4] docker compose build ===
docker compose build
if errorlevel 1 (
    echo [ERROR] docker compose build 실패
    popd
    exit /b 1
)

echo.
echo === [4/4] docker compose up -d ===
docker compose up -d
if errorlevel 1 (
    echo [ERROR] docker compose up 실패
    popd
    exit /b 1
)
popd

echo.
echo === 배포 완료 ===
exit /b 0
