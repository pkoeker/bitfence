package de.pk86.bf.pl;

import java.util.BitSet;

import junit.framework.TestCase;

public class TestCompress extends TestCase {
	public void test1() {
		BfPL pl = BfPL.getInstance();
      Item slots;
      try {
	      slots = pl.getSlot("w");	      
	      perfSlots(slots);
	      slots = pl.getSlot("m");
	      perfSlots(slots);
	      slots = pl.getSlot("berlin");
	      perfSlots(slots);
	      slots = pl.getSlot("münchen");
	      perfSlots(slots);
	      slots = pl.getSlot("braunschweig");
	      perfSlots(slots);
	      slots = pl.getSlot("01");
	      perfSlots(slots);
	      slots = pl.getSlot("01.01.1900");
	      perfSlots(slots);
	      slots = pl.getSlot("10999");
	      perfSlots(slots);
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
