@echo off
rem Run the UI Components Tester

echo Building project...
call mvn clean install -DskipTests

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b %ERRORLEVEL%
)

echo Running UI Component Tester...
call mvn exec:java -Dexec.mainClass="com.angrysurfer.beats.test.UIComponentTester" -Dexec.classpathScope=test

echo Done!
