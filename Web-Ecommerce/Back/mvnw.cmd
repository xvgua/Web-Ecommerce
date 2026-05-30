@echo off
REM Maven Wrapper for Windows (cmd.exe / PowerShell)

set MVNW_VER=3.2.0
set BASE_DIR=%~dp0
set MAVEN_USER_HOME=%USERPROFILE%\.m2

if defined JAVA_HOME (
  set "JAVACMD=%JAVA_HOME%\bin\java.exe"
) else (
  for %%i in (java.exe) do set JAVACMD=%%~$PATH:i
)

if not exist "%JAVACMD%" (
  echo ERROR: JAVA_HOME is not set and no 'java' command found in PATH.
  exit /b 1
)

set CLASSWORLDS_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
set MVNW_REPOURL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/%MVNW_VER%/maven-wrapper-%MVNW_VER%.jar
set WRAPPER_JAR=%BASE_DIR%\.mvn\wrapper\maven-wrapper.jar

if not exist "%WRAPPER_JAR%" (
  echo Downloading Maven Wrapper JAR...
  mkdir "%~dp0\.mvn\wrapper" 2>nul
  curl -fsSL -o "%WRAPPER_JAR%" "%MVNW_REPOURL%"
)

set "MAVEN_PROJECTBASEDIR=%BASE_DIR%..\"
"%JAVACMD%" -cp "%WRAPPER_JAR%" %CLASSWORLDS_LAUNCHER% %*
