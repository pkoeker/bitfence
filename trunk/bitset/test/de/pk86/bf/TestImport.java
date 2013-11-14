package de.pk86.bf;

import org.junit.*;

import de.pk86.bf.pl.AllTest2;
import static org.junit.Assert.*;


public class TestImport  {
	private ObjectItemServiceIF srv = AllTest2.getService();
	
	@Test public void test1() {
		String data = "Hans Müller m Müllerstraße 13 10999 Berlin\n" +
				" Maria Meier w Hans Sommer Straße 14 10123 Berlin";
		int anz = srv.importDatabaseCSV(data);
		assertEquals(2, anz);
	}
}

