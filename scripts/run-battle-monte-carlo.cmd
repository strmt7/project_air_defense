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

set ENGAGEMENT_RANGE=%~6
if "%ENGAGEMENT_RANGE%"=="" set ENGAGEMENT_RANGE=2825

set INTERCEPTOR_SPEED=%~7
if "%INTERCEPTOR_SPEED%"=="" set INTERCEPTOR_SPEED=700

set LAUNCH_COOLDOWN=%~8
if "%LAUNCH_COOLDOWN%"=="" set LAUNCH_COOLDOWN=0.3

set BLAST_RADIUS=%~9
if "%BLAST_RADIUS%"=="" set BLAST_RADIUS=82

set ROOT=%~dp0..
call "%ROOT%\gradlew.bat" :core:runBattleMonteCarlo -Pruns=%RUNS% -Pwaves=%WAVES% -Pseconds=%SECONDS% -Pstep=%STEP% -Pseed=%SEED% -PengagementRange=%ENGAGEMENT_RANGE% -PinterceptorSpeed=%INTERCEPTOR_SPEED% -PlaunchCooldown=%LAUNCH_COOLDOWN% -PblastRadius=%BLAST_RADIUS% --no-daemon --console=plain
