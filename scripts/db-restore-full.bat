@echo off
chcp 65001 > nul

rem ============================================================
rem  Cheonil DB Restore - Full mode
rem
rem  ⚠️ 위험: 기존 cheonil DB 를 완전히 삭제하고 백업으로 재구성합니다.
rem           실행 전 'YES' 확인을 받습니다.
rem
rem  동작:
rem    1) app 컨테이너 정지 (DB 연결 해제)
rem    2) 남은 세션 강제 종료
rem    3) DROP DATABASE / CREATE DATABASE
rem    4) pg_restore 로 .dump 적용
rem    5) app 컨테이너 재시작
rem
rem  사용:
rem    db-restore-full.bat <백업파일.dump>
rem
rem  주의:
rem    - 입력은 db-backup.bat 이 생성한 *.dump (custom format)
rem    - *_data.sql (plain data-only) 는 db-restore-append.bat 사용
rem ============================================================

setlocal

if "%~1"=="" (
    echo 사용법: %~nx0 ^<백업파일.dump^>
    exit /b 1
)

set "BACKUP_FILE=%~1"
if not exist "%BACKUP_FILE%" (
    echo [ERROR] 파일 없음: %BACKUP_FILE%
    exit /b 1
)

set "SCRIPT_DIR=%~dp0"
set "BACKEND_DIR=%SCRIPT_DIR%.."
set "CONTAINER=cheonil-db"
set "DB=cheonil"
set "DB_USER=root"

echo.
echo ⚠️  경고: %DB% DB 가 완전히 삭제되고 아래 백업으로 대체됩니다.
echo     백업 파일: %BACKUP_FILE%
echo.
set /p CONFIRM="계속하려면 'YES' 입력: "
if /I not "%CONFIRM%"=="YES" (
    echo 취소되었습니다.
    exit /b 1
)

echo.
echo === [1/6] app 컨테이너 정지 ===
pushd "%BACKEND_DIR%"
docker compose stop app
popd

echo.
echo === [2/6] 백업 파일을 컨테이너로 복사 ===
docker cp "%BACKUP_FILE%" %CONTAINER%:/tmp/restore.dump
if errorlevel 1 (
    echo [ERROR] docker cp 실패
    goto :restart_app_on_error
)

echo.
echo === [3/6] 잔여 DB 세션 강제 종료 ===
docker exec %CONTAINER% psql -U %DB_USER% -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='%DB%' AND pid<>pg_backend_pid();"

echo.
echo === [4/6] DB DROP / CREATE ===
docker exec %CONTAINER% psql -U %DB_USER% -d postgres -c "DROP DATABASE IF EXISTS %DB%;"
if errorlevel 1 (
    echo [ERROR] DROP DATABASE 실패
    goto :restart_app_on_error
)
docker exec %CONTAINER% psql -U %DB_USER% -d postgres -c "CREATE DATABASE %DB%;"
if errorlevel 1 (
    echo [ERROR] CREATE DATABASE 실패
    goto :restart_app_on_error
)

echo.
echo === [5/6] pg_restore ===
docker exec %CONTAINER% pg_restore -U %DB_USER% -d %DB% --no-owner --no-acl /tmp/restore.dump
if errorlevel 1 (
    echo [WARN] pg_restore 가 일부 경고를 출력했습니다. 위 로그를 확인하세요.
)
docker exec %CONTAINER% rm -f /tmp/restore.dump

echo.
echo === [6/6] app 컨테이너 재시작 ===
pushd "%BACKEND_DIR%"
docker compose start app
popd

echo.
echo === Full 복원 완료 ===
endlocal
exit /b 0

:restart_app_on_error
echo.
echo [복구 시도] app 컨테이너 재시작
pushd "%BACKEND_DIR%"
docker compose start app
popd
docker exec %CONTAINER% rm -f /tmp/restore.dump 2>nul
endlocal
exit /b 1
