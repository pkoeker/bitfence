package de.pk86.bf;

import junit.framework.TestCase;
import de.guibuilder.framework.GuiUtil;

public class TestExecute extends TestCase {
	public void test1() {
		ObjectItemService srv = new ObjectItemService();
		//long[] x = srv.execute("\"§ 100 a II. WoBauG\" | \"§ 1021 BGB\"");
		//boolean b = srv.hasItem("AGBG");
		ExpressionResult x = srv.execute("marco");
		x = srv.execute("freitag");
		x = srv.execute("marco freitag");
		x = srv.execute("marco freitag 01.07.1981");
		x = srv.execute("marco freitag  01.07.1981  08297");
		x = srv.execute("zwönitz marco freitag  01.07.1981  08297  ");
		x = srv.execute("marco freitag  01.07.1981  08297  zwönitz  mertensheide");
		x = srv.execute("marco freitag  01.07.1981  08297  zwönitz  mertensheide  2");
		boolean b = srv.hasItem("sonja");
		//System.out.println(x.objekts.length);
		if (!GuiUtil.yesNoMessage(null, "Return to continue", "Start")) {
			return;
		}
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			x = srv.execute("marco freitag 01.07.1981 08297 zwönitz mertensheide 2");
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
}
