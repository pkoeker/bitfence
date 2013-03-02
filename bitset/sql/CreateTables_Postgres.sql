-- DROP DATABASE bitset;

CREATE DATABASE bitset

-- DROP TABLE objekt;

CREATE TABLE objekt
(
  oid integer NOT NULL,
  content text,
  CONSTRAINT objekt_pkey PRIMARY KEY (oid)
)

-- DROP TABLE item;

CREATE TABLE item
(
  itemname character varying(128) NOT NULL,
  bits bytea NOT NULL,
  CONSTRAINT item_pkey PRIMARY KEY (itemname)
)