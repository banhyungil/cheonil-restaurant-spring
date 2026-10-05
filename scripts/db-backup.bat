@echo off
chcp 65001 > nul

rem ============================================================
rem  Cheonil DB Backup (Windows)
rem
rem  생성 파일:
rem    <BACKUP_DIR>\cheonil_YYYYMMDD_HHMMSS.dump       (custom format, 전체 복원용)
rem    <BACKUP_DIR>\cheonil_YYYYMMDD_HHMMSS_data.sql   (plain data-only, append 복원용)
rem
rem  사용:
rem    db-backup.bat
rem
rem  환경변수 (선택):
rem    BACKUP_DIR        백업 저장 경로 (기본: <BACKEND_DIR>\backup)
rem    RETENTION_DAYS    이 일수 이전 백업은 자동 삭제 (기본: 14)
rem    BACKUP_GIT_PUSH   1 이면 백업 폴더만 커밋 + push (기본: 1, 0 이면 건너뜀)
rem
rem  사전 조건:
rem    - docker compose 로 cheonil-db 컨테이너가 실행 중
rem ============================================================

setlocal

set "SCRIPT_DIR=%~dp0"
set "BACKEND_DIR=%SCRIPT_DIR%.."

if not defined BACKUP_DIR set "BACKUP_DIR=%BACKEND_DIR%\backup"
if not defined RETENTION_DAYS set "RETENTION_DAYS=14"
if not defined BACKUP_GIT_PUSH set "BACKUP_GIT_PUSH=1"

set "CONTAINER=cheonil-db"
set "DB=cheonil"
set "DB_USER=root"

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format 'yyyyMMdd_HHmmss'"') do set "TS=%%i"
set "DUMP_FILE=%BACKUP_DIR%\cheonil_%TS%.dump"
set "DATA_FILE=%BACKUP_DIR%\cheonil_%TS%_data.sql"

echo.
echo === [1/4] Custom dump (전체 복원용) ===
echo path: %DUMP_FILE%
docker exec %CONTAINER% sh -c "pg_dump -U %DB_USER% -d %DB% -Fc -f /tmp/backup.dump"
if errorlevel 1 (
    echo [ERROR] pg_dump (custom) 실패
    exit /b 1
)
docker cp %CONTAINER%:/tmp/backup.dump "%DUMP_FILE%"
if errorlevel 1 (
    echo [ERROR] docker cp 실패
    exit /b 1
)
docker exec %CONTAINER% rm -f /tmp/backup.dump

echo.
echo === [2/4] Data-only SQL (append 복원용) ===
echo path: %DATA_FILE%
docker exec %CONTAINER% sh -c "pg_dump -U %DB_USER% -d %DB% -Fp --data-only --column-inserts -f /tmp/backup_data.sql"
if errorlevel 1 (
    echo [ERROR] pg_dump (data-only) 실패
    exit /b 1
)
docker cp %CONTAINER%:/tmp/backup_data.sql "%DATA_FILE%"
if errorlevel 1 (
    echo [ERROR] docker cp 실패
    exit /b 1
)
docker exec %CONTAINER% rm -f /tmp/backup_data.sql

echo.
echo === [3/4] %RETENTION_DAYS%일 이상 된 백업 정리 ===
powershell -NoProfile -Command "Get-ChildItem -LiteralPath '%BACKUP_DIR%' -Filter 'cheonil_*' -File | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-%RETENTION_DAYS%) } | ForEach-Object { Write-Host ('삭제: ' + $_.Name); Remove-Item -LiteralPath $_.FullName -Force }"

echo.
echo === [4/4] git 커밋 + push ===
rem 백업 폴더만 stage / commit — 작업 중인 다른 변경은 건드리지 않는다.
rem 실패해도 백업 파일은 이미 만들어졌으므로 경고만 하고 정상 종료.
if not "%BACKUP_GIT_PUSH%"=="1" (
    echo 건너뜀 ^(BACKUP_GIT_PUSH=%BACKUP_GIT_PUSH%^)
    goto :done
)
git -C "%BACKEND_DIR%" add -A -- "%BACKUP_DIR%"
if errorlevel 1 (
    echo [WARN] git add 실패 - 백업 폴더가 저장소 밖이면 커밋 불가
    goto :done
)
git -C "%BACKEND_DIR%" diff --cached --quiet -- "%BACKUP_DIR%"
if not errorlevel 1 (
    echo 변경 없음
    goto :done
)
git -C "%BACKEND_DIR%" commit -q -m "backup: db backup %TS%" -- "%BACKUP_DIR%"
if errorlevel 1 (
    echo [WARN] git commit 실패
    goto :done
)
git -C "%BACKEND_DIR%" push -q
if errorlevel 1 (
    echo [WARN] git push 실패 - 커밋은 로컬에 남아 있음, 나중에 git push
    goto :done
)
echo push 완료: backup: db backup %TS%

:done
echo.
echo === 백업 완료 ===
endlocal
exit /b 0
