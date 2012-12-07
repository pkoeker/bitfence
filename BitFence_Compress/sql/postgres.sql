create table ITEM (
    ITEMNAME VARCHAR(128) not null,
    primary key (ITEMNAME)
); 

create table OBJECT
(
    OID                             INTEGER                not null,
    content			    			TEXT,    
    primary key (OID)
); 

create table OBJECTITEM
(
    OID                             INTEGER                not null,
    ITEMNAME                        VARCHAR(128)            not null,
    primary key (OID, ITEMNAME),
    foreign key (OID)
       references OBJECT (OID) ON DELETE CASCADE,
    foreign key (ITEMNAME)
       references ITEM (ITEMNAME) ON DELETE CASCADE
);

create table SLOT
(
    ITEMNAME                        VARCHAR(128)            not null,
    BITS                            bytea                   not null,
    BITCOUNT                        INTEGER                        ,
    primary key (ITEMNAME),
    foreign key (ITEMNAME)
       references ITEM (ITEMNAME)
);


