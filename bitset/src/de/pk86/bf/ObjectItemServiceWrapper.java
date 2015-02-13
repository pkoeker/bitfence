package de.pk86.bf;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.servlet.annotation.WebListener;

import de.jdataset.JDataSet;
import de.pk86.bf.Selection.Oper;

/**
 * Wrapper für Spring- und SOAP-Service 
 * @author peter
 */
@WebService
(    
  serviceName = "bitset",
  portName = "bitset", 
  targetNamespace = "http://pk86.de/bitset", // Wie beim Client-Interface!
  endpointInterface = "de.pk86.bf.ObjectItemSOAPService"
)
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebListener
public class ObjectItemServiceWrapper implements ObjectItemServiceIF {
	private ObjectItemServiceIF me;
	private String type;
	
	public ObjectItemServiceWrapper() {
		me = ObjectItemServiceImpl.getInstance();
		this.type = "soap";
	}
    public ObjectItemServiceWrapper(String type) {
       me = ObjectItemServiceImpl.getInstance();
       this.type = type;
   }

	@Override
   public int createObject(String content) {
	   return me.createObject(content);
   }

	@Override
   public void createObject(int oid, String content) {
		me.createObject(oid, content);
   }

	@Override
   public void createItem(String itemname) {
		me.createItem(itemname);
   }

	@Override
   public void deleteItem(String itemname) {
	   me.deleteItem(itemname);	   
   }

	@Override
   public boolean hasItem(String itemname) {
	   return me.hasItem(itemname);
   }

	@Override
   public int getItemCount(String itemname) {
		return me.getItemCount(itemname);
	}

	@Override
   public String[] findItems(String pattern) {
		return me.findItems(pattern);
	}

	@Override
   public Selection startSession() {
		return me.startSession();
	}

	@Override
   public void resetAllSessions() {
		me.resetAllSessions();
   }

	@Override
	@Deprecated
   public int performOper(int sessionId, String itemname, Oper operand) {
	   return me.performOper(sessionId, itemname, operand);
   }

	@Override
   public ExpressionResult performOper(ArrayList<OperToken> al) {
	   return me.performOper(al);
   }

	@Override
   public ExpressionResult getResultSet(int sessionId) {
	   return me.getResultSet(sessionId);
   }

	@Override
   public String getObjekts(int[] oids) {
		return me.getObjekts(oids);
	}

	@Override
   public String getObjekts(long[] oids) {
	   return me.getObjekts(oids);
   }

	@Override
   public boolean hasNext(int sessionId) {
	   return me.hasNext(sessionId);
   }

	@Override
   public Map<String, Integer> getOtherItems(int sessionId) {
	   return me.getOtherItems(sessionId);
   }

	@Override
   public boolean hasSession(int sessionId) {
	   return me.hasSession(sessionId);
   }

	@Override
   public boolean endSession(int sessionId) {
	   return me.endSession(sessionId);
   }

	@Override
   public ExpressionResult execute(String expression) throws RemoteException {
	   return me.execute(expression);
   }

	@Override
   public int createSession(String expression) throws RemoteException {
	   return me.createSession(expression);
   }

	@Override
   public JDataSet getFirstPage(int sessionId) {
	   return me.getFirstPage(sessionId);
   }

	@Override
   public String getFirstPageString(int sessionId) {
	   return me.getFirstPageString(sessionId);
   }

	@Override
   public JDataSet getNextPage(int sessionId) {
	   return me.getNextPage(sessionId);
   }

	@Override
   public String getNextPageString(int sessionId) {
	   return me.getNextPageString(sessionId);
   }

	@Override
   public JDataSet getPrevPage(int sessionId) {
	   return me.getPrevPage(sessionId);
   }

	@Override
   public String getPrevPageString(int sessionId) {
	   return me.getPrevPageString(sessionId);
   }

	@Override
   public int updateObjects(JDataSet ds) {
	   return me.updateObjects(ds);
   }

	@Override
   public void importItems(String text, boolean lowercase) {
		me.importItems(text, lowercase);
   }

	@Override
   public void indexObject(int oid, String text, boolean createItems, boolean lowercase) {
	   me.indexObject(oid, text, createItems, lowercase);
   }

	@Override
   public void startSpider() {
		me.startSpider();
   }

	@Override
   public int importDatabaseCSV(String content) {
	   return me.importDatabaseCSV(content);
   }

	@Override
   public int importDatabaseDataset(JDataSet data) {
	   return me.importDatabaseDataset(data);
   }

	@Override
   public String getItemCacheStatistics() {
	   return me.getItemCacheStatistics();
   }

	@Override
	public void indexDatabase() {
		me.indexDatabase();
   }

	@Override
   public String sayHello() {
	   return me.sayHello();
   }

	@Override
   public String echo(String s) {
	   return me.echo(s);
   }

	@Override
   public ExpressionResult select(String expression) throws RemoteException {
	   return me.select(expression);
   }
}
