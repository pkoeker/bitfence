package de.pk86.bf.util;

import java.util.ArrayList;
import java.util.StringTokenizer;

import junit.framework.TestCase;
import de.guibuilder.framework.GuiUtil;
import de.jdataset.JDataRow;
import de.jdataset.JDataSet;
import de.pkjs.pl.PL;
import de.pkjs.pl.PLException;

/**
 * Generiert Zufallsobjekte aus Vornamen, Nachname, Geschlecht, Geburtsdatum PLZ+Ort, Straße+Hausnummer 
 * @author peter
 */
public class ImportObjects extends TestCase {
	private static PL pl;
	static String FIRSTNAMES_DELIM = " -/.'";
	static String LASTNAME_DELIM = " -/.'";
	static String BIRTHDATE_DELIM = " ";
	static String ZIP_CITY_DELIM = " ;()/-";
	static String STREET_NUMBER_DELIM = " ;()/-'";

	static PL getPL() {
		if (pl == null) {
	      try {	      	
		      //pl = new PL("BitDemoPLConfig.xml");
		      pl = new PL("BF_PLConfig.xml");
	      } catch (Exception e) {
		      e.printStackTrace();
		      throw new IllegalStateException(e);
	      }
		}
      return pl;
	}

	public void testImport1() {
		long start = System.currentTimeMillis();
		// Firstnames
		ArrayList<String> al1 = new ArrayList<String>(50000);
		{
			String s = GuiUtil.fileToString("resources/1_firstnames.csv");
			StringTokenizer toks = new StringTokenizer(s, "\n\r");
			while (toks.hasMoreTokens()) {
				String tok = toks.nextToken();
				al1.add(tok);
			}
		}
		// Lastnames
		ArrayList<String> al2 = new ArrayList<String>(50000);
		{
			String s = GuiUtil.fileToString("resources/2_lastnames.csv");
			StringTokenizer toks = new StringTokenizer(s, "\n\r");
			while (toks.hasMoreTokens()) {
				String tok = toks.nextToken();
				al2.add(tok);
			}
		}
		// Bithday
		ArrayList<String> al3 = new ArrayList<String>(50000);
		{
			String s = GuiUtil.fileToString("resources/3_birthdate.csv");
			StringTokenizer toks = new StringTokenizer(s, "\n\r");
			while (toks.hasMoreTokens()) {
				String tok = toks.nextToken();
				al3.add(tok);
			}
		}
		// PLZ_Ort
		ArrayList<String> al4 = new ArrayList<String>(50000);
		{
			String s = GuiUtil.fileToString("resources/4_zip_city.csv");
			StringTokenizer toks = new StringTokenizer(s, "\n\r");
			while (toks.hasMoreTokens()) {
				String tok = toks.nextToken();
				al4.add(tok);
			}
		}
		// Straße 
		ArrayList<String> al5 = new ArrayList<String>(50000);
		{
			String s = GuiUtil.fileToString("resources/5_street_number_ext.csv");
			StringTokenizer toks = new StringTokenizer(s, "\n\r");
			while (toks.hasMoreTokens()) {
				String tok = toks.nextToken();
				al5.add(tok);
			}
		}
		// Straße nr, zusatz
		ArrayList<String> al6 = new ArrayList<String>(500);
		{
			String s = GuiUtil.fileToString("resources/6_Hausnummer.csv");
			StringTokenizer toks = new StringTokenizer(s, "\n\r");
			while (toks.hasMoreTokens()) {
				String tok = toks.nextToken();
				al6.add(tok);
			}
		}
		long end1 = System.currentTimeMillis();
		PL pl = getPL();
		JDataSet ds = pl.getEmptyDataset("objekt");
		
		for (int i = 0; i < 1000000; i++) {
			StringBuilder sb = new StringBuilder();			
			double r1 = Math.random();
			double d1 = r1 * al1.size();
			long i1 = Math.round(d1)-1;
			if (i1 == -1) i1 = 0;
			String s1 = al1.get((int)i1); 
			sb.append(s1);
			sb.append(" ");

			double r2 = Math.random();
			double d2 = r2 * al2.size();
			long i2 = Math.round(d2)-1;
			if (i2 == -1) i2 = 0;
			String s2 = al2.get((int)i2); 
			sb.append(s2);
			sb.append(" ");

			// Geschlecht
			String gend;
			if (i % 2 == 0) {
				gend = "m";
			} else {
				gend = "w";
			}
			sb.append(gend + " ");
			

			double r3 = Math.random();
			double d3 = r3 * al3.size();
			long i3 = Math.round(d3)-1;
			if (i3 == -1) i3 = 0;
			String s3 = al3.get((int)i3); 
			sb.append(s3);
			sb.append(" ");
			// PLZ Ort
			double r4 = Math.random();
			double d4 = r4 * al4.size();
			long i4 = Math.round(d4)-1;
			if (i4 == -1) i4 = 0;
			String s4 = al4.get((int)i4);
			sb.append(s4);
			sb.append(" ");

			// Straße 
			double r5 = Math.random();
			double d5 = r5 * al5.size();
			long i5 = Math.round(d5)-1;
			if (i5 == -1) i5 = 0;
			String s5 = al5.get((int)i5); 
			String[] s5a = s5.split(";");
			sb.append(s5a[0]);
			sb.append(" ");
			// Hausnummer Zusatz
			double r6 = Math.random();
			double d6 = r6 * al6.size();
			long i6 = Math.round(d6)-1;
			if (i6 == -1) i6 = 0;
			String s6 = al6.get((int)i6); 
			sb.append(s6);
			//sb.append(" ");
			
			JDataRow row = ds.createChildRow();
			row.setValue("obid", i);
			String s = sb.toString();
			s = s.replaceAll(";", " ");
			row.setValue("content", s);
			
		}
		long end2 = System.currentTimeMillis();
		long sz = ds.getSize();
		System.out.println(sz + "/" + end2);
//		try {
//			ArrayList<JDataSet> alds = new ArrayList<JDataSet>();
//			alds.add(ds);
//	      int anz = pl.setDataset(alds);
//			long end3 = System.currentTimeMillis();
//			System.out.println(anz + " Dauer: " + (end1-start) + "/" + (end2-end1) + "/" + (end3-end2));
//      } catch (PLException e) {
//	      e.printStackTrace();
//	      fail(e.getMessage());
//      }		
	}
}
