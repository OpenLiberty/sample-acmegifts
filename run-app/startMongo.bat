@REM This script is used to start a Mongo database with a specified name and port. 
@REM
@REM Example usage:
@REM
@REM startMongo.sh <databaseName> <databasePort> [mongoPath] 
@REM
@REM <databaseName> : The name of the directory to create and use to hold the Mongo DB
@REM <databasePort> : The port with which to start the Mongo DB server
@REM [mongoPath] : Optional absolute path to the Mongo install bin directory

@echo off

@REM Get the needed params.
set dbName=%1

set dbPort=%2
set mongoPath=%3

@REM Set the mongoDB path.
IF defined mongoPath SET PATH=%PATH%;%mongoPath%

@REM Create the needed directories.
if defined dbName (
  IF NOT exist .\target\%dbName% (
      md .\target\%dbName%
  )
  IF NOT exist .\target\%dbName%\logs (
      md .\target\%dbName%\logs
  )
  IF NOT exist .\target\%dbName%\mongoDB (
      md .\target\%dbName%\mongoDB
  )
)
@REM Install a mongoDB service and start it. 

mongod --logpath %CD%\target\%dbName%\logs\%dbName%_start.log --port %dbPort% --dbpath %CD%\target\%dbName%\mongoDB --install --serviceName %dbName% --serviceDisplayName %dbName%

net start %dbName%