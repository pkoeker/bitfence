package de.pk86.bf.pl;

import junit.framework.TestCase;

public class TestRepair extends TestCase {
	private BfPL pl = AllTest2.getPL();

	public void testIndex() {
		try {
	      pl.repair(0,Integer.MAX_VALUE);
      } catch (Exception e) {
	      e.printStackTrace();
	      fail(e.getMessage());
      }
	}

}
