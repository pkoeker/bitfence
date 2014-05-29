package de.pk86.bf;

import java.io.Serializable;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import de.jdataset.DataSetFactory;
import de.jdataset.JDataRow;
import de.jdataset.JDataSet;

public class ExpressionResult implements Serializable {
   private static final long serialVersionUID = 1L;
   
   public int sessionId;
   
   public String expression;
   public String missingItems;

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
   public ExpressionResult() { }
   
   public ExpressionResult(int sessionId) {
   	this.sessionId = sessionId;
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
   		sb.append("[" + row.getValue("obid") +"] ");
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
   
   public JSONObject getJSONResult() {
   	JSONObject jo = new JSONObject();
   	jo.put("SessionId", sessionId);
   	jo.put("ResultSetSize", resultsetSize);
   	jo.put("ElapsedTime", getDuration());
   	if (missingItems != null) {
   		jo.put("MissingItems", missingItems);
   	}
   	JSONArray ja = DataSetFactory.toJSONArray(firstPage.getChildRows());
   	jo.put("ResultSetFirstPage", ja);
   	return jo;   	
   }
   
}
