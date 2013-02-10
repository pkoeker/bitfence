package de.pk86.bf;
import java.util.ArrayList;

import de.jdataset.JDataSet;
/**
 * @author peter
 */
public interface ObjectItemServiceIF {
	public void createObject(long oid);
	public long createObject();
	public void deleteObject(long oid);
	public void createItem(String name);
	public void deleteItem(String name);
	public String[] getObjectItems(long oid);
	public boolean hasItem(long oid, String itemname);
	public boolean hasItem(String itemname);
	public int getItemCount(String itemname);
	public String[] findItems(String pattern);
	public void startSession(String transname);
	public void resetAllSessions();
	/** @deprecated @see #performOper(List)*/
	public int performOper(String transname, String itemname, Selection.Oper operand);
	public ExpressionResult performOper(String name, ArrayList<OperToken> al);
	public ExpressionResult getResultSet(String transname);
	public String getObjekts(long[] oids);
	public boolean hasNext(String transname);
	public long[] getNext(String transname);
	public ArrayList<String> getOtherItems(String transname);
	public void endSession(String transname);
	public ExpressionResult execute(String transname, String expression);
	public ExpressionResult execute(String expression);
	public JDataSet getFirstPage(String name);
	public JDataSet getNextPage(String name);
	public JDataSet getPrevPage(String name);
	public int updateObjects(JDataSet ds);
	public void importItems(String text, boolean lowercase);
	public void indexObject(long oid,String text,boolean createItems,boolean lowercase);
	public void startSpider();
	/** @deprecated macht nix sinnvolles */
	public void validate();
	/**
	 * Erzeugt eine leere Datenbasis
	 */
	public void createDatabase();
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
	public void clearDatabase();

}