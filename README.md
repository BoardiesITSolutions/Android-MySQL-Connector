# Introduction
This is (as far as we are aware) the first and only native MySQL connector for Android.
It connects directly to your database instead of relying on some sort of web service to
act as a middleware between Android and the MySQL Server.

Using a webservice is still the recommended way of connecting to your database as it
ensures only clients that are supposed to are connecting to your database and avoids the requirement
of needing to expose your MySQL server to the Internet.

However, if you want, or have a need to connect directly to your database, then this is the library
you need. This is a fairly basic MySQL connector compared to the official connectors that are available
on other platforms. Therefore, below is a list of the known limitations of the library:
<ul>
    <li>Doesn't support compression, if the MySQL server reports that it supports compression, the connector will turn it off</li>
    <li>Doesn't support prepared statements</li>
    <li>Only supports UTF8 or Latin 1 encodings</li>
    <li>If authentication is required, then only mysql_native_password is supported</li>
</ul>

The library has been tested on the following MySQL Servers
<ul>
    <li>5.1.72</li>
    <li>5.5.59</li>
    <li>5.6.39</li>
    <li>5.7.22</li>
</ul>

We've so far only tested on the the highest minor version of each MySQL major version. You'll notice
that this doesn't include MySQL 8.0. This is due to MySQL 8 changing the default authentication mechanism,
this is something we plan on adding in the future. We haven't yet tested with MariaDB however, the equivalent
MeriaDB and MySQL version should be compatible and therefore should work in the same way.

# Adding Dependency
Adding the library to your project couldn't be simpler. Add the following to your apps build.gradle
'''
    repositories {
        maven { url 'https://jitpack.io' }
    }

    dependencies {
        implementation 'com.github.BoardiesITSolutions:Android-MySQL-Connector:TAG'
    }
'''

The `TAG` above will be the tagged version number of the library.

# Using the Library
Due to the way Android works, using the library is a little different compared to using the official
connector on other platforms. This is due to to Android completely restricting any network activity on the main
thread. Obviously you shouldn't do any network activity on the main thread on any platform, but most platforms don't stop
you if you want to, however, Android will throw an exception if any network activity is done on the main thread. Therefore
for each action you wish to take, such as connecting, switching databases, performing a query, the relevant method
or constructor will take an interface which will get called once the action has completed which you can then handle.

## Connecting to a MySQL Server
To connect to a MySQL server you first need to create the MySQL Connection object. You can do this as follows:
'''
Connection  mysqlConnection = new Connection("&lt;Hostname&gt;", "&lt;Username&gt;", "&lt;Password&gt;", &lt;Port&gt;, "&lt;Database&gt;", new IConnectionInterface()
'''

In the above example, &lt;database&gt; is an optional parameter. By setting this, when the connection is established, the database name will be the default
database used. The IConnectionInterface parameter handles connection specific events, such as successfully connected or exception handlers.

For example, when creating a new IConnectionInterface, you will need to provide the following methods:
**actionCompleted**
This means that the connection was successfully established and authenticated and the connection is ready for use

**handleInvalidSQLException**
You shouldn't really ever get this, but you might if you are connecting to a MySQL server that either is configured slightly differently
as to what has bee tested or isn't compatible with the connector, and has caused the connector to receive a network packet from the MySQL
server that the connector wasn't expecting.

**handleMySQLException**
This will happen if a generic MySQL exception occurs within the connector

**handleIOException**
This is used if an internal socket error occurs within the connector, for example, if server aborted the connection but the library
didn't realise and tried to perform an operation on the MySQL socket which is now closed

**handleMySQLConnException**
This will be an exception related to a connection error, such as authentication failure

**handleException**
This is a generic exception handler if any of the above doesn't match the exception

That's it, you have successfully connected, now we can execute some queries. Remember though, keep
your connection object available in your class as this will be required in order to run queries on the DB.

## Switching Database
You can change the default database to use in your established MySQL Connection. To do this, do not execute
a standard USE DATABASE &lt;db&gt; query as it won't work.

You need to use your connection object and call the method `switchDatabase(db, new IConnectionInterface())`.
`db` being a String of your database name, and again, pass in the IConnectionInterface. If it switched successfully
you will receive a call back to the actionCompleted method.

## Execute Statement (Such as INSERT or UPDATE where no resultset is returned)
First of all you need to create a statement object from your connection object.
You can do this using the following code snippet
'''
Statement statement = connection.createStatement();
'''

Then in order to execute your statement you then do the following
'''
statement.execute(query, new IConnectionInterface());
'''

`query` is your statement that you want to execute such as an INSERT or UPDATE statement. Again the second
parameter is the IConnectionInterface and if the statement execute successfully, then actionCompleted will be called.

## Execute Query (Such as SELECT)
The execute query function allows you to execute a query such as SELECT or DESCRIBE, basically any query
which return a result set. Same with executing a statement, you need to create the statement object from your
connection object. This can be done using the following code snippet:
'''
Statement statement = connection.createStatement();
'''

Then you need to call the executeQuery withn your statement object, but this time passing in a new
IResultInterface as shown below:
'''
statement.executeQuery(query, new IResultInterface());
'''

The IResultInterface is pretty similar to the IConnectionInterface, you'll have the same exception handlers,
the main difference that if the query executes successfully, you'll receive executionComplete which will have
a parameter to the result set.

# Processing a result set
When you execute a query such as SELECT or DESCRIBE, you will get a call back which will provide a result set object.
This result set object contains all the information about what was returned such as the columns and the rows.

## Get total number of rows returned
To get the total number of rows you can call `resultset.getNumRows();`.

## Get column definitions
The column definitions are stored in a `List&lt;ColumnDefinition&gt;`. You can get this using the following code snipper:

'''
List&lt;ColumnDefinition&gt; columns = result.getFields();
'''

You can then loop over the list to get ColumnDefinition for each column returned in the result set. Within the column
definition class, you can use the following methods:
**getDatabase**
The database name where the column was retrieved from

**getTable**
The table name where the column was retrieved from

**getColumnName**
The name of the column

**getColumnType**
Returns an enum of type `ColumnType`

**isPrimaryKey**
Returns a boolean as to whether or not the column is a primary key

## Iterating through each row
You can iterate through each row, you first need to create an empty `MySQLRow` variable that can be used
to be set within a while loop to get each row. You can call `getNextRow()` on the result set to get a MySQLRow.
Once there are no rows left, null is returned. The following code snippet provides an example:

'''
MySQLRow row;
while ((row = result.getNextRow()) != null)
{
   //Read the row contents here
}
'''

Once you have your MySQLRow you can then call the following methods to return the value of the field.
Each of the following methods, take a String parameter which is the column name that should be retrieved.
* getString(String column)
* getInt(String column)
* getFloat(String column)
* getDouble(String column)

# Escaping Strings
When sending dynamic paraters in your MySQL query, the string should be escaped to avoid SQL injection attacks.
This can be done by using the following code snippet:

'''
String var = connection.escape_string(variable);
'''

# Best Practices
Currently the connection object can't be passed between Android activities, so if you do need to create a new Activity
and perform database action, you should close the DB connection in your current activity, pass the connection details
to your new activity, and then create a new connection object in your new activity.

Also, to avoid leaving the DB connection open for no reason, in your activities onPause and onDestroy methods
you should close the DB connection and then create a new instance to restablish the connection in the onCreate and/or onResume

You can close the DB by calling `connection.close()`.