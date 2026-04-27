@echo off
setlocal

set APP_HOME=%~dp0
set WRAPPER_PROPERTIES=%APP_HOME%gradle\wrapper\gradle-wrapper.properties

if not exist "%WRAPPER_PROPERTIES%" (
  echo Missing %WRAPPER_PROPERTIES%
  exit /b 1
)

for /f "tokens=1,* delims==" %%A in ('findstr /b "distributionUrl=" "%WRAPPER_PROPERTIES%"') do set DISTRIBUTION_URL=%%B
set DISTRIBUTION_URL=%DISTRIBUTION_URL:\:=:%

for /f "tokens=2 delims=-" %%A in ("%DISTRIBUTION_URL%") do set GRADLE_VERSION=%%A

if "%GRADLE_USER_HOME%"=="" (
  set GRADLE_USER_HOME_DIR=%USERPROFILE%\.gradle
) else (
  set GRADLE_USER_HOME_DIR=%GRADLE_USER_HOME%
)

set GRADLE_DIR=%GRADLE_USER_HOME_DIR%\wrapper\dists\gradle-%GRADLE_VERSION%-bin
set GRADLE_HOME=%GRADLE_DIR%\gradle-%GRADLE_VERSION%
set GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat
set ZIP_PATH=%GRADLE_DIR%\gradle-%GRADLE_VERSION%-bin.zip

if not exist "%GRADLE_BIN%" (
  if not exist "%GRADLE_DIR%" mkdir "%GRADLE_DIR%"

  if not exist "%ZIP_PATH%" (
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%ZIP_PATH%'"
    if errorlevel 1 exit /b 1
  )

  echo Unpacking Gradle %GRADLE_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP_PATH%' '%GRADLE_DIR%'"
  if errorlevel 1 exit /b 1
)

call "%GRADLE_BIN%" %*
