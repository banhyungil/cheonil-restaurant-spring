@echo off
chcp 65001 >nul
REM =====================================================================
REM cheonil 배포 스크립트 (Windows)
REM   1. Frontend git pull
REM   2. Backend  git pull
REM   3. docker compose pull (최신 이미지) + up -d
REM
REM 사용:
REM   - 본 파일을 바탕화면 또는 임의 위치에 복사
REM   - 더블클릭 또는 cmd 에서 실행
REM
REM 경로 변경 시 아래 set 줄 3개만 수정.
REM =====================================================================

REM ─── 경로 (절대경로) ───
set "FRONT_DIR=C:\Users\banhyungil\workspace\projects\cheonil-restaurant-next"
set "BACK_DIR=C:\Users\banhyungil\workspace\projects\cheonil-restaurant-spring"
set "COMPOSE_DIR=%BACK_DIR%"

REM ─── 1. Frontend pull ───
echo.
echo [1/3] Frontend git pull
pushd "%FRONT_DIR%" || (
    echo  ! Frontend dir not found: %FRONT_DIR%
    pause
    exit /b 1
)
git pull
if errorlevel 1 (
    echo  ! Frontend git pull failed
    popd
    pause
    exit /b 1
)
popd

REM ─── 2. Backend pull ───
echo.
echo [2/3] Backend git pull
pushd "%BACK_DIR%" || (
    echo  ! Backend dir not found: %BACK_DIR%
    pause
    exit /b 1
)
git pull
if errorlevel 1 (
    echo  ! Backend git pull failed
    popd
    pause
    exit /b 1
)
popd

REM ─── 3. docker compose ───
echo.
echo [3/3] docker compose pull + up -d
pushd "%COMPOSE_DIR%" || (
    echo  ! Compose dir not found: %COMPOSE_DIR%
    pause
    exit /b 1
)

REM 최신 이미지 가져오기 (Docker Hub 등 registry 의 latest 태그 갱신)
docker compose pull
if errorlevel 1 (
    echo  ! docker compose pull failed
    popd
    pause
    exit /b 1
)

REM 변경된 이미지만 재생성
docker compose up -d
if errorlevel 1 (
    echo  ! docker compose up failed
    popd
    pause
    exit /b 1
)

popd

echo.
echo === Deploy complete ===
docker compose -f "%COMPOSE_DIR%\docker-compose.yaml" ps
echo.
pause

REM ─── 참고 ───
REM 로컬 소스에서 직접 이미지 빌드 후 띄우려면 위 [3/3] 부분을:
REM   docker compose up -d --build
REM 로 변경. 이 경우 git pull 한 소스가 그대로 빌드됨 (registry 무관).
