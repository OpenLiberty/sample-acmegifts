# This script is used to start a Mongo database with a specified name and port. 
#
# Example usage:
#
# startMongo.sh <databaseName> <databasePort> [mongoPath] 
#
# <databaseName> : The name of the directory to create and use to hold the Mongo DB
# <databasePort> : The port with which to start the Mongo DB server
# [mongoPath] : Optional, absolute path to the Mongo install bin directory
#

dbName=$1
dbPort=$2
mongoPath=$3

# Set mongo path if defined
if [ -z mongoPath ]; then
    export PATH=$PATH;${mongoPath}
fi

# Create the mongo db directory
if [ ! -d ./target/${dbName} ]; then
    mkdir ./target/${dbName}
fi
if [ ! -d ./target/${dbName}/logs ]; then
    mkdir ./target/${dbName}/logs
fi
if [ ! -d ./target/${dbName}/mongoDB ]; then
    mkdir ./target/${dbName}/mongoDB
fi

# Start the mongo db server
mongod --fork --logpath ./target/${dbName}/logs/${dbName}.log --port ${dbPort} --dbpath ./target/${dbName}/mongoDB | 
grep "forked process:" | 
awk '{split($0,a,":"); print a[2]}' > ./target/${dbName}/${dbName}.pid

echo "Starting user database with pid: "
cat ./target/${dbName}/${dbName}.pid
