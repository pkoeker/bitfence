package de.pk86.bf.pl;

import java.io.Serializable;
import java.util.BitSet;

import de.jdataset.JDataRow;
import de.jdataset.JDataValue;

/**
 * Objekte diese Klassen halten den BitZaun für ein Item
 * @author peter
 */
public class Item implements Serializable {
	private static final long serialVersionUID = 1L;

	public String itemname;

	private BitSet bitset;
	private boolean inserted;
	private boolean modified;

	// Constructor
	public Item(String itemname) {
		this.itemname = itemname;
		this.bitset = new BitSet();
		this.inserted = true;
	}

	public Item(String itemname, byte[] bitFence) {
		this.itemname = itemname;
		this.bitset = BitSet.valueOf(bitFence);
	}

	public Item(String itemname, JDataRow row) {
		this.itemname = itemname;
		JDataValue val = row.getDataValue("bits");
		Object oval = val.getObjectValue();
		if (oval == null) {
			throw new IllegalArgumentException("Item BitSet null");
		}
		this.bitset = BitSet.valueOf((byte[]) oval);
	}
	
	public BitSet getBitset() {
		return bitset;
	}
	/**
	 * Für Persistenz des BitSet
	 * @return
	 */
	public byte[] getBytes() {
		return bitset.toByteArray();
	}
	
	public void setBitset(BitSet bitset) {
		this.bitset = bitset;
		//this.modified = true; // siehe #performBrace 
	}

	boolean isInserted() {
		return inserted;
	}
		
	boolean isModified() {
		return modified;
	}
	
	void setUpdated() {
		this.inserted = false;
		this.modified = false;
	}
	/**
	 * Setzt ein Bit
	 * 
	 * @param l
	 *           Eine Zahl zwischen 0 und 1024*8 -1
	 * @return boolean Wenn true, dann wurde das Bit wirklich gesetzt
	 */
	void setBit(long l) {
		synchronized(bitset) {
			bitset.set((int)l);
		}
		this.modified = true; // TODO: prüfen, ob Bit schon zuvor gesetzt war?
	}

	/**
	 * Testet ob ein Bit gesetzt ist.
	 * 
	 * @param l
	 * @return boolean
	 */
	boolean testBit(long l) {
		boolean b = bitset.get((int)l);
		return b;
	}

	void removeBit(long l) {
		synchronized(bitset) {
			bitset.set((int)l, false);
		}
		this.modified = true; // TODO: prüfen, ob Bit schon zuvor gelöscht war?
	}

	public int countBits() {
		return bitset.cardinality();
	}
	
	public Item clone() {
		Item clone = new Item(this.itemname);
		clone.inserted = true;
		synchronized(bitset) {
			clone.bitset = (BitSet)bitset.clone();
		}		
		return clone;
	}
	
	public String toString() {
		return itemname + " [" + countBits() + "]";
	}
}
