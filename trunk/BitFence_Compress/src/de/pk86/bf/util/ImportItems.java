package de.pk86.bf.util;

import java.util.HashSet;
import java.util.StringTokenizer;

import junit.framework.TestCase;
import de.guibuilder.framework.GuiUtil;
import de.jdataset.JDataRow;
import de.jdataset.JDataSet;
import de.pkjs.pl.PL;
import de.pkjs.pl.PLException;

public class ImportItems extends TestCase {
	public static void main(String[] args) {
		
	}
	
	public void testImport1() {
		HashSet<String> hs = new HashSet<String>(10000);
		this.importFile("resources/1_firstnames.csv", ImportMain.FIRSTNAMES_DELIM, hs);
		this.importFile("resources/2_lastnames.csv", ImportMain.LASTNAME_DELIM, hs);
		this.importFile("resources/3_birthdate.csv", ImportMain.BIRTHDATE_DELIM, hs);
		this.importFile("resources/4_zip_city.csv", ImportMain.ZIP_CITY_DELIM, hs);
		this.importFile("resources/5_street_number_ext.csv",ImportMain.STREET_NUMBER_DELIM, hs);
		long start = System.currentTimeMillis();
		PL pl = ImportMain.getPL();
		JDataSet ds = pl.getEmptyDataset("item");
		for(String item:hs) {
			JDataRow row = ds.createChildRow();
			row.setValue("itemname", item);
		}
		try {
	      pl.setDataset(ds);
			long end2 = System.currentTimeMillis();
			long dur2 = end2 - start;
			System.out.println("Anzahl: " + ds.getRowCount() + "  Dauer: " + dur2);
      } catch (PLException e) {
	      e.printStackTrace();
	      fail(e.getMessage());
      }
	}
	
	private void importFile(String filename, String delim, HashSet<String> hs) {
		System.out.println(filename);
		long start = System.currentTimeMillis();
		PL pl = ImportMain.getPL();
		String s = GuiUtil.fileToString(filename);
		StringTokenizer toks = new StringTokenizer(s, "\n\r");
		while (toks.hasMoreTokens()) {
			String tok = toks.nextToken();
			StringTokenizer toks2 = new StringTokenizer(tok, delim);
			while (toks2.hasMoreTokens()) {
				String si = toks2.nextToken();
				if (si.length() > 0) {
					boolean add = hs.add(si.toLowerCase());
				}
			}
		}
		long end = System.currentTimeMillis();
		long dur1 = end - start;
		System.out.println(dur1);
	}
}
