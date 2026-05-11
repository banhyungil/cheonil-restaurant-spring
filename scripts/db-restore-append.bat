@echo off
chcp 65001 > nul

rem ============================================================
rem  Cheonil DB Restore - Append mode
rem
rem  특징:
rem    - 기존 데이터를 지우지 않음
rem    - 백업의 INSERT 문에 'ON CONFLICT DO NOTHING' 을 자동 추가
rem      → PK / unique 충돌 시 해당 row 만 skip
rem    - 복원 후 모든 시퀀스를 MAX 값으로 재정렬
rem
rem  사용:
rem    db-restore-append.bat <백업파일_data.sql>
rem
rem  주의:
rem    - 입력은 db-backup.bat 이 생성한 *_data.sql (plain, column-inserts)
rem    - .dump (custom format) 는 db-restore-full.bat 사용
rem ============================================================

setlocal

if "%~1"=="" (
    echo 사용법: %~nx0 ^<백업파일_data.sql^>
    exit /b 1
)

set "BACKUP_FILE=%~1"
if not exist "%BACKUP_FILE%" (
    echo [ERROR] 파일 없음: %BACKUP_FILE%
    exit /b 1
)

set "SCRIPT_DIR=%~dp0"
set "RESET_SQL=%SCRIPT_DIR%sql\reset-sequences.sql"
if not exist "%RESET_SQL%" (
    echo [ERROR] reset-sequences.sql 없음: %RESET_SQL%
    exit /b 1
)

set "CONTAINER=cheonil-db"
set "DB=cheonil"
set "DB_USER=root"

set "TMP_FILE=%TEMP%\cheonil_restore_%RANDOM%.sql"

echo.
echo === [1/4] INSERT 문에 ON CONFLICT DO NOTHING 추가 ===
echo source: %BACKUP_FILE%
echo temp:   %TMP_FILE%
powershell -NoProfile -Command "(Get-Content -LiteralPath '%BACKUP_FILE%') -replace '^(INSERT INTO .*\));$', '$1 ON CONFLICT DO NOTHING;' | Set-Content -LiteralPath '%TMP_FILE%' -Encoding UTF8"
if errorlevel 1 (
    echo [ERROR] 전처리 실패
    exit /b 1
)

echo.
echo === [2/4] 컨테이너로 SQL 복사 ===
docker cp "%TMP_FILE%" %CONTAINER%:/tmp/restore.sql
if errorlevel 1 (
    echo [ERROR] docker cp 실패
    del "%TMP_FILE%" 2>nul
    exit /b 1
)
del "%TMP_FILE%" 2>nul

echo.
echo === [3/4] 데이터 복원 (append) ===
docker exec %CONTAINER% psql -U %DB_USER% -d %DB% -v ON_ERROR_STOP=1 -f /tmp/restore.sql
if errorlevel 1 (
    echo [ERROR] 복원 실패 (위 로그 확인)
    docker exec %CONTAINER% rm -f /tmp/restore.sql
    exit /b 1
)
docker exec %CONTAINER% rm -f /tmp/restore.sql

echo.
echo === [4/4] 시퀀스 재정렬 ===
docker cp "%RESET_SQL%" %CONTAINER%:/tmp/reset-sequences.sql
if errorlevel 1 (
    echo [ERROR] reset-sequences.sql 복사 실패
    exit /b 1
)
docker exec %CONTAINER% psql -U %DB_USER% -d %DB% -f /tmp/reset-sequences.sql
if errorlevel 1 (
    echo [ERROR] 시퀀스 재정렬 실패
    docker exec %CONTAINER% rm -f /tmp/reset-sequences.sql
    exit /b 1
)
docker exec %CONTAINER% rm -f /tmp/reset-sequences.sql

echo.
echo === Append 복원 완료 ===
endlocal
exit /b 0
