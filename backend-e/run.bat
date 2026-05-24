@echo off
REM Quick local run: skip tests and test compilation.
call "%~dp0mvnw.cmd" spring-boot:run -DskipTests -Dmaven.test.skip=true
