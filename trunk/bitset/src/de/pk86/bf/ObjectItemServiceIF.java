package de.pk86.bf;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;

import de.jdataset.JDataSet;
/**
 * @author peter
 */
public interface ObjectItemServiceIF {
	public static String DEFAULT_DELIM = " ;.,()/-";
	public static int MAX_SESSIONS = 1000;
	
	/**
	 * Erzeugt ein beliebiges Text-Objekt mit dem angegebenen Inhalt.
	 * Die Objekt-Id wird vom System vergeben.
	 * @param content
	 * @return Die vergebene Objekt-Id
	 */
	public long createObject(String content);
	/**
	 * Erzeugt ein beliebiges Text-Objekt mit dem angegebenen Inhalt
	 * unter der angegebenen Objekt-Id
	 * @param oid Eindeutige Id für Objekte
	 * @param content Mit White Space getrennter Inhalt
	 */
	public void createObject(long oid, String content);
	/**
	 * Erzeugt ein neues Schlüsselwort
	 * @param itemname
	 */
	public void createItem(String itemname);
	public void deleteItem(String itemname);
	public boolean hasItem(String itemname);
	public int getItemCount(String itemname);
	public String[] findItems(String pattern);
	/**
	 * Wirft eine IllegalArgumentException, wenn MAX_SESSIONS (1000) überschritten
	 * @return
	 */
	public Selection startSession();
	/**
	 * Löscht alle Sessions
	 */
	public void resetAllSessions();
	/** @deprecated @see #performOper(List)*/
	public int performOper(int sessionId, String itemname, Selection.Oper operand);
	public ExpressionResult performOper(ArrayList<OperToken> al);
	public ExpressionResult getResultSet(int sessionId);
	public String getObjekts(long[] oids);
	public boolean hasNext(int sessionId);
	public Map<String,Integer> getOtherItems(int sessionId);
	/**
	 * Liefert true, wenn eine Session mit der angegebenen Id existiert
	 * vor {@link #endSession(int)} aufrufen.
	 * @param sessionId
	 * @return
	 */
	public boolean hasSession(int sessionId);
	/**
	 * @param sessionId
	 * @return false, wenn die angegebene Session nicht (mehr) existiert.
	 */
	public boolean endSession(int sessionId);
	public ExpressionResult execute(String expression) throws RemoteException;
	public JDataSet getFirstPage(int sessionId);
	public JDataSet getNextPage(int sessionId);
	public JDataSet getPrevPage(int sessionId);
	public int updateObjects(JDataSet ds);
	public void importItems(String text, boolean lowercase);
	public void indexObject(long oid,String text,boolean createItems,boolean lowercase);
	public void startSpider();
	/**
	 * Importiert Objekte in die Datenbasis;
	 * Objekte werden zeilenweise erwartet; 
	 * die Eigenschaften der Objekte durch white space getrennt.
	 * @param data
	 * @return Anzahl importierter Objekte
	 */
	public int importDatabaseCSV(String content);
	public int importDatabaseDataset(JDataSet data);
	/**
	 * Indiziert die Datenbasis; der Index (BitZaun) wird aus den Objekten neu aufgebaut
	 */
	public void indexDatabase();

}