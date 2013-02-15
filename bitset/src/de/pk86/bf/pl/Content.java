package de.pk86.bf.pl;

import java.util.ArrayList;

import de.jdataset.JDataRow;
import de.jdataset.JDataSet;

/**
 * Domain-Object für Content 
 * @author peter
 */
public class Content {
	private JDataSet ds;
	private JDataRow row;
	
	public Content(JDataSet ds) {
		if (ds.getRowCount() != 1) {
			throw new IllegalArgumentException("Single DataRow DataSet expected");
		}
		this.ds = ds;
		this.row = ds.getRow();
	}
	public Content(JDataSet ds, JDataRow row) {
		if (ds.getRowCount() == 0) {
			throw new IllegalArgumentException("DataSet contains no rows");
		}
		// TODO: prüfen, ob Row Bestandteil des DataSet
		this.ds = ds;
		this.row = row;
	}
	public Content(JDataSet ds, int index) {
		this.ds = ds;
		this.row = ds.getChildRow(index);		
	}
	
	public JDataSet getDataSet() {
		return ds;
	}
	
	public JDataRow getDataRow() {
		return row;
	}
	
	public long getOid() {
		return row.getValueLong("oid");
	}
	
	public String getContent() {
		return row.getValue("content");
	}
	
	public void setContent(String s) {
		row.setValue("content", s);
	}
	
	public ArrayList<String> getItems() {
		String c = getContent();
		return BfPL.getObjectItems(c);
	}
}
