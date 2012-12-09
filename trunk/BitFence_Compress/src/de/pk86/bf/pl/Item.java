package de.pk86.bf.pl;

import de.pkjs.pl.IPLContext;

/**
 * Eine Eigenschaft (eines Objekts)
 * @author peter
 */
class Item {
	private static BfPL pl = BfPL.getInstance();
	// Methods
	private static Slot findSlot(String itemname, boolean force, IPLContext ipl) throws Exception {
		Slot s = null;
		s = pl.selectSlot(itemname,  ipl);
		if (force == true && s == null) {
			s = new Slot(itemname);
		}
		return s;		
	}
	private static void writeSlot(Slot s, IPLContext ipl) throws Exception {
		if (s.isInserted() == true) {
			pl.insertSlot(s, ipl);
		} else {
			pl.updateSlot(s, ipl);
		}
	}
	static boolean setBit(long l, String itemname, IPLContext ipl) throws Exception {
		boolean ret = false;
		Slot s = Item.findSlot(itemname, true, ipl);
		ret = s.setBit(l);
		writeSlot(s, ipl);
		return ret;
	}
	static boolean testBit(long l, String itemname, IPLContext ipl) throws Exception {
		Slot s = Item.findSlot(itemname, false, ipl);
		if (s == null) {
			return false;		
		} else {
			return s.testBit(l);
		}
	}
	static boolean removeBit(long l, String itemname, IPLContext ipl) throws Exception {
		Slot s = Item.findSlot(itemname, false, ipl);
		if (s == null) {
			return false;		
		} else {
			boolean hasRemoved = s.removeBit(l);
			if (s.countBits() == 0) {
				pl.removeSlot(s, ipl);
			} else {
				pl.updateSlot(s, ipl);
			}
			return hasRemoved;
		}		
	}
}
