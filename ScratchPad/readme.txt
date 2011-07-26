1 - starts two hsqldb instances on etc/scripts
    - start each script in two separate cmd windows like
      dir> startHsqldb.bat 0 127.0.0.1 12000
      dir> startHsqldb.bat 1 127.0.0.1 12001
      first param is the database id, second the ip where the db is listening and third the port

2 - make sure that beans in application context hsqldb0.datasource hsqldb1.datasource do match the hsqldb start params

3 - attempts to have a start/stop of an instance of the db are in progress - comment out the line that calls doDbRestart and doDbStart in the main
