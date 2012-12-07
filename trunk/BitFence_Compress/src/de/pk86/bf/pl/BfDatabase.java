package de.pk86.bf.pl;

import java.sql.Connection;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import de.pkjs.util.Convert;
import electric.xml.DocType;
import electric.xml.Document;
import electric.xml.Element;
import electric.xml.Elements;
/**
 * @deprecated
 */
final class BfDatabase  {
  // Database Software
  public static final int UNKNOWN = 0;
  public static final int JDBC_ODBC = 1;
  public static final int MYSQL = 2;
  public static final int FIREBIRD = 3;
  public static final int SQL_SERVER = 4;
  public static final int MCKOI = 5;
  public static final int MAX_DB = 6;
  public static final int SYBASE = 7;
  public static final int ORACLE = 8;
  public static final int CACHE = 9;
  public static final int DB2 = 10;
  public static final int AXION = 11;
  public static final int HSQLDB = 12;
  public static final int POSTGRES = 13;
  private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BfDatabase.class);
  
  // Default Formats
  private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
  private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
  private SimpleDateFormat timestampFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
  private DecimalFormat decimalFormat = new DecimalFormat("#0.00");

  // Attributes
  public static final LinkedHashMap<String, Integer> databaseTypes = new LinkedHashMap<String, Integer>();

  private Element elDatabase;

  private int dbType = UNKNOWN; // DataBaseType
  private boolean isDebug;
  //private String encoding = "UTF-8";
  private String databaseName;
  private String jdbcDriver;
  private String databaseURL;
  private String catalog;
  private String schema;
  String getSchema() {
    return schema;
  }  

  private String username;
  private String password;
  private int transactionIsolationLevel = Connection.TRANSACTION_READ_COMMITTED; // Default
  /**
   * SessionID der Datenbank ermitteln (bei hoch belasteten Datenbanken besser ausschalten)
   */
  private boolean detectDatabaseSessionID;
  public boolean isDetectDatabaseSessionID() {
	  return detectDatabaseSessionID;
  }
  public void setDetectDatabaseSessionID(boolean b) {
	  this.detectDatabaseSessionID = b;
  }
  private String validationQuery;
  private String initSQL;

  // Options
  private String optimisticField;
  private String createUserField;
  private String updateUserField;
  private Logger slowQueryLogger;
  private int defaultMaxExcutionTime = 5000;
  
  private String datasetDefinitionFileName;
  
  private final java.util.Date createdTimeStamp = new java.util.Date();
  private java.util.Date resetTimeStamp;

  private PoolProperties pp = new PoolProperties();
  private org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
  
  org.apache.tomcat.jdbc.pool.DataSource getDataSource() {
    return dataSource;
  }

  // Constructor ===================================
  BfDatabase(Element root, String username, String password) throws BfException {
    // Aktivierte DB rausfischen
    Elements dbEles = root.getElements("Database");
    while (dbEles.hasMoreElements()) {
      Element dbEle = dbEles.next();
      String sEn = dbEle.getAttribute("enabled");
      if (sEn == null || sEn.equalsIgnoreCase("true")) {
        elDatabase = dbEle;
      }
    }
    if (elDatabase == null) {
      throw new IllegalStateException("No enabled Database defined!");
    }

    // DatabaseName
    this.databaseName = elDatabase.getAttribute("name");
    { // Options; Muß zuerst erfolgen wegen debug!
      Element optEle = root.getElement("Options");
      if (optEle != null) {
        // Debug
        Element debugEle = optEle.getElement("Debug");
        if (debugEle != null) {
          String sDebug = debugEle.getAttribute("value");
          if (sDebug != null && sDebug.equals("true")) {
            this.isDebug = true;
          }
        }
        // SlowQuery
        Element slowEle = optEle.getElement("SlowQueryLogger");
        if (slowEle != null) {
          String loggerName = slowEle.getAttribute("loggerName");
          slowQueryLogger = Logger.getLogger(loggerName);
          String s = slowEle.getAttribute("defaultMaxExecutionTime");
          if (s != null) {
             defaultMaxExcutionTime = Convert.toInt(s);
          }
        }
      }
    }


    // DatabaseType
    String _dbType = elDatabase.getAttribute("type");
    if (_dbType != null) {
      this.dbType = getSupportedDatabaseType(_dbType);
      if (this.dbType == UNKNOWN)
        logger
            .error("PL [ " + databaseName + " ] " + "new Database: Unknown DatabaseType: '" + _dbType + "'");
    }

    Element driverEle = elDatabase.getElement("JDBC-Driver");
    if (driverEle != null) {
      jdbcDriver = driverEle.getTextString();
      pp.setDriverClassName(jdbcDriver);
    }
    Element urlEle = elDatabase.getElement("URL");
    if (urlEle != null) {
      databaseURL = urlEle.getTextString();
      pp.setUrl(databaseURL);
      logger.info("DatabaseURL: "+databaseURL);
    }
//    Element dataSourceEle = elDatabase.getElement("DataSource");
//    if (dataSourceEle != null) {
//      dataSource = dataSourceEle.getTextString();
//      logger.info("DataSouce: "+dataSource);
//    }
    this.setDatabaseType(databaseURL);
    // Alles raus, was nicht zu dieser DB gehört.
    this.removeOtherDatabaseElements(elDatabase, this.getDatabaseType());

    Element catEle = elDatabase.getElement("Catalog");
    if (catEle != null) {
      catalog = catEle.getTextString();
      pp.setDefaultCatalog(catalog);
    }

    Element schEle = elDatabase.getElement("Schema");
    if (schEle != null) {
      schema = schEle.getTextString();
    }
    if (username != null && password != null) {
      this.username = username;
      this.password = password;
    } else { // Username + Password nicht als Argument übergeben
      Element userEle = elDatabase.getElement("Username");
      if (userEle != null) {
        this.username = userEle.getTextString();
      }
      Element passwordEle = elDatabase.getElement("Password");
      if (passwordEle != null) {
        this.password = passwordEle.getTextString();
      }
    }
    pp.setUsername(this.username);
    pp.setPassword(this.password);

    // TransactionIsolation
    Element isoEle = elDatabase.getElement("TransactionIsolationLevel");
    if (isoEle != null) {
      String sTransactionLevel = isoEle.getAttribute("value");
      this.transactionIsolationLevel = getTransactionIsolationLevel(sTransactionLevel);
      // siehe initConnection
    }
    pp.setDefaultTransactionIsolation(this.getTransactionIsolationLevel());
    // detect Session
    Element detEle = elDatabase.getElement("DetectDatabaseSessionID");
    if (detEle != null) {
      String sDet = isoEle.getAttribute("value");
      this.setDetectDatabaseSessionID(Convert.toBoolean(sDet));
    }

    // Config
    // MaxActiveConnections
    Element elMaxActiveConnections = elDatabase.getElement("MaxActiveConnections");
    if (elMaxActiveConnections != null) {
      String sMaxActiveConnections = elMaxActiveConnections.getAttribute("value");
      if (sMaxActiveConnections != null) {
        pp.setMaxActive(Integer.parseInt(sMaxActiveConnections));
      }
    }

    // MaxIdleConnections
    Element elMaxIdleConnections = elDatabase.getElement("MaxIdleConnections");
    if (elMaxIdleConnections != null) {
      String sMaxIdleConnections = elMaxIdleConnections.getAttribute("value");
      if (sMaxIdleConnections != null) {
        pp.setMaxIdle(Integer.parseInt(sMaxIdleConnections));
      }
    }

    // MinIdleConnections
    Element elMinIdleConnections = elDatabase.getElement("MinIdleConnections");
    if (elMinIdleConnections != null) {
      String sMinIdleConnections = elMinIdleConnections.getAttribute("value");
      if (sMinIdleConnections != null) {
        pp.setMinIdle(Integer.parseInt(sMinIdleConnections));
      }
    }
    Element elConnectionTimeOut = elDatabase.getElement("ConnectionTimeOut");
    if (elConnectionTimeOut != null) {
      String value = elConnectionTimeOut.getAttribute("value");
      if (value != null) {
        /*
         * TODO: Was soll ConnectionTimeout eigentlich bewirken? maxWait, ist
         * jedenfalls die Zeit, die ein borrowObject dauern darf, bis eine
         * Exception geworfen wird (falls der Pool erschöpft ist)
         */
        pp.setMaxWait(Integer.parseInt(value));
      }
    }
    // testOnBorrow: default = true 
    boolean testOnBorrow = true;
    Element elTestOnBorrow = elDatabase.getElement("TestOnBorrow"); {
      if (elTestOnBorrow != null) {
         testOnBorrow = Convert.toBoolean(elTestOnBorrow.getAttribute("value"));
      }
    }
    pp.setTestOnBorrow(testOnBorrow);
    // testOnReturn
    Element elTestOnReturn = elDatabase.getElement("TestOnReturn"); {
      if (elTestOnReturn != null) {
        boolean b = Convert.toBoolean(elTestOnReturn.getAttribute("value"));
        pp.setTestOnReturn(b);
      }
    }
    // Validation
    Element elValidQuery = elDatabase.getElement("ValidationQuery");
    if (elValidQuery != null) {
      this.validationQuery = elValidQuery.getTextString();
    }
    pp.setValidationQuery(this.getValidationQuery()); // lädt u.U. den default
    // ValidInter
    Element elValidInter = elDatabase.getElement("ValidationInterval"); {
      if (elValidInter != null) {
        int i = Convert.toInt(elValidInter.getAttribute("value"));
        pp.setValidationInterval(i);
      }
    }
    Element elInitScript = elDatabase.getElement("InitializationScript");
    if (elInitScript != null) {
      this.initSQL = elInitScript.getTextString();
      pp.setInitSQL(initSQL);
    }


    // Format
    Element form = root.getElement("Format");
    if (form != null) {
      try {
        dateFormat = new SimpleDateFormat(form.getElement("DateFormat").getAttribute("value"));
      } catch (Exception ex) {
        System.err.println("PL [ " + this.getLayerName() + " ] " + "PLConfig.xml: Missing DateFormat");
      }
      try {
        timeFormat = new SimpleDateFormat(form.getElement("TimeFormat").getAttribute("value"));
      } catch (Exception ex) {
        System.err.println("PL [ " + this.getLayerName() + " ] " + "PLConfig.xml: Missing TimeFormat");
      }
      try {
        timestampFormat = new SimpleDateFormat(form.getElement("TimestampFormat").getAttribute("value"));
      } catch (Exception ex) {
        System.err.println("PL [ " + this.getLayerName() + " ] " + "PLConfig.xml: Missing TimestampFormat");
      }
      try {
        decimalFormat = new DecimalFormat(form.getElement("DecimalFormat").getAttribute("value"));
      } catch (Exception ex) {
        System.err.println("PL [ " + this.getLayerName() + " ] " + "PLConfig.xml: Missing DecimalFormat");
      }
    }    
  }

  /**
   * Löscht alle Elemente aus dem XML-Teilbaum, die für einen anderen
   * Datenbank-Typ vorgesehen sind.
   * 
   * @param ele
   * @param databaseType
   */
  private void removeOtherDatabaseElements(Element ele, int databaseType) {
    String s = ele.getAttribute("databaseType");
    if (s != null) {
      int type = this.getSupportedDatabaseType(s);
      if (type != databaseType) {
        @SuppressWarnings("unused")
        boolean removed = ele.remove(); // funktioniert irgendwie nicht!
        return;
      }
    }
    Elements eles = ele.getElements();
    this.removeOtherDatabaseElements(eles, databaseType);
  }

  private void removeOtherDatabaseElements(Elements eles, int databaseType) {
    if (eles == null)
      return;
    while (eles.hasMoreElements()) {
      Element child = eles.next();
      this.removeOtherDatabaseElements(child, databaseType);
    }
  }


  /**
   * Liefert die Zugriffsdefinitionen aus DatabaseConfig.xml
   * 
   * @return
   */
  public Document getDatabaseConfig() {
    Document doc = new Document();
    doc.setEncoding("UTF-8");
    DocType dt = new DocType("Server");
    dt.setSystemId("PLConfig.dtd");
    doc.addChild(dt);
    Element reqs = new Element("Server");
    doc.setRoot(reqs);

    return doc;
  }

  /**
   * Der Cache mit den Datenbankabfragen wird gelöscht.
   * 
   * @throws BfException
   */
  public void reset(BfConnection dbConnection) throws BfException {
    this.resetTimeStamp = new Date();
  }


  static int getTransactionIsolationLevel(String sTransactionLevel) {
    int level = -1;
    if (sTransactionLevel != null) {
      if (sTransactionLevel.equalsIgnoreCase("TRANSACTION_NONE")) {
        level = Connection.TRANSACTION_NONE;
      } else if (sTransactionLevel.equalsIgnoreCase("TRANSACTION_READ_UNCOMMITTED")) {
        level = Connection.TRANSACTION_READ_UNCOMMITTED;
      } else if (sTransactionLevel.equalsIgnoreCase("TRANSACTION_READ_COMMITTED")) {
        level = Connection.TRANSACTION_READ_COMMITTED;
      } else if (sTransactionLevel.equalsIgnoreCase("TRANSACTION_REPEATABLE_READ")) {
        level = Connection.TRANSACTION_REPEATABLE_READ;
      } else if (sTransactionLevel.equalsIgnoreCase("TRANSACTION_SERIALIZABLE")) {
        level = Connection.TRANSACTION_SERIALIZABLE;
      } else {
        logger.error("Illegal TransactionIsolationLevel: " + sTransactionLevel);
        level = -1;
      }
    }
    return level;
  }

  /**
   * Ändert den TransactionIsolationLevel Wirft eine IllegalArgumentEx, wenn
   * ungültiger Level
   * 
   * @see Connection
   */
  public void setTransactionIsolationLevel(int level) {
    if (level != Connection.TRANSACTION_NONE && level != Connection.TRANSACTION_READ_UNCOMMITTED
        && level != Connection.TRANSACTION_READ_COMMITTED && level != Connection.TRANSACTION_REPEATABLE_READ
        && level != Connection.TRANSACTION_SERIALIZABLE)
      throw new IllegalArgumentException("Illegal TransactionIsolationLevel");
    this.transactionIsolationLevel = level;
  }


  public Element getElement() {
    Element ele = new Element("Database");
    ele.setAttribute("name", this.databaseName);
    // JDBC-Driver
    Element driverEle = ele.addElement("JDBC-Driver");
    driverEle.setText(this.jdbcDriver);
    // URL
    Element urlEle = ele.addElement("URL");
    urlEle.setText(this.databaseURL);
    // Catalog
    if (istLeer(this.catalog) == false) {
      Element catEle = ele.addElement("Catalog");
      catEle.setText(this.catalog);
    }
    // Schema
    if (istLeer(this.schema) == false) {
      Element schemaEle = ele.addElement("Schema");
      schemaEle.setText(this.schema);
    }
    // Username
    Element userEle = ele.addElement("Username");
    userEle.setText(this.username);
    // Password ???
    Element passEle = ele.addElement("Password");
    passEle.setText(this.password);
    // Isolation Level
    if (this.transactionIsolationLevel != -1) {
      Element traEle = ele.addElement("TransactionIsolationLevel");
      traEle.setAttribute("value", Integer.toString(transactionIsolationLevel));
    }
    // Optimistic
    if (istLeer(this.optimisticField) == false) {
      Element optEle = ele.addElement("OptimisticLockingField");
      optEle.setAttribute("value", this.optimisticField);
    }
    // CreateUser
    if (istLeer(this.createUserField) == false) {
      Element cuEle = ele.addElement("CreateUserField");
      cuEle.setAttribute("value", this.createUserField);
    }
    // UpdateUser
    if (istLeer(this.updateUserField) == false) {
      Element upEle = ele.addElement("UpdateUserField");
      upEle.setAttribute("value", this.updateUserField);
    }

    return ele;
  }


  public boolean isDebug() {
    return this.isDebug;
  }

  public String getDatasetDefinitionFileName() {
    return this.datasetDefinitionFileName;
  }

  /**
   * Liefert den Namen dieser Datenbank.
   * 
   * @return
   */
  public String getDatabaseName() {
    return databaseName;
  }

  /**
   * Liefert den Namen des Feldes, über das das optimistische Locking
   * abgewickelt wird.
   * <p>
   * Wenn eine Tabelle keine Column dieses Namens enthält, dann findet kein
   * optimistisches Locking mit dieser Tabelle statt.
   * 
   * @return
   */
  public String getOptimisticField() {
    return optimisticField;
  }

  /**
   * Liefert den Namen des Feldes, in das der Benutzername beim Insert
   * eingetragen werden soll.
   * 
   * @return
   */
  public String getCreateUserField() {
    return createUserField;
  }

  /**
   * Liefert den Namen des Feldes, in das der Benutzername beim Update
   * eingetragen werden soll.
   * 
   * @return
   */
  public String getUpdateUserField() {
    return updateUserField;
  }

  private void setDatabaseType(String url) {
    if (url == null)
      return;
    String s = url.toLowerCase();
    if (s.startsWith("jdbc:mysql")) {
      this.dbType = MYSQL;
    } else if (s.startsWith("jdbc:mckoi")) {
      this.dbType = MCKOI;
    } else if (s.startsWith("jdbc:odbc")) {
      this.dbType = JDBC_ODBC;
    } else if (s.startsWith("jdbc:microsoft:sqlserver")) {
      this.dbType = SQL_SERVER;
    } else if (s.startsWith("jdbc:jtds:sqlserver")) {
      this.dbType = SQL_SERVER;
    } else if (s.startsWith("jdbc:firebirdsql")) {
      this.dbType = FIREBIRD;
    } else if (s.startsWith("jdbc:sapdb")) {
      this.dbType = MAX_DB;
    } else if (s.startsWith("jdbc:sybase")) {
      this.dbType = SYBASE;
    } else if (s.startsWith("jdbc:oracle")) {
      this.dbType = ORACLE;
    } else if (s.startsWith("jdbc:cache")) {
      this.dbType = CACHE;
    } else if (s.startsWith("jdbc:db2")) {
      this.dbType = DB2;
    } else if (s.startsWith("jdbc:hsqldb")) {
      this.dbType = HSQLDB;
    } else if (s.startsWith("jdbc:postgresql")) {
      this.dbType = POSTGRES;
    } else {
      logger.warn("PL [ " + databaseName + " ] "
          + "Database#setDatabaseType Warning: Unknown Database Type: " + url);
    }
  }

  public int getDatabaseType() {
    return this.dbType;
  }

  // Meta


  private int getSupportedDatabaseType(String _dbType) {
    _dbType = _dbType.toUpperCase();
    if (databaseTypes.containsKey(_dbType)) {
      Integer i = databaseTypes.get(_dbType);
      return i.intValue();
    } else
      return UNKNOWN;
  }

  private static boolean istLeer(String s) {
    if (s == null || s.length() == 0) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Liefert die URL über die der JDBC-Trieber auf die Datenbank zugreift.
   * <p>
   * Beispiel: "jdbc:sapdb://myServerName/myDatabaseName"
   * 
   * @return
   */
  public String getDatabaseURL() {
    return databaseURL;
  }

  /**
   * Liefert die Klassennamen des JDBC-Treibers.
   * <p>
   * Beispiel: "com.sap.dbtech.jdbc.DriverSapDB"
   * 
   * @return
   */
  public String getJDBCDriver() {
    return this.jdbcDriver;
  }

  /**
   * @return Returns the layerName.
   */
  public String getLayerName() {
    return this.databaseName;
  }


  /**
   * @return Returns the password.
   */
  public String getPassword() {
    return this.password;
  }

  /**
   * @return Returns the transactionLevel.
   */
  public int getTransactionIsolationLevel() {
    return this.transactionIsolationLevel;
  }

  /**
   * Liefert das Statement, welches zum anpingen der Datenbank verwendet werden
   * soll; z.B.: SELECT 1 FROM DUAL (Oracle, MAXDB); SELECT 1 AS Test (Postgres)
   * ValidationQuery
   * 
   * @return
   */
  public String getValidationQuery() {
     if (this.validationQuery == null) {
        switch (this.dbType) {
           default: // MAXDB, Oracle OK
              validationQuery = "SELECT 1 FROM DUAL";
              break;
           case POSTGRES:
              validationQuery = "SELECT 1 AS Test";
              break;
           case MYSQL:
           case SQL_SERVER:
              validationQuery = "SELECT 1";
           break;
        }
     }
    return this.validationQuery;
  }

  /**
   * Dieses Statement wird beim Erzeugen ienr Connection zuerst ausgeführt
   * Siehe Tomcat-Property initSQL
   */
  public String getInitSQL() {
    return this.initSQL;
  }

  public String getUsername() {
    return this.username;
  }

  public boolean isAutocommit() {
    return false;
  }

  public PoolProperties getPoolConfig() {
    return this.pp;
  }

  public int getMaxActive() {
    return pp.getMaxActive();
  }

  public int getMinIdle() {
    return pp.getMinIdle();
  }

  public int getMaxIdle() {
    return pp.getMaxIdle();
  }

  public long getConnectionTimeOut() {
    return this.pp.getMaxWait();
  }

  public SimpleDateFormat getDateFormat() {
    return this.dateFormat;
  }

  public SimpleDateFormat getTimeFormat() {
    return this.timeFormat;
  }

  public SimpleDateFormat getTimestampFormat() {
    return this.timestampFormat;
  }

  /**
   * Liefert das heutige Datum im gewählten Format (dd.MM.yyyy).
   * 
   * @see #getDateFormat
   */
  public String getTodayString() {
    String s = getDateFormat().format(new java.util.Date());
    return s;
  }

  /**
   * Liefert die aktuelle Uhrzeit im gewähltenFormat (HH:mm).
   * 
   * @see #getTimeFormat
   */
  public String getNowString() {
    String s = getTimeFormat().format(new java.util.Date());
    return s;
  }

  public String getTodayNowString() {
    String s = getTimestampFormat().format(new java.util.Date());
    return s;
  }

  public void setDebug(boolean state) {
    this.isDebug = state;
  }

  public DecimalFormat getDecimalFormat() {
    return this.decimalFormat;
  }


  public Date getCreatedTimeStamp() {
    return this.createdTimeStamp;
  }

  public Date getResetTimeStamp() {
    return this.resetTimeStamp;
  }
  
  /**
   * 
   * @return null, wenn kein Logger definiert
   */
  Logger getSlowQueryLogger() {
    return this.slowQueryLogger;
  }
  
  int getDefaultMaxExecutionTime() {
    return defaultMaxExcutionTime;
  }
  /**
   * true, wenn die Datenbank über die Syntax "... LIMIT #" verfügt
   * @return
   */
  boolean hasLimit() {
    switch(this.dbType) {
      case MAX_DB:
      case POSTGRES:
      case MYSQL:
        return true;
    }
    return false;
  }

  static {
    databaseTypes.put("JDBC_ODBC", new Integer(JDBC_ODBC));
    databaseTypes.put("MYSQL", new Integer(MYSQL));
    databaseTypes.put("FIREBIRD", new Integer(FIREBIRD));
    databaseTypes.put("SQL_SERVER", new Integer(SQL_SERVER));
    databaseTypes.put("MCKOI", new Integer(MCKOI));
    databaseTypes.put("MAXDB", new Integer(MAX_DB));
    databaseTypes.put("SYBASE", new Integer(SYBASE));
    databaseTypes.put("ORACLE", new Integer(ORACLE));
    databaseTypes.put("CACHE", new Integer(CACHE));
    databaseTypes.put("DB2", new Integer(DB2));
    databaseTypes.put("AXION", new Integer(AXION));
    databaseTypes.put("HSQLDB", new Integer(HSQLDB));
    databaseTypes.put("POSTGRES", new Integer(POSTGRES));
  }

}
