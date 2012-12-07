package de.pk86.bf;

import de.pk86.bf.pl.BfPL;

public class Const {
	/**
	 * Größe des Datenbank-Slots in Bytes (1024)
	 */
	public static final int SLOT_BYTES = 1024 * BfPL.maxSlot; 
	/**
	 * Anzahl Bits je Slot (8192) // 106.496
	 */
	public static final int SLOT_BITS = SLOT_BYTES * 8;
	/**
	 * Größe des Integer-Arrays im Slot (256)
	 */
	public static final int SLOT_INT =  SLOT_BYTES / 4;
	/*
	public static final int MAX_SLOT = 10;
	public static final int MAX_OID = SLOT_BITS * MAX_SLOT -1;
	*/

}
