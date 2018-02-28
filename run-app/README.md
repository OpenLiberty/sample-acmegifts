# Acme Gifts Quick Start

> This project allows users to get Acme Gifts up and running in no time.

## Maven profiles

1. **start-servers:** Starts all liberty servers
1. **start-databases:** Starts all mongoDBs
1. **stop-servers:** Stops all liberty servers
1. **stop-databases:** Stops all mongoDBs
1. **demo:** Preloads Acme Gifts with pertinent data. Use it only after all databases and servers have been started.
   
### Example Usage:

From the sample-acmegifts/run-app project:
```
mvn package -P start-databases  
mvn package -P start-servers  
mvn package -P demo  
mvn package -P start-databases,start-servers,demo  
mvn package -P stop-servers,stop-databases  
```

From the sample-acmegifts root project:
```
mvn package -P start-databases -pl run-app  
mvn package -P start-servers -pl run-app  
mvn package -P demo -pl run-app  
mvn package -P start-databases,start-servers,demo -pl run-app  
mvn package -P stop-servers,stop-databases -pl run-app  
```

## Gradle tasks

1. **startServers:** Starts all liberty servers
1. **startDatabases:** Starts all mongoDBs
1. **start**: Starts all liberty servers and databases
1. **stopServers:** Stops all liberty servers
1. **stopDatabases:** Stops all mongoDBs
1. **stop**: Stops all liberty servers and databases
1. **demo:** Preloads Acme Gifts with pertinent data. Calling this directly will automatically start all databases and servers.

These are tasks that are specifically owned by `run-app`.
`startServers` wraps all `libertyStart` tasks from the other projects and each `libertyStart` has a dependency on their related `startDatabase`. 
Stopping the servers and their databases have the same structure.  

For your convenience, `start` and `demo` will automatically start all necessary preparations and `stop` will shut them down.


### Example Usage:
From the sample-acmegifts/run-app project:
```
gradle startServers startDatabases
gradle startServers
gradle start
gradle stopServers stopDatabases
gradle stopServers
gradle stop
gradle demo
```

From the sample-acmegifts root project, you can call all run-app tasks above in addition to:
```
gradle libertyStart
gradle libertyStop
```

These commands work from the root project because each project has an instance of a `libertyStart` or `libertyStop` task that Gradle will recognize and execute.  
Additionally, the tasks called in `sample-acmegifts/run-app` could also be called from the root project dir and Gradle will understand to execute `run-app`'s tasks.

        
## Bootstrap Data

> This project allows an initial set of data to be bootstrapped into Acme Gifts. The data is contained in
3 different JSON files located here: `run-app/src/resources`. The content of those files can be customized. The entries must follow
the formats shown below: 

#### users.json 

Json file containing an array of user objects. 
    
    Example:
    [{
        "id": "user1",
        "firstName": "John",
        "lastName": "Smith",
        "userName": "jsmith",
        "twitterHandle": "@jsmith",
        "wishListLink": "http://my.wishlist.com/jsmith",
        "password": "jsmith"
    },...]


**Where:**

1. **Id:** A unique user id. It must be unique among all users. It is replaced with another unique (valid) id during boot data processing.
1. **firstname:** The first name of the user. It is an arbitrary name.
1. **lastname:** The last name of the user. It is an arbitrary name. It is used to login to Acme Gifts.
1. **username:** A unique username. It is an arbitrary name that must be unique among all usernames.
1. **twitterHandle:** The user's twitter handle. A valid twitter handle if microservice_notification_v1_1  is used, or an arbitraty name if not.
1. **wishListLink:** The link to the user's wishlist. It can be an arbitrary link.
1. **password:** The user's password. It can be an arbitrary name. It is used to login to Acme Gifts.


#### groups.json 

Json file containing an array of group objects. 
    
    For example:
    
    [{
        "id": "group1",
        "name": "Co-workers",
        "members": ["user1","user2","user3"]
    },...]
 
**Where:**
    
1. **Id:** A unique group id. It must be unique among all group ids. It is replaced with another unique (valid) id during boot data processing.
1. **name:** The name of the group. It is an arbitrary name.
1. **members:** An array of user ids belonging to the group:
    1. **userId:** The id of the user in the group. The user id is chosen from one of the users created under users.json. The value is replaced during boot data processing.


#### occasions.json 

Json file containing an array of occasion objects. 

    For example:
    
    [{
        "id": "occasion1",
        "date": "year-month-day",
        "groupId": "group1",
        "name": "John's Birthday",
        "organizerId": "user2",
        "recipientId": "user1",
        "contributions" : [
                      {
                        "userId": "user2",
                        "amount": 50
                      },
                      {
                        "userId": "user3",
                        "amount": 100
                      }
                    ]
    },...]


**Where:**
    
1. **Id:** A unique occasion id. It must be unique among all occasions. It is replaced with another unique (valid) id during boot data processing.
1. **date:** Represents when the occasion is to take place. The value 'year-month-day' is replaced
        during boot procession with a date randomly selected between 90 to 200 days from the current date.
    A specific date of the form yyyy-MM-dd could also be specified and will be interpreted literally.   
1. **groupId:** The id of the group associated with occasion. The group id is chosen from one of the groups created under groups.json. The value is replaced during boot data processing.
1. **name:** The name of the occasion. It is an arbitrary name.
1. **organizerId:** The id of the user organizing the occasion. The user id is chosen from one of the users created under users.json. The value is replaced during boot data processing.
1. **recipientId:** The id of the user for which the occasion was created. The user id is chosen from one of the users created under groups.json. The value is replaced during boot data processing.
1. **Contributions:** An array of user contributions:
    1. **userId:** The id of the user contributing to the occasion. The user id is chosen from one of the users created under users.json. The value is replaced during boot data processing.
    1. **amount:** The amount being contributed by the associated user. It is an arbitrary number that is a whole number or a whole number with up to 2 decimal places. For example: 15, 15.01, or 15.99.


### Sample data details
    
The sample boot data files define 5 users, 2 groups, and 4 occasion as follows:

    Users: 
        1. John Smith 
        2. Fred Murphy 
        3. Stacey White 
        4. Linda Williams 
        5. Allen Grants      

    Groups: 
        1. Friends 
           - John Smith 
           - Fred Murphy 
           - Stacey White 

        2. Co-workers 
           - John Smith 
           - Linda Williams 
           - Allen Grants 

    Occasions:
        1. John's Birthday: 
           - Group: Friends 
           - Organizer: Fred Murphy 
           - Recipient: John Smith 
           - Contributors: Fred M. and Stacey W. 
    
        2. John's Birthday: 
           - Group: Co-workers 
           - Organizer: Linda Williams 
           - Recipient: John Smith 
           - Contributors: Linda W. and Allen G. 
           
        3. Stacy's Retirement Party: 
           - Group: Friends 
           - Organizer: John Smith 
           - Recipient:Stacey White 
           - Contributors: John S. and Fred M. 
           
        4. Linda's Birthday: 
           - Group: Co-workers 
           - Organizer: John Smith 
           - Recipient: Linda Williams 
           - Contributors: John S. and Allen G.
