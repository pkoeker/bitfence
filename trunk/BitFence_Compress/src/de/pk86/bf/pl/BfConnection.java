package de.pk86.bf.pl;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

import org.apache.tomcat.jdbc.pool.PooledConnection;
import org.apache.tomcat.jdbc.pool.ProxyConnection;


/**
 * @deprecated
 * Wrapper-Klasser um eine JDBC-Connection zur Datenbank.
 */
final class BfConnection {

  private static org.apache.log4j.Logger       logger = org.apache.log4j.Logger
                                                        .getLogger(BfConnection.class);
  private String                               label;
  private Connection                           connection;
  private BfDatabase                           config;
  private String                               layerName;
  private BfTransactionManager                 tm;
  // Debug-Info
  private Date                                 created;
  private Date                                 lastUsed;
  private Thread                               owner;
  private String                               sessionID;
  // ID
  private long        id;
  private static long idCounter;
  private boolean     closed; // als Ersatz für isClosed, weil das geht übers Netz an die Datenbank!
  /**
   * Erzeugt ein neues DatabaseConnection Objekt <br>
   * <i>Creates a new DatabaseConnection object </i>
   * @param config
   *           Einstellungen f&uuml;r diese neue DatabaseConnection. <br>
   *           <i>Settings für this new DatabaseConnection. </i>
   * @param conn Eine JDBC-Connection aus dem ConnectionPool.
   * @throws BfException
   *            wenn die neue DatabaseConnection nicht initialisiert werden
   *            kannn. <br>
   *            <i>If the new DatabaseConnection could not be initalized. </i>
   */
  BfConnection(BfDatabase database, Connection conn) {
    this.config = database;
    this.layerName = config.getLayerName();
    this.created = new Date();
    this.lastUsed = new Date();
    this.connection = conn;
    // Beliebig viele benannte Sequences
    synchronized (BfConnection.class) {
      this.id = idCounter++;             
    }
    // Transaction Manager erzeugen
    this.tm = new BfTransactionManager(this);
  }
  
  BfDatabase getDatabase() {
    return this.config;
  }

  void close(String reason) throws SQLException {
    try {
      synchronized (connection) {
        if (!closed) {
        //if (!connection.isClosed()) { // Das dauert zu lange! Extra Netzwerkzugriff!
          logger.debug("Closing a connection: " + this.toString() + " Reason: " + reason);
          this.connection.close();
          closed = true;
        } else {
          logger.warn("Closing a connection which is already closed: " + this.label + " Reason: " + reason);
        }
      }
    } catch (SQLException ex) {
      logger.error(ex.getMessage(), ex);
      throw ex;
    }
  }

  /**
   * Schlie&szlig;t die DatabaseConnection, falls JDBC-Connection noch
   * erreichbar ist (con != null). <br>
   * <i>Closes the DatabaseConnection object if the JDBC connection is
   * reachable (con != null). </i>
   */
  protected void finalize() throws Throwable {
    try {
      synchronized (connection) {
        if (this.connection != null) {
          if (!closed) {
            logger.warn("PL [ " + layerName + "/" + this.label + " ]  Owner: [ "
              + this.getOwnerName() + " ] closing DatabaseConnection " + this.toString());
            this.close("finalize");
          }
        }
      }
    } finally {
      super.finalize();
    }
  }

  void startTransaction(String transName) {
    this.tm.startTransaction(transName);
  }

  boolean testCommit() {
    return this.tm.testCommit();
  }

  boolean rollbackTransaction(String transName) throws BfException {
    boolean rollback = this.tm.rollbackTransaction(transName);
    updateLastUsed();
    return rollback;
  }

  boolean commitTransaction(String transName) throws BfException {
    boolean committed = tm.commitTransaction(transName);
    updateLastUsed();
    return committed;
  }

  boolean abortTransaction(String transName) throws BfException {
    boolean aborted = this.tm.abortTransaction(transName);
    return aborted;
  }

  boolean hasOpenTransaction() {
    return tm.hasOpenTransaction();
  }

  String getOpenTransactions() {
    return tm.getOpenTransactions();
  }

  /**
   * Liefert die Connection zur Datenbank
   * @return Die JDBC-Connection
   * @throws BfException
   */
  Connection getConnection() throws BfException {
     if (closed) {
       String msg = "Getting Connection which is already been closed! " + this.label;
       logger.error(msg);
       throw new BfException(msg);
     }
    this.updateLastUsed();
    return connection;
  }

  int getDatabaseType() {
    return config.getDatabaseType();
  }

  /**
   * @return Returns the layerName.
   */
  String getLayerName() {
    return layerName;
  }


  BfTransactionManager getTransactionManager() {
    return this.tm;
  }

