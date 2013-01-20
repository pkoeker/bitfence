package de.pk86.bf.bitset;

import java.util.BitSet;

import junit.framework.TestCase;
import de.pk86.bf.pl.BfPL;
import de.pk86.bf.pl.Slot;

public class TestBitSet extends TestCase {
	public void test1() {
		BfPL pl = BfPL.getInstance();
		try {
	      Slot s1 = pl.getSlot("w");
	      long[] l1 = getOids(s1);
	      Slot s2 = pl.getSlot("berlin");
	      long[] l2 = getOids(s2);
	      BitSet b1 = new BitSet(200000);
	      BitSet b2 = new BitSet(200000);
	      for (int i = 0; i < l1.length; i++) {
	      	long l = l1[i];
	      	b1.set((int)l);
	      }
	      for (int i = 0; i < l2.length; i++) {
	      	long l = l2[i];
	      	b2.set((int)l);
	      }
	      b1.and(b2);
	      int cnt = b1.cardinality();
	      long[] la = b1.toLongArray();
	      byte[] ba = b1.toByteArray();
	      System.out.println(cnt);
      } catch (Exception e) {
	      e.printStackTrace();
	      fail(e.getMessage());
      }
	}
	
	
	private long[] getOids(Slot slot) {
		BitSet bs = slot.getBitset();
		long[] ret = new long[slot.countBits()];
		int poi = 0; // Pointer zum Array
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
			ret[poi] = i;
			poi++;
		}
//		for (int j = 0; j < Const.SLOT_INT; j++) {
//			int ii = arr[j];
//			if (ii != 0) {
//				for ( int k=0; k<32; k++ ) {
//					int erg = ii & 1;
//					if (erg == 1) {
//						long oid = j * 32 + k; 
//						ret[poi] = oid;
//						poi++;
//					}	 	
//					ii >>>= 1;
//				}	// Next k				
//			} // ii != 0
//		} // Next j
		// Ende
		return ret;
	}
}
