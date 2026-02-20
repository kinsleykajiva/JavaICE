@echo off
echo Starting copy...
copy ..\libnice\build-x-win64\nice\libnice-10.dll src\main\resources\natives\windows-x64\libnice-10.dll
copy ..\libnice\build-x-linux\nice\libnice.so.10 src\main\resources\natives\linux-x64\libnice.so.10
echo Copy done.
dir src\main\resources\natives\windows-x64
echo Running maven package...
mvn package -DskipTests
echo Maven done.
