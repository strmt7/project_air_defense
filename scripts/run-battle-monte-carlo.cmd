@echo off
setlocal

set RUNS=%~1
if "%RUNS%"=="" set RUNS=300

set WAVES=%~2
if "%WAVES%"=="" set WAVES=1

set SECONDS=%~3
if "%SECONDS%"=="" set SECONDS=48

set STEP=%~4
if "%STEP%"=="" set STEP=0.05

set SEED=%~5
if "%SEED%"=="" set SEED=20260411

set ROOT=%~dp0..
call "%ROOT%\gradlew.bat" :core:runBattleMonteCarlo -Pruns=%RUNS% -Pwaves=%WAVES% -Pseconds=%SECONDS% -Pstep=%STEP% -Pseed=%SEED% --no-daemon --console=plain
