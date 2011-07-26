CREATE TABLE performance
(
  id int8 NOT NULL,
  runid int8,
  name varchar,
  unitpersecond float8,
  averageduration float8,
  numberofruns int8,
  numberofsuccessfulruns int8,
  variance float8,
  standarddeviation float8,
  success bool,
  whencreated timestamp,
  description varchar(4096),
  threadinfo varchar(256),
  testtype varchar(256),
  CONSTRAINT pk_performance PRIMARY KEY (id)
)
WITHOUT OIDS;
ALTER TABLE Performance OWNER TO %%USERNAME%%;

-- DROP SEQUENCE performance_sequence;

CREATE SEQUENCE performance_sequence
  INCREMENT 1
  MINVALUE 0
  MAXVALUE 9223372036854775807
  START 0
  CACHE 1;
ALTER TABLE performance_sequence OWNER TO %%USERNAME%%;
GRANT ALL ON TABLE performance_sequence TO %%USERNAME%%;

