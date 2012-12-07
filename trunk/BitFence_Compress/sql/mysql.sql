# ============================================================
#   Database name  :  SCHLAGWORT                              
#   DBMS name      :  MySQL                                   
#   Created on     :  24.12.2002  14:04                       
# ============================================================

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
create table OBJECT
(
    OID                             INTEGER                not null,
    primary key (OID)
); 

# ============================================================
#   Table : OBJECTITEM                                        
# Der INDEX ist wichtig:
# Alle Foreign Keys, die nicht das erste Feld in einem Key (hier der Primary Key) sind,
# benötigen einen extra Index!
# ============================================================
create table OBJECTITEM
(
    OID                             INTEGER                not null,
    ITEMNAME                        VARCHAR(50)            not null,
    primary key (OID, ITEMNAME),
    foreign key (OID)
       references OBJECT (OID) ON DELETE CASCADE,
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

