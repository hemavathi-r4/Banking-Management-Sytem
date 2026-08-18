@echo off
REM ============================================================
REM  Banking Management System — Stage 12, 13 & 14 Test Runner
REM  Compiles all source + test files, then runs JUnit 4 tests.
REM
REM  USAGE: Double-click this file OR run from BMS directory:
REM         compile_and_run_tests.bat
REM ============================================================

echo.
echo ==========================================
echo   BMS — Stage 12, 13 & 14 Test Runner
echo ==========================================
echo.

REM --- Classpath for all jars ---
SET CP=lib\mysql-connector-j-8.0.33.jar;lib\jbcrypt-0.4.jar;lib\junit-4.13.2.jar;lib\hamcrest-core-1.3.jar

echo [1/3] Compiling main source files...
javac -cp "%CP%" -d bin ^
    src\database\*.java ^
    src\model\*.java ^
    src\exception\*.java ^
    src\util\*.java ^
    src\dao\*.java ^
    src\service\*.java ^
    src\menu\*.java ^
    src\Main.java

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo [FAIL] Source compilation failed. Fix errors above before running tests.
    pause
    exit /b 1
)
echo        Source compilation SUCCESS.
echo.

echo [2/3] Compiling test files...
javac -cp "bin;%CP%" -d bin ^
    test\TestDBHelper.java ^
    test\PasswordUtilTest.java ^
    test\CustomerDAOTest.java ^
    test\AccountDAOTest.java ^
    test\TransactionDAOTest.java ^
    test\AuditLogServiceTest.java ^
    test\PaginationTest.java ^
    test\TransactionSearchTest.java ^
    test\AdminSearchTest.java

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo [FAIL] Test compilation failed. Fix errors above.
    pause
    exit /b 1
)
echo        Test compilation SUCCESS.
echo.

echo [3/3] Running JUnit Tests...
echo ------------------------------------------
java -cp "bin;%CP%" org.junit.runner.JUnitCore ^
    PasswordUtilTest ^
    CustomerDAOTest ^
    AccountDAOTest ^
    TransactionDAOTest ^
    AuditLogServiceTest ^
    PaginationTest ^
    TransactionSearchTest ^
    AdminSearchTest

echo.
echo ==========================================
echo   Test run complete.
echo ==========================================
pause
