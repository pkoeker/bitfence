CREATE ROLE bitset LOGIN
  PASSWORD 'bitset'
  SUPERUSER INHERIT CREATEDB CREATEROLE NOREPLICATION;
  
-- DROP DATABASE bitsetTest;

CREATE DATABASE bitsetTest
  WITH OWNER = bitset
       ENCODING = 'UTF8'
       TABLESPACE = pg_default
       LC_COLLATE = 'de_DE.UTF-8'
       LC_CTYPE = 'de_DE.UTF-8'
       CONNECTION LIMIT = -1;

-- DROP TABLE item;

CREATE TABLE item
(
  itemname character varying(128) NOT NULL,
  bits bytea NOT NULL,
  CONSTRAINT item_pkey PRIMARY KEY (itemname)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE item
  OWNER TO bitset;
  
-- DROP TABLE objekt;

CREATE TABLE objekt
(
  oid integer NOT NULL,
  content text,
  CONSTRAINT objekt_pkey PRIMARY KEY (oid)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE objekt
  OWNER TO bitset;