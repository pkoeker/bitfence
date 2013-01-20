package de.pk86.bf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.jdataset.JDataRow;
import de.jdataset.JDataSet;

public class ExpressionResult implements Serializable {
   private static final long serialVersionUID = 1L;
   
   public String sessionName;
   
   public String expression;
   public String missingItems;
   public long[] objekts; // nur die erste Page
   public int resultsetSize;
   public long duraDB1;
   public long duraAlg;
   public long duraDB2;
   public long duraNet;
   public int pointer;
   public JDataSet firstPage;
   
   public String trace;
   
   /**
    * @deprecated nur für SOAP
    */
   public ExpressionResult() {
   	
   }
   
   public ExpressionResult(String name) {
   	this.sessionName = name;
   }
   
   public String getFirstPage() {
   	String s = pageToString(firstPage);
   	if (missingItems != null && missingItems.length() > 0) {
   		s += "\nUngültige Suchbegriffe: " + missingItems;
   	}
   	return s;
   }
   
   public static String pageToString(JDataSet ds) {
   	if (ds == null || ds.getRowCount() == 0) {
   		return "<null>";
   	}
   	Iterator<JDataRow> it = ds.getChildRows();
   	StringBuilder sb = new StringBuilder();
   	while (it.hasNext()) {
   		JDataRow row = it.next();
   		sb.append("[" + row.getValue("oid") +"] ");
   		sb.append(row.getValue("content"));
   		sb.append('\n');
   	}
   	return sb.toString();
   }
   
   public String getDuration() {
   	String s = duraDB1 + "+" + duraAlg + "+" + duraDB2;
   	if (duraNet > 0) {
   		s+= "+"+duraNet;
   	}
   	s += " = " + (duraDB1 + duraAlg + duraDB2 + duraNet);
   	return s;
   }
   
}
