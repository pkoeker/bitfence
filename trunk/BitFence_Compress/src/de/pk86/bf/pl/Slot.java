package de.pk86.bf.pl;

import java.io.Serializable;

import de.jdataset.JDataRow;
import de.jdataset.JDataValue;
import de.pk86.bf.Const;
import de.pk86.bf.Selection;

/**
 * Objekte diese Klassen halten den BitZaun für ein Item
 * @author peter
 */
public class Slot implements Serializable {
	private static final long serialVersionUID = 1L;

	public String itemname;

	private int bitCount; // geht schneller!
	private int[] fence = new int[Const.SLOT_INT];
	private boolean inserted = false;

	// Constructor
	public Slot(String itemname) {
		this.itemname = itemname;
		this.inserted = true;
	}

	public Slot(String itemname, int[] bitFence, int count) {
		this.itemname = itemname;
		this.fence = bitFence;
		if (fence.length != Const.SLOT_INT) {
			throw new IllegalArgumentException("Illegal fence size: " + itemname + "/" + fence.length);
		}
		this.bitCount = count;
	}

	public Slot(String itemname, JDataRow row) {
		this.itemname = itemname;
		JDataValue val = row.getDataValue("bits");
		Object oval = val.getObjectValue();
		if (oval == null) {
			throw new IllegalArgumentException("Slot bitfence null");
		}
		this.fence = BfPL.byteToInt((byte[]) oval);
		if (fence.length != Const.SLOT_INT) {
			throw new IllegalArgumentException("Illegal fence size: " + itemname + "/" + fence.length);
		}
		this.bitCount = row.getValueInt("bitcount");
	}

	public int[] getBits() {
		return fence;
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
	boolean setBit(long l) {
		int index = (int) l >>> 5; // Index für Array errechen (Division durch 32)
		int bitNumber = (int) l % 32; // Position des Bits errechnen
		int mask = 1 << bitNumber; // ein Bit wird an die richtige Stelle
											// geschoben.
		int oldValue = fence[index]; // Den vorigen Wert merken
		fence[index] = fence[index] | mask; // Bit mit OR setzen.
		if (fence[index] != oldValue) {
			bitCount++;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Testet ob ein Bit gesetzt ist.
	 * 
	 * @param l
	 * @return boolean
	 */
	boolean testBit(long l) {
		int index = (int) l >>> 5; // Division durch 32
		int bitNumber = (int) l % 32;
		int erg = fence[index] >> bitNumber & 1;
		if (erg == 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Wenn das letzte Bit gelöscht wird, dann auch den Slot löschen
	 * 
	 * @param l
	 * @return boolean Wenn true, dann wurde das Bit wirklich gelöscht
	 */
	boolean removeBit(long l) {
		int index = (int) l >>> 5; // Division durch 32
		int bitNumber = (int) l % 32;
		int mask = 1 << bitNumber;
		int oldValue = fence[index];
		fence[index] = fence[index] ^ mask;
		if (fence[index] != oldValue) {
			bitCount--;
			return true;
		} else {
			return false;
		}
	}

	public int countBits() {
		return bitCount;
	}

	public void validate() {
		int cnt = Selection.countBitsSet(fence);
		if (cnt != bitCount) {
			throw new IllegalStateException("Slot #bits/bitCount mismatch: " + itemname);
		}
	}
}
