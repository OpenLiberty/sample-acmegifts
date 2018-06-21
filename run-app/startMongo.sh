# This script is used to start a Mongo database with a specified name and port. 
#
# Example usage:
#
# startMongo.sh <databaseName> <databasePort> [mongoPath] 
#
# <databaseName> : The name of the directory to create and use to hold the Mongo DB
# <databasePort> : The port with which to start the Mongo DB server
# <buildDir>     : Name of the build directory (Gradle = build, Maven = target)
# [mongoPath] : Optional, absolute path to the Mongo install bin directory
#

dbName=$1
dbPort=$2
buildDir=$3
mongoPath=$4

if [ ! $# -ge 3 ]; then
    echo 'Please provide at least a dbName, dbPort, and buildDir'
    echo 'Example usage:'
    echo ''
    echo '    startMongo.sh <databaseName> <databasePort> <buildDir> [mongoPath] '
fi

# Set mongo path if defined
if [ ! -z mongoPath ]; then
    export PATH=$PATH:${mongoPath}
fi

# Create the mongo db directory
if [ ! -d ./${buildDir}/${dbName} ]; then
    mkdir ./${buildDir}/${dbName}
fi
if [ ! -d ./${buildDir}/${dbName}/logs ]; then
    mkdir ./${buildDir}/${dbName}/logs
fi
if [ ! -d ./${buildDir}/${dbName}/mongoDB ]; then
    mkdir ./${buildDir}/${dbName}/mongoDB
fi

# Start the mongo db server
mongod --fork --logpath ./${buildDir}/${dbName}/logs/${dbName}.log --port ${dbPort} --dbpath ./${buildDir}/${dbName}/mongoDB | 
grep "forked process:" | 
awk '{split($0,a,":"); print a[2]}' > ./${buildDir}/${dbName}/${dbName}.pid

echo "Starting user database with pid: "
cat ./${buildDir}/${dbName}/${dbName}.pid
