package de.pk86.bf.client;

import java.rmi.RemoteException;

import javax.servlet.http.HttpServletRequest;

import de.jdataset.JDataSet;
import de.pk86.bf.ExpressionResult;
import de.pk86.bf.ObjectItemServiceIF;

public class BitdemoBean {
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BitdemoBean.class);
	
	private ObjectItemServiceIF sv;
	private String expression = "";
	private ExpressionResult res;
	private String page = "";
	private JDataSet currentPage;
	private HttpServletRequest request;
	
	public BitdemoBean() {
		sv = ServiceFactory.getDirectService();
		logger.debug("new BitDemoBean created");
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
				if (res != null) {
					sv.endSession(res.sessionId);
				}
				res = sv.execute(expression);	// throws RemoteException		
				if (res != null) {
					currentPage = res.firstPage;
					request.getSession().setAttribute("currentPage", currentPage);
					this.dispResult(currentPage, res.trace);
				} else {
					currentPage = null;
					request.getSession().setAttribute("currentPage", currentPage);
				}
			} else if (res != null) { 
				if ("weiter".equalsIgnoreCase(param)) {
					currentPage = sv.getNextPage(res.sessionId);
					request.getSession().setAttribute("currentPage", currentPage);
					this.res.pointer = (int)currentPage.getOid();
					this.dispResult(currentPage, null);
				} else if ("zurück".equalsIgnoreCase(param)) {
					currentPage = sv.getPrevPage(res.sessionId);
					request.getSession().setAttribute("currentPage", currentPage);
					this.res.pointer = (int)currentPage.getOid();
					this.dispResult(currentPage, null);
				}
			}
		} catch (RemoteException ex) {
			page = ex.getMessage();
		}
	}
		
	public void setSearchPattern(String pattern) {
		this.expression = pattern.trim();
		if (request == null) return;
		logger.debug("Expression: " + expression);
		String param = request.getParameter("action");
		logger.debug("Parameter: " + param);
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
