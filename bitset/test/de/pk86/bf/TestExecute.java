package de.pk86.bf;

import static org.junit.Assert.*;

import java.rmi.RemoteException;

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
	      x = srv.execute("marco");
	      x = srv.execute("freitag");
	      x = srv.execute("marco freitag");
	      x = srv.execute("marco freitag 01.07.1981");
	      x = srv.execute("marco freitag  01.07.1981  08297");
	      x = srv.execute("zwönitz marco freitag  01.07.1981  08297  ");
	      x = srv.execute("marco freitag  01.07.1981  08297  zwönitz  mertensheide");
	      x = srv.execute("marco freitag  01.07.1981  08297  zwönitz  mertensheide  2");
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
	         x = srv.execute("marco freitag 01.07.1981 08297 zwönitz mertensheide 2");
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
		System.out.println(end-start);
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
}
