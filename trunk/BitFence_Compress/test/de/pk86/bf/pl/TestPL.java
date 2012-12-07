package de.pk86.bf.pl;

import junit.framework.TestCase;
import de.pk86.bf.client.ObjectItemGui;

public class TestPL extends TestCase {
	
	public static void main(String[] args) {
		TestPL me = new TestPL();
   	if (!ObjectItemGui.yesNoMessage(null, "Return to continue", "Start")) {
   		return;
   	}
		me.test0();
		me.test1();
		me.testItem1();
   	if (!ObjectItemGui.yesNoMessage(null, "Return to continue", "End")) {
   		return;
   	}
	}
	
	public void test0() {
		BfPL pl = BfPL.getInstance();
		assertNotNull(pl);
	}
	public void test1() {
		BfPL pl = BfPL.getInstance();
		long oid = pl.getMaxOid();
		assertNotNull(oid);
	}
	
	public void testItem1() {
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
	
	public void testObject1() {
		BfPL pl = BfPL.getInstance();
		try {
			long oid = pl.getOid();
			pl.createObject(oid);
			int cnt = pl.deleteObject(oid);
			assertEquals(1,cnt);
      } catch (Exception e) {      	
      	e.printStackTrace();
      	fail(e.getMessage());
      }
	}
	public void testObjectItem1() {
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
			long oid = pl.getOid();
			pl.createObject(oid);
			pl.addObjectItem(oid, "xxx.xxx");
      	int anzi = pl.getItemCount("xxx.xxx");
      	assertEquals(1, anzi); 
      	String[] sItems = pl.getObjectItems(oid);
      	assertEquals("xxx.xxx", sItems[0]);
			pl.removeObjectItem(oid, "xxx.xxx");
      	anzi = pl.getItemCount("xxx.xxx");
      	assertEquals(0, anzi);
			int cnt = pl.deleteObject(oid);
			assertEquals(1,cnt);
			
      	int cntdel = pl.deleteItem(itemName);
      	assertNotNull(cntdel);
      	int anzj = pl.getItemCount("xxx.xxx");
      	assertEquals(0, anzj);

      } catch (Exception e) {      	
      	e.printStackTrace();
      	fail(e.getMessage());
      }
	}
	
	public void testObjectItem2() {		
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
			itemName = "xxx.yyy";
	      yes = pl.hasItem(itemName);
	      if (yes) {
	      	int cntdel = pl.deleteItem(itemName);
	      	assertNotNull(cntdel);
	      }
	      cntcrea = pl.createItem(itemName);	      
	      assertEquals(1, cntcrea);
	      
			long oid = pl.getOid();
			pl.createObject(oid);
			pl.addObjectItem(oid, "xxx.xxx");
      	int anzi = pl.getItemCount("xxx.xxx");
      	assertEquals(1, anzi); 
			pl.addObjectItem(oid, "xxx.yyy");
      	anzi = pl.getItemCount("xxx.yyy");
      	assertEquals(1, anzi); 
      	
      	String[] its = pl.findItems("xxx.%");
      	assertEquals(2, its.length);
      	
      	String[] sItems = pl.getObjectItems(oid);
      	assertEquals("xxx.xxx", sItems[0]);
      	assertEquals("xxx.yyy", sItems[1]);
			pl.removeObjectItem(oid, "xxx.xxx");
      	anzi = pl.getItemCount("xxx.xxx");
      	assertEquals(0, anzi);
			pl.removeObjectItem(oid, "xxx.yyy");
      	anzi = pl.getItemCount("xxx.yyy");
      	assertEquals(0, anzi);

      	int cnt = pl.deleteObject(oid);
			assertEquals(1,cnt);
			
      	int cntdel = pl.deleteItem("xxx.xxx");
      	assertNotNull(cntdel);
      	int anzj = pl.getItemCount("xxx.xxx");
      	assertEquals(0, anzj);
      	
      	cntdel = pl.deleteItem("xxx.yyy");
      	assertNotNull(cntdel);
      	anzj = pl.getItemCount("xxx.yyy");
      	assertEquals(0, anzj);

      } catch (Exception e) {      	
      	e.printStackTrace();
      	fail(e.getMessage());
      }
	}
	public void testObjectItem3() {
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
			long oid1 = pl.getOid();
			pl.createObject(oid1);
			pl.addObjectItem(oid1, "xxx.xxx");
	   	int anzi = pl.getItemCount("xxx.xxx");
	   	assertEquals(1, anzi); 
	   	String[] sItems = pl.getObjectItems(oid1);
	   	assertEquals("xxx.xxx", sItems[0]);
	   	
			long oid2 = pl.getOid();
			pl.createObject(oid2);
			pl.addObjectItem(oid2, "xxx.xxx");
	   	anzi = pl.getItemCount("xxx.xxx");
	   	assertEquals(2, anzi); 
	   	sItems = pl.getObjectItems(oid2);
	   	assertEquals("xxx.xxx", sItems[0]);
	   	
			pl.removeObjectItem(oid1, "xxx.xxx");
	   	anzi = pl.getItemCount("xxx.xxx");
	   	assertEquals(1, anzi);
			int cnt = pl.deleteObject(oid1);
			assertEquals(1,cnt);
			
			pl.removeObjectItem(oid2, "xxx.xxx");
	   	anzi = pl.getItemCount("xxx.xxx");
	   	assertEquals(0, anzi);
			cnt = pl.deleteObject(oid2);
			assertEquals(1,cnt);
			
	   	int cntdel = pl.deleteItem(itemName);
	   	assertNotNull(cntdel);
	   	int anzj = pl.getItemCount("xxx.xxx");
	   	assertEquals(0, anzj);
	
	   } catch (Exception e) {      	
	   	e.printStackTrace();
	   	fail(e.getMessage());
	   }
	}
	
	public void testRenameItem() {
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
	
	public void testValid() {
//		BfPL pl = BfPL.getInstance();
//		try {
//			pl.validate();
//	   } catch (Exception e) {      	
//	   	e.printStackTrace();
//	   	fail(e.getMessage());
//	   }
	}
}
