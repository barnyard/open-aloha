@echo off
rem  +----------------+-------------+----------+------------------------------+
rem  |    OPTION      |    TYPE     | DEFAULT  |         DESCRIPTION          |
rem  +----------------+-------------+----------+------------------------------|
rem  | -?             | --          | --       | prints this message          |
rem  | -address       | name|number | any      | server inet address          |
rem  | -port          | number      | 9001/544 | port at which server listens |
rem  | -database.i    | [type]spec  | 0=test   | path of database i           |
rem  | -dbname.i      | alias       | --       | url alias for database i     |
rem  | -silent        | true|false  | true     | false => display all queries |
rem  | -trace         | true|false  | false    | display JDBC trace messages  |
rem  | -tls           | true|false  | false    | TLS/SSL (secure) sockets     |
rem  | -no_system_exit| true|false  | false    | do not issue System.exit()   |
rem  | -remote_open   | true|false  | false    | can open databases remotely  |
rem  +----------------+-------------+----------+------------------------------+

title HSQLDB:xdb%1

set JAVA=java
set HSQLDB_CP=-cp ../../lib/hsqldb/hsqldb.jar
set HSQLDB_ADDRESS=-address %2
set HSQLDB_PORT=-port %3
set HSQLDB_SERVER_CLASS=org.hsqldb.Server
set DATABASE=-database.%1 file:c:\Temp\xdb%1
rem set DATABASE=-database.%1 mem:xdb%1
set DATABASE_NAME=-dbname.%1 xdb%1
set HSQLDB_FLAGS=-silent false -trace true

%JAVA% %HSQLDB_CP% %HSQLDB_SERVER_CLASS% %HSQLDB_ADDRESS% %HSQLDB_PORT% %DATABASE% %DATABASE_NAME% %HSQLDB_FLAGS%