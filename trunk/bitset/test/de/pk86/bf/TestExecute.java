package de.pk86.bf;

import static org.junit.Assert.fail;

import java.rmi.RemoteException;
import java.util.Map;

import org.junit.Test;

import de.guibuilder.framework.GuiUtil;
import de.pk86.bf.pl.AllTest2;


public class TestExecute  {
	private ObjectItemServiceIF srv = AllTest2.getService();
	
	@Test public void test1() {
		//long[] x = srv.execute("\"§ 100 a II. WoBauG\" | \"§ 1021 BGB\"");
		//boolean b = srv.hasItem("AGBG");
		ExpressionResult x;
      try {
	      x = srv.execute("hans");
	      x = srv.execute("müller");
	      x = srv.execute("Hans Müller");
	      x = srv.execute("Hans Müller 21.12.1966");
	      x = srv.execute("Hans Müller m 21.12.1966");
	      x = srv.execute("Hans Müller m 21.12.1966 Müllerstraße 13");
	      x = srv.execute("Hans Müller m 21.12.1966 Müllerstraße 13 10999 Berlin");
	      x = srv.execute("Maria Meier w 22.11.1977");
      } catch (RemoteException e) {
	      e.printStackTrace();
	      fail(e.getMessage());
      }
		boolean b = srv.hasItem("sonja");
		//System.out.println(x.objekts.length);
		if (!GuiUtil.yesNoMessage(null, "Return to continue", "Start")) {
			return;
		}
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			try {
	         x = srv.execute("Hans Müller m 21.12.1966 Müllerstraße 13 10999 Berlin");
	         srv.endSession(x.sessionId);
         } catch (RemoteException e) {
	         e.printStackTrace();
		      fail(e.getMessage());
         }
			//long[] oid = srv.execute("\"§ 100 a II. WoBauG\"");
   		//b = srv.hasItem("AGBG");
			//System.out.println(oid.length);
		}
		long end = System.currentTimeMillis();
		System.out.println("Execute duration: " + (end-start));
   	if (!GuiUtil.yesNoMessage(null, "Return to continue", "End")) {
   		return;
   	}
	}
	@Test public void test2() {
		ExpressionResult x;
      try {
	      x = srv.execute("berlin");
	      String p = x.getFirstPage();
      } catch (RemoteException e) {
	      e.printStackTrace();
	      fail(e.getMessage());
      }
	}
	@Test public void test3() {
		ExpressionResult x;
      try {
	      x = srv.execute("(berlin | münchen) hans");
	      String p = x.getFirstPage();
	      Map<String,Integer> map = srv.getOtherItems(x.sessionId);
	      System.out.println("OtherItems:" + map.size());
      } catch (RemoteException e) {
	      e.printStackTrace();
	      fail(e.getMessage());
      }
	}
}
