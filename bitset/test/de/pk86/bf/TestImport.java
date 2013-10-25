package de.pk86.bf;

import org.junit.Test;



public class TestImport  {
	private ObjectItemService sv = new ObjectItemService();
	@Test
	public void test1() {
		String data = "Hans Müller m Müllerstraße 13 10999 Berlin\n" +
				" Maria Meier w Hans Sommer Straße 14 10123 Berlin";
		int anz = sv.importDatabaseCSV(data);
	}
}

