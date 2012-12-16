create table OBJEKT
(
    OID                             INTEGER                not null,
    content			    			TEXT,    
    primary key (OID)
)


create table ITEM
(
    ITEMNAME                        VARCHAR(128)            not null,
    BITS                            bytea                   not null,
    primary key (ITEMNAME)
)



