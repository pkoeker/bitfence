
# ============================================================
#   Table : ITEM                                              
# ============================================================
create table ITEM (
    ITEMNAME VARCHAR(50) not null,
    primary key (ITEMNAME)
); 

# ============================================================
#   Table : OBJECT                                            
# ============================================================
create table OBJEKT
(
    OID                             INTEGER                not null,
    content			    varchar(255),
    primary key (OID)
); 

create table OBJEKTITEM
(
    OID                             INTEGER                not null,
    ITEMNAME                        VARCHAR(50)            not null,
    primary key (OID, ITEMNAME),
    foreign key (OID)
       references OBJEKT (OID) ON DELETE CASCADE,
	INDEX FK_Itemname (Itemname),
    foreign key (ITEMNAME)
       references ITEM (ITEMNAME) ON DELETE CASCADE
);

# ============================================================
#   Table : SLOT                                              
# ============================================================
create table SLOT
(
    ITEMNAME                        VARCHAR(50)            not null,
    SLOTNUMBER                      INTEGER                not null,
    BITS                            BLOB                   not null,
    BITCOUNT                        INTEGER                        ,
    primary key (ITEMNAME, SLOTNUMBER),
    foreign key (ITEMNAME)
       references ITEM (ITEMNAME)
);

