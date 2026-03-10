@echo off
set JLINK_VM_OPTIONS=--enable-native-access=javafx.graphics
set DIR=%~dp0
"%DIR%\java" %JLINK_VM_OPTIONS% -m com.example.application/com.example.application.LibraryApp %*
