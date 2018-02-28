@REM This script is used to stop a Mongo database
@REM
@REM Example usage:
@REM
@REM stopMongo.sh <databaseName> <buildDir> [mongoPath] 
@REM
@REM <databaseName> : The name of the directory where the Mongo DB is running
@REM <buildDir>     : Name of the build directory (Gradle = build, Maven = target)
@REM [mongoPath] : Optional, absolute path to the Mongo install bin directory

@echo off

@REM Get the needed params.
set dbName=%1
set buildDir=%2
set mongoPath=%3

@REM Set the mongoDB path.
IF defined mongoPath SET PATH=%PATH%;%mongoPath%

@REM Create the needed directories.
if defined dbName (
  IF NOT exist .\%buildDir%\%dbName% (
      md .\%buildDir%\%dbName%
  )
  IF NOT exist .\%buildDir%\%dbName%\logs (
      md .\%buildDir%\%dbName%\logs
  )
)

@REM Stop and remove any existing mongo db service.
mongod --logpath %CD%\%buildDir%\%dbName%\logs\%dbName%_stop.log --remove --serviceName %dbName%
