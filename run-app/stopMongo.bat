@REM This script is used to stop a Mongo database
@REM
@REM Example usage:
@REM
@REM stopMongo.sh <databaseName> [mongoPath] 
@REM
@REM <databaseName> : The name of the directory where the Mongo DB is running
@REM [mongoPath] : Optional, absolute path to the Mongo install bin directory

@echo off

@REM Get the needed params.
set dbName=%1

set mongoPath=%2

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
)

@REM Stop and remove any existing mongo db service.
mongod --logpath %CD%\target\%dbName%\logs\%dbName%_stop.log --remove --serviceName %dbName%