package de.pk86.bf.client;

import javax.servlet.http.HttpServletRequest;

import de.jdataset.JDataSet;
import de.pk86.bf.ExpressionResult;
import de.pk86.bf.ObjectItemServiceIF;

public class BitdemoBean {
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BitdemoBean.class);
	
	private ObjectItemServiceIF sv;
	private String expression = "";
	ExpressionResult res;
	private String page = "";
	private HttpServletRequest request;
	
	public BitdemoBean() {
		sv = ServiceFactory.getDirectService();
	}
	
	public void processRequest(HttpServletRequest request) {
		this.request = request;
		String param = request.getParameter("action");
		if (param == null) {
			return;
		}
		logger.debug("Parameter: " + param);
		logger.debug("Expression: " + expression);
		//System.out.println("processRequest: " + param);
		try {
			if ("suchen".equalsIgnoreCase(param)) {
				res = sv.execute(expression);			
				if (res != null) {
					this.dispResult(res.firstPage, res.trace);
				}
			} else if ("weiter".equalsIgnoreCase(param)) {
				JDataSet ds = sv.getNextPage(res.sessionName);
				this.res.pointer = (int)ds.getOid();
				this.dispResult(ds, null);
			} else if ("zurück".equalsIgnoreCase(param)) {
				JDataSet ds = sv.getPrevPage(res.sessionName);
				this.res.pointer = (int)ds.getOid();
				this.dispResult(ds, null);
			}
		} catch (IllegalStateException ex) {
			page = ex.getMessage();
		}
	}
		
	public void setSearchPattern(String pattern) {
		this.expression = pattern.trim();
		String param = request.getParameter("action");
		logger.debug("Parameter: " + param);
		logger.debug("Expression: " + expression);
		//System.out.println("setSearchPattern: " + param);
	}
		
	public String getSearchPattern() {
		return expression;
	}
	
	public String getSearchResult() {		
		return page;
	}
	
	public String getDuration() {
		if (res != null) {
			return res.getDuration();
		} else {
			return "";
		}
	}
	
	public String getResultSetSize() {
		if (res != null) {
			return Integer.toString(res.resultsetSize);
		} else {
			return "";
		}
	}
	
	private void dispResult(JDataSet ds, String trace) {
		String s = ExpressionResult.pageToString(ds);
		if (trace != null && trace.length() > 0) {
			s += "\nEntwicklung der Ergebnismenge:\n" + trace;
		}
		if (res.missingItems != null && res.missingItems.length() > 0) {
			s += "\nIgnorierte Suchbegriffe: " + res.missingItems;
		}
		page = s;
	}
	
	public String getPointer() {
		if (res != null) {
			return Integer.toString(res.pointer);
		} else {
			return "";
		}
	}
}
