package de.pk86.bf.pl;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestObjekt {
	private BfPL pl = AllTest2.getPL();

	@Test public void createObjekt() {
		try {
	      int anzHans0 = pl.getItemCount("hans");
	      long oid = pl.createObject("Hans Müller m Müllerstraße 13 10123 Berlin");
	      if (oid <= 0) {
	      	fail("oid <= 0");
	      }
	      boolean b = pl.hasItem("hans");
	      int anzHans1 = pl.getItemCount("hans");
	      assertEquals(anzHans0 +1, anzHans1);
	      assertEquals(true, b);
	      b = pl.hasItem("müller");
	      assertEquals(true, b);
	      b = pl.hasItem("m");
	      assertEquals(true, b);
	      b = pl.hasItem("müllerstraße");
	      assertEquals(true, b);
	      b = pl.hasItem("13");
	      assertEquals(true, b);
	      b = pl.hasItem("10123");
	      assertEquals(true, b);
	      b = pl.hasItem("berlin");
	      assertEquals(true, b);
	      boolean deleted = pl.deleteObject(oid);
	      assertEquals(true, deleted);
	      int anzHans2 = pl.getItemCount("hans");
	      assertEquals(anzHans0, anzHans2);
      } catch (Exception e) {
	      e.printStackTrace();
	      fail(e.getMessage());
      }
	}
}
