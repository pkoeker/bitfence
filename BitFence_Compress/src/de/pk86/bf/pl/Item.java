package de.pk86.bf.pl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import de.pkjs.pl.IPLContext;

/**
 * TODO: Die Klassen Item und Slot vereinen
 * @author peter
 */
class Item {
	private static LinkedHashMap<String, Slot> hash = new LinkedHashMap<String, Slot>();
	private static BfPL pl = BfPL.getInstance();
	// Methods
	private static Slot findSlot(String itemname, boolean force, IPLContext ipl) throws Exception {
		Slot s = null;
		if (ipl == null) {
			s = hash.get(itemname); 
		} else {
			s = pl.selectSlot(itemname,  ipl);
		}
		if (force == true && s == null) {
			s = new Slot(itemname);
			hash.put(itemname, s);
		}
		return s;		
	}
	private static void writeSlot(Slot s, IPLContext ipl) throws Exception {
		if (ipl == null) {
			return;
		}
		if (s.isInserted() == true) {
			pl.insertSlot(s, ipl);
		} else {
			pl.updateSlot(s, ipl);
		}
	}
	static void setBit(long l, String itemname, IPLContext ipl) throws Exception {
		Slot s = Item.findSlot(itemname, true, ipl);
		s.setBit(l);
		writeSlot(s, ipl);
	}
	static boolean testBit(long l, String itemname, IPLContext ipl) throws Exception {
		Slot s = Item.findSlot(itemname, false, ipl);
		if (s == null) {
			return false;		
		} else {
			return s.testBit(l);
		}
	}
	static void removeBit(long l, String itemname, IPLContext ipl) throws Exception {
		Slot s = Item.findSlot(itemname, false, ipl);
		if (s == null) {
			return;		
		} else {
			s.removeBit(l);
			pl.updateSlot(s, ipl);
		}		
	}
	
	static void whiteAll(IPLContext ipl) throws Exception {
		Iterator<Map.Entry<String, Slot>> it = hash.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, Slot> entry = it.next();
			writeSlot(entry.getValue(), ipl);
		}
	}
}
