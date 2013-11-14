package de.pk86.bf;

import static org.junit.Assert.*;

import org.junit.Test;

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
}
