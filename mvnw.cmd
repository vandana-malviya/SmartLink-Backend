@echo off
setlocal

if "%JAVA_HOME%"=="" (
    if exist "D:\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=D:\Android Studio\jbr"
        set "PATH=D:\Android Studio\jbr\bin;%PATH%"
    )
)

set "MAVEN_CMD=C:\Users\vanda\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd"
if exist "%MAVEN_CMD%" (
    call "%MAVEN_CMD%" %*
) else (
    mvn %*
)
