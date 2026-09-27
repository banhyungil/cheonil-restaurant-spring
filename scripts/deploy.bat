@echo off
chcp 65001 > nul

rem ============================================================
rem  Cheonil Restaurant - Deploy Script (매장 PC, Windows)
rem
rem  실행 순서:
rem    1) Backend (cheonil-restaurant-spring) git pull  - compose / scripts 최신화
rem    2) docker compose pull app web                   - Docker Hub 이미지 수신
rem    3) docker compose up -d --no-build               - 변경된 컨테이너만 재기동
rem
rem  이미지는 개발 PC 에서 scripts\build-push.sh 로 미리 push.
rem  롤백: .env 에 APP_TAG / WEB_TAG=<git-sha> 지정 후 재실행 (기본 latest).
rem
rem  사전 조건:
rem    - Docker Desktop 실행 중 + docker login 완료 (private 이미지)
rem    - .env 작성 (DB_PASSWORD, CLOUDFLARED_TOKEN, GOOGLE_API_KEY)
rem    - git, docker CLI PATH 등록
rem ============================================================

set "SCRIPT_DIR=%~dp0"
set "BACKEND_DIR=%SCRIPT_DIR%.."

pushd "%BACKEND_DIR%" || (
    echo [ERROR] Backend 디렉터리를 찾을 수 없습니다.
    exit /b 1
)

echo.
echo === [1/3] Backend git pull ===
echo path: %BACKEND_DIR%
git pull
if errorlevel 1 (
    echo [ERROR] Backend git pull 실패
    popd
    exit /b 1
)

echo.
echo === [2/3] docker compose pull ===
rem app / web 만 - db / whisper / cloudflared 는 의도치 않은 버전 업 방지.
docker compose pull app web
if errorlevel 1 (
    echo [ERROR] docker compose pull 실패 ^(docker login 확인^)
    popd
    exit /b 1
)

echo.
echo === [3/3] docker compose up -d ===
docker compose up -d --no-build
if errorlevel 1 (
    echo [ERROR] docker compose up 실패
    popd
    exit /b 1
)
popd

echo.
echo === 배포 완료 ===
exit /b 0