  void shutdown() {
    try {
      if (connection != null) {
        connection.close();
      }
    } catch (SQLException ex) {
      logger.error(
        "PL [ " + layerName + " ] " + "Unable to shutdown Database.\n" + ex.getMessage(), ex);
    }
  }


  boolean isDebug() {
    return this.config.isDebug();
  }
  
  long getId() {
    return this.id;
  }
  
  Date getCreatedTimestamp() {
    return this.created;
  }


  boolean isCommited() {
    return tm.isCommited();
  }

  boolean isRollback() {
    return tm.isRollback();
  }

  String getOptimisticField() {
    return this.config.getOptimisticField();
  }

  /**
   * @return Returns the connectionTimeOut. After this time the connection
   *         should be checked
   */
  long getConnectionTimeOut() {
    return config.getConnectionTimeOut();
  }

  /**
   * @return Returns the transactionLevel.
   */
  int getTransactionLevel() {
    return config.getTransactionIsolationLevel();
  }

  /**
   * @return Returns the owner.
   */
  Thread getOwner() {
    return owner;
  }

  /**
   * @param owner
   *           The owner to set.
   */
  void setOwner(Thread owner) {
    this.owner = owner;
  }

  /**
   * Liefert den Namen des Owner-Thread oder null, wenn keine Owner.
   * @return
   */
  String getOwnerName() {
    if (owner != null) {
      return owner.getName();
    }
    return null;
  }

  Date getLastusedTimestamp() {
    return lastUsed;
  }

  private void updateLastUsed() {
    this.lastUsed = new Date();
  }

  String getCreateUserField() {
    return this.config.getCreateUserField();
  }

  String getUpdateUserField() {
    return this.config.getUpdateUserField();
  }

  /**
   * Antwortet mit dem Datenbanknamen. <br>
   * <i>Returns the databaseName. </i>
   */
  String getDatabaseName() {
    return this.config.getDatabaseName();
  }
  
  /**
   * @return Returns the label.
   */
  String getLabel() {
    return label;
  }

  /**
   * @param label
   *           The label to set.
   */
  void setLabel(String label) {
    this.label = label;
    //this.setClientInfo("label", label);
  }
  
   String getDatabaseSessionID() {
      if(this.sessionID != null) {
         return sessionID;
      }
      sessionID = "-1"; // default
      switch(config.getDatabaseType()) {
        case BfDatabase.POSTGRES: {
          Object oconn1 = getMember(connection.getClass(), connection, "h");
          if (oconn1 != null) {
            Object oconn2 = getMember(oconn1.getClass(), oconn1, "connection");
            if (oconn2 != null) {
               Object oconn3 = getMember(oconn2.getClass(), oconn2, "connection");
               if (oconn3 != null) {
	              Object opc = getMember(oconn3.getClass(), oconn3, "protoConnection");
	              if (opc != null) {
	                Object pid = getMember(opc.getClass(), opc, "cancelPid");
	                sessionID = pid.toString();
	              }
	            }
	          }
	        }
        }
          break;
        case BfDatabase.MAX_DB:
            try {
              Object o = getMember(connection.getClass(), connection, "h"); 
              if(o instanceof ProxyConnection) { // tomcat
                ProxyConnection prcon = (ProxyConnection)o;
                PooledConnection poolcon = prcon.getConnection();
                Connection conn = poolcon.getConnection();
              }
            }
            catch (Exception ex) {
              logger.debug(ex.getMessage(), ex);
            }
            break;
      }
      return sessionID;
   }
   
  private Object getMember(Class<?> clazz, Object op, String name) {
    try {
      Field fp = clazz.getDeclaredField(name);
      fp.setAccessible(true);
      Object o = fp.get(op);
      return o;
    } catch (SecurityException ex) {
      ex.printStackTrace();
    } catch (NoSuchFieldException ex) {
      if (clazz.getSuperclass() != null) {
        Class<?> sc = clazz.getSuperclass();
        return getMember(sc, op, name);
      } else {
        logger.debug(ex.getMessage(), ex);
      }
    } catch (IllegalArgumentException ex) {
      logger.debug(ex.getMessage(), ex);
    } catch (IllegalAccessException ex) {
      logger.debug(ex.getMessage(), ex);
    }
    return null;
  }
   
  void setIsolationLevel(int level) {
    if (connection != null) {
      try {
        int l = connection.getTransactionIsolation();
        if (l != level) {
          connection.setTransactionIsolation(level);          
        }
      } catch (SQLException ex) {
        logger.error("Can't set TransactionIsolationLevel: " + level, ex);
      }
    }
  }      
   
  public String toString() {
    if (label != null && label.length() != 0 && sessionID != null) {
      return label + "/" + sessionID;
    }
    return super.toString();
  }
}