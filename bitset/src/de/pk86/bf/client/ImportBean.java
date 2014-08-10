package de.pk86.bf.client;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import de.pk86.bf.ObjectItemServiceIF;

public class ImportBean implements Serializable {
   private static final long serialVersionUID = 1L;

	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ImportBean.class);
	
	private transient ObjectItemServiceIF sv;
	private String data = "";
	private long dura;
	private int anzahl;
	//private HttpServletRequest request;
	
	public ImportBean() {
		sv = ServiceFactory.getDirectService();
	}
	
	public void processRequest(HttpServletRequest request) {
		String param = request.getParameter("action");
		if (param == null) {
			return;
		}
		logger.debug("Parameter: " + param);
		if ("importieren".equalsIgnoreCase(param) && data.length() > 0) {
			long start = System.currentTimeMillis();
			if (sv == null) {
				sv = ServiceFactory.getDirectService();
			}
			anzahl = sv.importDatabaseCSV(data);
			long end = System.currentTimeMillis();
			dura = end - start;
		}
	}
	
	public void setImportData(String data) {
		//if (request == null) return;
		this.data = data.trim();

	}
	
	public String getImportData() {
		return data;
	}
	public int getDataSize() {
		return anzahl;
	}
	public long getDuration() {
		return dura;
	}
}
