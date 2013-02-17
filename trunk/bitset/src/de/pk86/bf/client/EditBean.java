package de.pk86.bf.client;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import de.jdataset.JDataRow;
import de.jdataset.JDataSet;
import de.pk86.bf.ObjectItemServiceIF;

public class EditBean {
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EditBean.class);
	
	private ObjectItemServiceIF sv;
	private HttpServletRequest request;
	private JDataSet page;
	private long dura;
	private int anzahl;

	public EditBean() {
		sv = ServiceFactory.getDirectService();
		
	}
	public void processRequest(HttpServletRequest request) {
		anzahl = 0; dura = 0;
		this.request = request;
		Enumeration<String> en = request.getParameterNames();
		this.page = (JDataSet)request.getSession().getAttribute("currentPage");
		String param = request.getParameter("action");
		if (param == null) {
			return;
		}
		logger.debug("Parameter: " + param);
		if ("speichern".equalsIgnoreCase(param)) {
			for (int i = 0; i < page.getRowCount(); i++) {
				JDataRow row = page.getChildRow(i);
				String del = request.getParameter("checked["+i+"]");
				if ("on".equalsIgnoreCase(del)) {
					row.setDeleted(true);
				}
				String cont = request.getParameter("content["+i+"]");
				row.setValue("content", cont.trim());
			}
			long start = System.currentTimeMillis();
			if (page.hasChanges()) {
				JDataSet dsChanges = page.getChanges();
				anzahl = sv.updateObjects(dsChanges);
				page.commitChanges();
			}
			long end = System.currentTimeMillis();
			dura = end - start;
		}

	}
	public JDataSet getPage() {
		return page;
	}
	
	public String getSubmitResult() {
		if (anzahl != 0 && dura != 0) {
			return " Anzahl Änderungen: " + anzahl + " Dauer[ms]: " + dura;
		} else {
			return "";
		}
	}
}
