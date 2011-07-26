-- login as postgres user (administrator) and run the following

-- the user role to connect to the database
CREATE ROLE springringuser LOGIN
  PASSWORD 'springringuser'
  NOSUPERUSER NOINHERIT NOCREATEDB NOCREATEROLE;


-- the database owned by the user role previously created
CREATE DATABASE springring
  WITH OWNER = springringuser
       ENCODING = 'UTF-8'
       TABLESPACE = pg_default;
