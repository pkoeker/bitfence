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
	 *          
	 * @return boolean Wenn true, dann wurde das Bit wirklich gesetzt
	 */
	void setBit(long l) {
		int i = (int)l;
		synchronized(bitset) {
			modified = !bitset.get(i);
			bitset.set(i);
		}
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
		int i = (int)l;
		synchronized(bitset) {
			modified = bitset.get(i);
			bitset.set((int)l, false);
		}
	}

	public int countBits() {
		return bitset.cardinality();
	}
	
	/**
	 * Liefert die Menge der aller Objekt-IDs zu diesem Item
	 * @return
	 */
	public int[] getOids() {
		int[] ret = new int[this.countBits()];
		BitSet bs = this.getBitset();
		int poi = 0;
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
			ret[poi] = i;
			poi++;
		}
		return ret;
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
