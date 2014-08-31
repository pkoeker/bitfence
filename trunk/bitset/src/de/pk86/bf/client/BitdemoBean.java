package de.pk86.bf.client;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import de.jdataset.JDataSet;
import de.pk86.bf.ExpressionResult;
import de.pk86.bf.ObjectItemServiceIF;

public class BitdemoBean implements Serializable {
   private static final long serialVersionUID = 1L;

	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BitdemoBean.class);
	
	private transient ObjectItemServiceIF sv;
	private String expression = "";
	private String page = "";
	private JDataSet currentPage;
	
	public BitdemoBean() {
		sv = ServiceFactory.getLocalService();
		logger.debug("new BitDemoBean created");
	}
	
//	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws Exception {
//			   request.setCharacterEncoding("UTF-8");
//			   chain.doFilter(request, response);
//	}

	
	public void processRequest(HttpServletRequest request) {
		try {
	      request.setCharacterEncoding("UTF-8");
      } catch (UnsupportedEncodingException e) {
	      e.printStackTrace();
      }
		String param = request.getParameter("action");
		if (param == null) {
			return;
		}
		logger.debug("Parameter: " + param); 
		logger.debug("Expression: " + expression);
		//System.out.println("processRequest: " + param);
		try {
			if (sv == null) {
				sv = ServiceFactory.getLocalService();
			}
			HttpSession ses = request.getSession(true);
			ExpressionResult res = (ExpressionResult)ses.getAttribute("ExpressionResult");
			if ("suchen".equalsIgnoreCase(param)) {
				if (res != null) { // Neu suchen: Ggf. bestehende Session zuvor beenden (die kann aber durch ein Timeout bereits beendet worden sein!)
					if (res.sel != null) {
						res.sel.reset();
					} else {
						boolean terminated = sv.endSession(res.sessionId);
					}
					res = null;
				}
				res = sv.execute(expression);	// throws RemoteException
				ses.setAttribute("ExpressionResult", res);
				if (res != null) {
					currentPage = res.firstPage;
					request.getSession().setAttribute("currentPage", currentPage);
					this.dispResult(request, currentPage, res.trace);
				} else {
					currentPage = null;
					request.getSession().setAttribute("currentPage", currentPage);
				}
			} else if (res != null) { 
				if ("weiter".equalsIgnoreCase(param)) {
					if (res.sel != null) {
						currentPage = res.sel.getNextPage();
					} else {
						currentPage = sv.getNextPage(res.sessionId);
					}
					request.getSession().setAttribute("currentPage", currentPage);
					res.pointer = (int)currentPage.getOid();
					this.dispResult(request, currentPage, null);
				} else if ("zurück".equalsIgnoreCase(param)) {
					if (res.sel != null) {
						currentPage = res.sel.getPrevPage();
					} else {
						currentPage = sv.getPrevPage(res.sessionId);
					}
					request.getSession().setAttribute("currentPage", currentPage);
					res.pointer = (int)currentPage.getOid();
					this.dispResult(request, currentPage, null);
				} else if ("statistic".equalsIgnoreCase(param)) {
					String cs = sv.getItemCacheStatistics();
					page = cs;
				} else if ("select".equalsIgnoreCase(param)) {
					res = sv.select(expression);
					if (res != null) {
						currentPage = res.firstPage;
						request.getSession().setAttribute("currentPage", currentPage);
						this.dispResult(request, currentPage, res.trace);
					} else {
						currentPage = null;
						request.getSession().setAttribute("currentPage", currentPage);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			page = ex.getMessage();
		}
	}
		
	public void setSearchPattern(String pattern) {
		this.expression = pattern.trim();
		System.out.println("setSearchPattern: " + pattern);
	}
		
	public String getSearchPattern() {
		logger.debug(expression);
		return expression;
	}
	
	public String getSearchResult() {		
		return page;
	}
	
	public String getDuration(HttpServletRequest request) {
		HttpSession ses = request.getSession();
		ExpressionResult res = (ExpressionResult)ses.getAttribute("ExpressionResult");
		if (res != null) {
			return res.getDuration();
		} else {
			return "";
		}
	}
	
	public String getResultSetSize(HttpServletRequest request) {
		HttpSession ses = request.getSession();
		ExpressionResult res = (ExpressionResult)ses.getAttribute("ExpressionResult");
		if (res != null) {
			return Integer.toString(res.resultsetSize);
		} else {
			return "";
		}
	}
	
	private void dispResult(HttpServletRequest request, JDataSet ds, String trace) {
		HttpSession ses = request.getSession();
		ExpressionResult res = (ExpressionResult)ses.getAttribute("ExpressionResult");
		String s = ExpressionResult.pageToString(ds);
		if (trace != null && trace.length() > 0) {
			s += "\nEntwicklung der Ergebnismenge:\n" + trace;
		}
		if (res.missingItems != null && res.missingItems.length() > 0) {
			s += "\nIgnorierte Suchbegriffe: " + res.missingItems;
		}
		page = s;
	}
	
	public String getPointer(HttpServletRequest request) {
		HttpSession ses = request.getSession();
		ExpressionResult res = (ExpressionResult)ses.getAttribute("ExpressionResult");
		if (res != null) {
			return Integer.toString(res.pointer);
		} else {
			return "";
		}
	}
}
