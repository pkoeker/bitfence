package de.pk86.bf;

import junit.framework.TestCase;

public class TestImport extends TestCase {
	private ObjectItemService sv = new ObjectItemService();

	public void test1() {
		String data = "Hans Müller m Müllerstraße 13 10999 Berlin\n" +
				" Maria Meier w Hans Sommer Straße 14 10123 Berlin";
		int anz = sv.importDatabaseCSV(data);
	}
}
