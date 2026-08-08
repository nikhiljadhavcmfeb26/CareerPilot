@echo off
REM Initialises careerpilot-config-repo as a git repository.
REM
REM Required because the Spring Cloud Config Server reads COMMITTED git content,
REM and .git\ is excluded from the distributed archive. Without this, config-server
REM fails to start and every other service fails with it.
REM
REM Safe to re-run: on an already-initialised repo it just commits pending changes.
setlocal
cd /d "%~dp0careerpilot-config-repo"

REM Fall back to a local identity if git has no global user configured.
set GITID=
git config user.email >nul 2>&1 || git config --global user.email >nul 2>&1
if errorlevel 1 (
    echo No git identity configured - using a local one for this repository only.
    set GITID=-c user.name=CareerPilot -c user.email=config@careerpilot.local
)

if exist .git (
    echo Config repo is already a git repository.
    git add -A
    git diff --cached --quiet && (echo Nothing to commit - working tree is clean.) || git %GITID% commit -m "Update CareerPilot configuration"
) else (
    git init -b main >nul 2>&1 || ( git init >nul && git checkout -b main >nul 2>&1 )
    git add .
    git %GITID% commit -m "Initial CareerPilot configuration" >nul
    echo Initialised config repo on branch 'main'.
)

echo.
git log --oneline
echo.
echo Done. You can now start config-server.
endlocal
