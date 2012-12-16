package de.pk86.bf.util;

import de.pkjs.pl.PL;

public class ImportMain {
	private static PL pl;
	static String FIRSTNAMES_DELIM = " -/.";
	static String LASTNAME_DELIM = " -/.";
	static String BIRTHDATE_DELIM = " ";
	static String ZIP_CITY_DELIM = " ;()/-";
	static String STREET_NUMBER_DELIM = " ;()/-";
	public static String DEFAULT_DELIM = " ;.,()/-";

	static PL getPL() {
		if (pl == null) {
	      try {	      	
		      pl = new PL("BitDemoPLConfig.xml");
	      } catch (Exception e) {
		      e.printStackTrace();
		      throw new IllegalStateException(e);
	      }
		}
      return pl;
	}
}
