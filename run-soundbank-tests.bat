@echo off
rem Run the Soundbank Tests

echo Building project...
call mvn clean install -DskipTests

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b %ERRORLEVEL%
)

echo Running Soundbank Tests...
call mvn test -Dtest=SoundbankTest

echo Done!
