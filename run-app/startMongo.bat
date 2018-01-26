@REM This script is used to start a Mongo database with a specified name and port. 
@REM
@REM Example usage:
@REM
@REM startMongo.sh <databaseName> <databasePort> <buildDir> [mongoPath] 
@REM
@REM <databaseName> : The name of the directory to create and use to hold the Mongo DB
@REM <databasePort> : The port with which to start the Mongo DB server
@REM <buildDir>     : Name of the build directory (Gradle = build, Maven = target)
@REM [mongoPath] : Optional absolute path to the Mongo install bin directory

@echo off

@REM Get the needed params.
set dbName=%1
set dbPort=%2

set buildDir=%3
set mongoPath=%4

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
  IF NOT exist .\%buildDir%\%dbName%\mongoDB (
      md .\%buildDir%\%dbName%\mongoDB
  )
)
@REM Install a mongoDB service and start it. 

mongod --logpath %CD%\%buildDir%\%dbName%\logs\%dbName%_start.log --port %dbPort% --dbpath %CD%\%buildDir%\%dbName%\mongoDB --install --serviceName %dbName% --serviceDisplayName %dbName%

net start %dbName%
