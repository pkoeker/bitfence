package de.pk86.bf.pl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class TestPL  {
	private BfPL pl = AllTest2.getPL();
		
	/**
	 * Item Anlegen, Item löschen
	 */
	@Test public void testItemCreateDelete() {
		try {
			String itemName = "xxx.xxx";
			// Löschen, falls vorhanden
	      boolean yes = pl.hasItem(itemName);
	      if (yes) {
	      	int cntdel = pl.deleteItem(itemName); 
	      	assertEquals(1,cntdel);
	      }
	      // fehlt jetzt
	      boolean b = pl.hasItem(itemName);
	      assertEquals(false, b);
	      // create
	      pl.createItem(itemName);
	      // ist jetzt vorhanden
	      b = pl.hasItem(itemName);
	      assertEquals(true, b);
	      //
	      String[] its = pl.findItems("xxx.xxx");
	      assertEquals(1, its.length);
	      // löschen
      	int cntdel = pl.deleteItem(itemName);
      	assertEquals(1,cntdel);
      	int anzi = pl.getItemCount("xxx.xxx");
      	// ist jetzt wieder weg
      	assertEquals(0, anzi);
	      b = pl.hasItem(itemName);
	      assertEquals(false, b);
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
	      pl.createItem(itemName);
	      int anz = pl.renameItem(itemName, "xxx.123");
      	assertNotNull(anz);
      	int cntdel = pl.deleteItem("xxx.123");
      	assertNotNull(cntdel);
	      
	   } catch (Exception e) {      	
	   	e.printStackTrace();
	   	fail(e.getMessage());
	   }
	}
		
	@Test public void testGetObject() {
		try {
			int anzObject = pl.getItemCount("berlin");
	      Item item = pl.loadItem("berlin");
	      int[] oids = item.getOids();
	      assertEquals(anzObject, oids.length);
	      String s = pl.getObjekts(oids);
      } catch (Exception e) {
	   	e.printStackTrace();
	   	fail(e.getMessage());
      }
	}
}
