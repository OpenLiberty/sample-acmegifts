# This script is used to stop a Mongo database
#
# Example usage:
#
# stopMongo.sh <databaseName> [mongoPath] 
#
# <databaseName> : The name of the directory where the Mongo DB is running
# [mongoPath] : Optional, absolute path to the Mongo install bin directory
#

dbName=$1
mongoPath=$2

# Set mongo path if defined
if [ -z mongoPath ]; then
    export PATH=$PATH;${mongoPath}
fi

# Kill any existing mongo server
if [ -e ./target/${dbName}.pid ]; then

	kill `cat ./target/${dbName}.pid`
	rm ./target/${dbName}.pid
fi
