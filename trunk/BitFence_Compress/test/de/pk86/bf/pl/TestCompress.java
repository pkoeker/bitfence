package de.pk86.bf.pl;

import junit.framework.TestCase;
import de.pk86.bf.Selection;

public class TestCompress extends TestCase {
	public void test1() {
		BfPL pl = BfPL.getInstance();
      Slot slots;
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
	
	private void perfSlots(Slot slots) {
		try {
	      int[] ints = slots.getBits();
	      int cnt1 = Selection.countBitsSet(ints);
	      long start = System.currentTimeMillis();
	      byte[] bts = BfPL.intToByte(ints, true);
	      long end1 = System.currentTimeMillis();
	      System.out.println("Dura: " + (end1-start));
	      
	      int[] di = BfPL.byteToInt(bts, true);
	      long end2 = System.currentTimeMillis();
	      System.out.println("Dura: " + (end2-end1));
	      int cnt = Selection.countBitsSet(di);
	      System.out.println("Bits: " + slots.countBits() + ":" + cnt1 + "/" + cnt);
      } catch (Exception e) {
	      e.printStackTrace();
	      fail(e.getMessage());
      }
	}
}
