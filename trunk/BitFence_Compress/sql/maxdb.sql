

create table ITEM (
    ITEMNAME VARCHAR(128) not null,
    primary key (ITEMNAME)
)

//
create table OBJEKT
(
    OID                             INTEGER                not null,
    content			    VARCHAR(4000),
    primary key (OID)
)

//
create table OBJEKTITEM
(
    OID                             INTEGER                not null,
    ITEMNAME                        VARCHAR(128)            not null,
    primary key (OID, ITEMNAME),
    foreign key (OID)
       references OBJEKT (OID) ON DELETE CASCADE,	
    foreign key (ITEMNAME)
       references ITEM (ITEMNAME) ON DELETE CASCADE
)

//
create table SLOT
(
    ITEMNAME                        VARCHAR(128)            not null,
    BITS                            BLOB                   not null,
    BITCOUNT                        INTEGER                        ,
    primary key (ITEMNAME),
    foreign key (ITEMNAME)
       references ITEM (ITEMNAME)
)


