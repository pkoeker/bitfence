package de.pk86.bf.pl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashSet;

import org.apache.log4j.Logger;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import electric.xml.Element;

/**
 * @deprecated
 * Wrapper für den Tomcat Connection Pool
 * org.apache.tomcat.jdbc.pool.DataSource
 */
final class BfConnectionPool {
   private static final Logger logger = Logger.getLogger(BfConnectionPool.class);

   private BfDatabase config;
   private LinkedHashSet<String> sessions = new LinkedHashSet<String>();

   /**
    * Erzeugt einen neuen DatabaseConnectionPool mit den gegebenen
    * Einstellungen. <br>
    * <i>Creates a new DatabaseConfigurationPool with the given settings. </i>
    * @param config
    *           Einstellungen f&uuml;r den neuen Pool. <br>
    *           <i>Settings for the new pool. </i>
    */
   BfConnectionPool(BfDatabase config) throws BfException {
      this.config = config;
      this.setupDriver();
   }

   int getNumIdle() {
     return config.getDataSource().getNumIdle();    
   }

   int getNumActive() {
     return config.getDataSource().getNumActive();    
   }

   /**
    * Liefert eine DatabaseConnection aus dem ConnectionPool.
    */
   BfConnection getConnection(String label, boolean autocommit) throws BfException {
		try {
			Connection conn = config.getDataSource().getConnection();
			int il = conn.getTransactionIsolation();
			int ilconf = config.getTransactionIsolationLevel();
			if (il != ilconf) {
				conn.setTransactionIsolation(ilconf);
				logger.debug("Set Isolation Level from/to: " + il +"/"+ilconf);
			}
			conn.setAutoCommit(autocommit);
			BfConnection dbConn = new BfConnection(this.config, conn);
			dbConn.setOwner(Thread.currentThread());
			dbConn.setLabel(label);
			if (!autocommit) {
				dbConn.startTransaction(label);
			}
			// SessionID
			if (config.isDetectDatabaseSessionID()) {
				String sessionID = dbConn.getDatabaseSessionID();
				boolean added = false;
				synchronized (sessions) { // LinkedHashSet nicht ThreadSave
					added = sessions.add(sessionID);
				}
				if (added) { // Neue SessionID
					logger.info("new connection: " + label + "/" + sessionID);
				} else { // existierende Connection wiederverwenden
					logger.debug("pooled connection: " + label + "/" + sessionID);
				}				
			}
			return dbConn;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw new BfException(ex);
		}
   }

   /**
    * ShutDown
    */
   void close() throws BfException {
     try {
       config.getDataSource().close();
     } catch (Exception ex) {
       throw new BfException(ex);
     }
   }

   /**
    * Liefert ein Element <Pool ...
    * @return
    */
   Element getElement() {
      Element root = new Element("Pool");
      root.setAttribute("NumActive", Integer.toString(getNumActive()));
      root.setAttribute("NumIdle", Integer.toString(getNumIdle()));
      return root;
   }

   private void setupDriver() throws BfException {
//     PoolProperties p = new PoolProperties();
//     p.setUrl("jdbc:mysql://localhost:3306/mysql");
//     p.setDriverClassName("com.mysql.jdbc.Driver");
//     p.setUsername("root");
//     p.setPassword("password");
//     p.setJmxEnabled(true);
//     p.setTestWhileIdle(false);
//     p.setTestOnBorrow(true);
//     p.setValidationQuery("SELECT 1");
//     p.setTestOnReturn(false);
//     p.setValidationInterval(30000);
//     p.setTimeBetweenEvictionRunsMillis(30000);
//     p.setMaxActive(100);
//     p.setInitialSize(10);
//     p.setMaxWait(10000);
//     p.setRemoveAbandonedTimeout(60);
//     p.setMinEvictableIdleTimeMillis(30000);
//     p.setMinIdle(10);
//     p.setLogAbandoned(true);
//     p.setRemoveAbandoned(true);
//     p.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"+
//       "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
     
     PoolProperties pp = config.getPoolConfig();
     // Connection hält dann eine TrapException!?
//     pp.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"+
//         "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
     config.getDataSource().setPoolProperties(pp); // Init Datasource
     try {
   	  config.getDataSource().createPool();
     } catch (SQLException e) {
   	  logger.error(e.getMessage(), e);
   	  throw new BfException(e);
     }     
  }  
}