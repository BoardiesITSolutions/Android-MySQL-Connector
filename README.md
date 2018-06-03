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