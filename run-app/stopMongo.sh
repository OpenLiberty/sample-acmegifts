# This script is used to stop a Mongo database
#
# Example usage:
#
# stopMongo.sh <databaseName> <buildDir> [mongoPath] 
#
# <databaseName> : The name of the directory where the Mongo DB is running
# <buildDir>     : Name of the build directory (Gradle = build, Maven = target)
# [mongoPath] : Optional, absolute path to the Mongo install bin directory
#

dbName=$1
buildDir=$2
mongoPath=$3

# Set mongo path if defined
if [ ! -z mongoPath ]; then
    export PATH=$PATH:${mongoPath}
fi

# Kill any existing mongo server
if [ -e ./${buildDir}/${dbName}/${dbName}.pid ]; then
	kill `cat ./${buildDir}/${dbName}/${dbName}.pid`
	rm ./${buildDir}/${dbName}/${dbName}.pid
fi
