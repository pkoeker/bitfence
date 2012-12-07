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
	public void addObjectItem(long oid, String itemname);
	public void removeObjectItem(long oid, String itemname);
	public String[] getObjectItems(long oid);
	public boolean hasItem(long oid, String itemname);
	public boolean hasItem(String itemname);
	public int getItemCount(String itemname);
	public String[] findItems(String pattern);
	public void startSession(String transname);
	public void resetAllSessions();
	public int performOper(String transname, String itemname, int operand);
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
	public void importItems(String text, boolean lowercase);
	public void indexObject(
		long oid,
		String text,
		boolean createItems,
		boolean lowercase);
	public void startSpider();
	public void validate();
	public void repair();
}