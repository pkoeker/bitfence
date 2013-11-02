package de.pk86.bf.pl;

import org.junit.Test;
import static org.junit.Assert.*;

import de.pk86.bf.client.ObjectItemGui;

public class TestPL /*extends TestCase*/ {
	
//	public static void main(String[] args) {
//		TestPL me = new TestPL();
//   	if (!ObjectItemGui.yesNoMessage(null, "Return to continue", "Start")) {
//   		return;
//   	}
//		me.test0();
//		me.test1();
//		me.testItem1();
//   	if (!ObjectItemGui.yesNoMessage(null, "Return to continue", "End")) {
//   		return;
//   	}
//	}
	
	@Test public void test0() {
		BfPL pl = BfPL.getInstance();
		assertNotNull(pl);
	}
	@Test public void test1() {
//		BfPL pl = BfPL.getInstance();
//		long oid = pl.getMaxOid();
//		assertNotNull(oid);
	}
	@Test public void testItem1() {
		BfPL pl = BfPL.getInstance();
		try {
			String itemName = "xxx.xxx";
	      boolean yes = pl.hasItem(itemName);
	      if (yes) {
	      	int cntdel = pl.deleteItem(itemName);
	      	assertNotNull(cntdel);
	      }
	      int cntcrea = pl.createItem(itemName);
	      assertEquals(1, cntcrea);
	      String[] its = pl.findItems("xxx.xxx");
	      assertEquals(1, its.length);
      	int cntdel = pl.deleteItem(itemName);
      	assertNotNull(cntdel);
      	int anzi = pl.getItemCount("xxx.xxx");
      	assertEquals(0, anzi);
      } catch (Exception e) {      	
      	e.printStackTrace();
      	fail(e.getMessage());
      }
	}
		
	
	@Test public void testRenameItem() {
		BfPL pl = BfPL.getInstance();
		try {
			String itemName = "xxx.xxx";
	      boolean yes = pl.hasItem(itemName);
	      if (yes) {
	      	int cntdel = pl.deleteItem(itemName);
	      	assertNotNull(cntdel);
	      }
	      int cntcrea = pl.createItem(itemName);
	      int anz = pl.renameItem(itemName, "xxx.123");
      	assertNotNull(anz);
      	int cntdel = pl.deleteItem("xxx.123");
      	assertNotNull(cntdel);
	      
	   } catch (Exception e) {      	
	   	e.printStackTrace();
	   	fail(e.getMessage());
	   }
	}
	
	@Test public void testValid() {
//		BfPL pl = BfPL.getInstance();
//		try {
//			pl.validate();
//	   } catch (Exception e) {      	
//	   	e.printStackTrace();
//	   	fail(e.getMessage());
//	   }
	}
}
