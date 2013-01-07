package de.pk86.bf.pl;

import java.io.Serializable;
import java.util.BitSet;

import de.jdataset.JDataRow;
import de.jdataset.JDataValue;

/**
 * Objekte diese Klassen halten den BitZaun für ein Item
 * @author peter
 */
public class Slot implements Serializable {
	private static final long serialVersionUID = 1L;

	public String itemname;

	private BitSet bitset;
	private boolean inserted = false;

	// Constructor
	public Slot(String itemname) {
		this.itemname = itemname;
		this.bitset = new BitSet();
		this.inserted = true;
	}

	public Slot(String itemname, byte[] bitFence) {
		this.itemname = itemname;
		this.bitset = BitSet.valueOf(bitFence);
	}

	public Slot(String itemname, JDataRow row) {
		this.itemname = itemname;
		JDataValue val = row.getDataValue("bits");
		Object oval = val.getObjectValue();
		if (oval == null) {
			throw new IllegalArgumentException("Slot bitfence null");
		}
		this.bitset = BitSet.valueOf((byte[]) oval);
	}
	
	public BitSet getBitset() {
		return bitset;
	}
	public byte[] getBytes() {
		return bitset.toByteArray();
	}
	
	public void setBitset(BitSet bitset) {
		this.bitset = bitset;
	}

	boolean isInserted() {
		return inserted;
	}

	/**
	 * Setzt ein Bit
	 * 
	 * @param l
	 *           Eine Zahl zwischen 0 und 1024*8 -1
	 * @return boolean Wenn true, dann wurde das Bit wirklich gesetzt
	 */
	void setBit(long l) {
		bitset.set((int)l);
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

	/**
	 * Wenn das letzte Bit gelöscht wird, dann auch den Slot löschen
	 * 
	 * @param l
	 * @return boolean Wenn true, dann wurde das Bit wirklich gelöscht
	 */
	void removeBit(long l) {
		bitset.set((int)l, false);
	}

	public int countBits() {
		return bitset.cardinality();
		//return bitCount;
	}

	
	public Slot clone() {
		Slot clone = new Slot(this.itemname);
		clone.inserted = true;
		clone.bitset = (BitSet)bitset.clone();
		
		return clone;
	}
}
