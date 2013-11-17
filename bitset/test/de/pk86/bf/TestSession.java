package de.pk86.bf;

import static org.junit.Assert.*;

import org.junit.Test;

import de.jdataset.JDataRow;
import de.jdataset.JDataSet;
import de.pk86.bf.pl.AllTest2;

public class TestSession {
	private ObjectItemServiceIF srv = AllTest2.getService();
	
	@Test public void testSession() {
		try {
	      ExpressionResult res = srv.execute("berlin | münchen");
	      int sessionId = res.sessionId;
	      boolean b = srv.hasSession(sessionId);
	      assertEquals(true, b);
	      JDataSet ds = srv.getFirstPage(sessionId);
	      if (srv.hasNext(sessionId)) {
	      	JDataSet dsn = srv.getNextPage(sessionId);
	      	JDataSet dsp = srv.getPrevPage(sessionId);
	      }
	      boolean end = srv.endSession(sessionId);
	      assertEquals(true, end);
      } catch (Exception e) {
	      e.printStackTrace();
	      fail(e.getMessage());
      }
	}
	@Test public void testSessionUpdate() {
		try {
	      ExpressionResult res = srv.execute("berlin | münchen");
	      int sessionId = res.sessionId;
	      boolean b = srv.hasSession(sessionId);
	      assertEquals(true, b);
	      JDataSet ds = srv.getFirstPage(sessionId);
	      for (int i = 0; i< ds.getRowCount(); i++) {
	      	JDataRow row = ds.getChildRow(i);
	      	switch (i) {
	      	case 0:
	      		row.setValue("content", row.getValue("content") + " update0");
	      		break;
	      	case 1:
	      		row.setValue("content", row.getValue("content") + " update1");
	      		break;
	      	case 2:
	      		row.setValue("content", "Karin Müller w 11.11.1955 10115 Berlin Am Pankepark 39");
	      		break;
	      	case 3:
	      		row.setDeleted(true);
	      		break;
	      	}
	      }
	      int cnt = srv.updateObjects(ds);
	      boolean end = srv.endSession(sessionId);
	      assertEquals(true, end);
      } catch (Exception e) {
	      e.printStackTrace();
	      fail(e.getMessage());
      }
	}
}
