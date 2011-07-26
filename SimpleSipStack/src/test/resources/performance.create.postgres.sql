-- the user role to connect to the database
CREATE ROLE performanceuser LOGIN
  PASSWORD 'performanceuser'
  NOSUPERUSER NOINHERIT NOCREATEDB NOCREATEROLE;

-- the database for performance metrics
CREATE DATABASE performancemetrics
  WITH OWNER = performanceuser
       ENCODING = 'UTF-8'
       TABLESPACE = pg_default;
       
-- create performance metrics tables
CREATE TABLE performance
(
  entrydate bigint
  callspersecond double precision,
  variance double precision,
  standarddeviation double precision,
) 
WITHOUT OIDS;
ALTER TABLE performance OWNER TO performanceuser;