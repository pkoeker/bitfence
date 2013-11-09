package de.pk86.bf.pl;

import java.util.BitSet;

import junit.framework.TestCase;

public class TestCompress extends TestCase {
	public void test1() {
		BfPL pl = BfPL.getInstance();
      Item items;
      try {
	      items = pl.loadItem("w");	      
	      perfSlots(items);
	      items = pl.loadItem("m");
	      perfSlots(items);
	      items = pl.loadItem("berlin");
	      perfSlots(items);
	      items = pl.loadItem("münchen");
	      perfSlots(items);
	      items = pl.loadItem("braunschweig");
	      perfSlots(items);
	      items = pl.loadItem("01");
	      perfSlots(items);
	      items = pl.loadItem("01.01.1900");
	      perfSlots(items);
	      items = pl.loadItem("10999");
	      perfSlots(items);
      } catch (Exception e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
      }
	}
	
	private void perfSlots(Item slots) {
		try {
	      BitSet bs = slots.getBitset();
	      int cnt1 = bs.cardinality();
	      long start = System.currentTimeMillis();
//	      byte[] bts = BfPL.intToByte(ints, true);
//	      long end1 = System.currentTimeMillis();
//	      System.out.println("Dura: " + (end1-start));
//	      
//	      int[] di = BfPL.byteToInt(bts, true);
//	      long end2 = System.currentTimeMillis();
//	      System.out.println("Dura: " + (end2-end1));
//	      int cnt = Selection.countBitsSet(di);
//	      System.out.println("Bits: " + slots.countBits() + ":" + cnt1 + "/" + cnt);
      } catch (Exception e) {
	      e.printStackTrace();
	      fail(e.getMessage());
      }
	}
}
