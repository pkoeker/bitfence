package de.pk86.bf;

import java.util.ArrayList;
import java.util.Arrays;

import de.jdataset.JDataSet;
import de.pk86.bf.pl.BfPL;
import de.pk86.bf.pl.Slot;

/**
 * Hält den Zustand einer Schlagwortselektion
 */
public class Selection {
	private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Selection.class);

	public static enum Oper {NONE, AND, OR, XOR, NOT}; 
	private String name;
	private BfPL pl = BfPL.getInstance();
	private Slot slot;
	private int bitCount;
	private int calls;
	private long duration;
	private int posi; // Pointer im Resultset
	private ArrayList<String> items = new ArrayList<String>();
	private String missingItems = ""; 
	// Constructor
	Selection (String name) {
		this.name = name;
	}
	String getName() {
		return this.name;
	}
	
	int performOper(ArrayList<OperToken> al) {
		
		// Klammer
		while (al.size() > 1) {
			int maxLevel = 0;
			int startIndex = 0;
			for (int i = 0; i < al.size(); i++) {
				OperToken ot = al.get(i);
				if (ot.level > maxLevel) {
					maxLevel = ot.level;
					startIndex = i;
				}
			}			
			this.performBrace(al, startIndex, maxLevel);
		} 
		
		
//		for (OperToken ot:al) {
//			this.performOper(ot);
//		}
		this.slot = al.get(0).slot;
		int[] arr = slot.getBits();
		int ret = countBitsSet(arr);
		this.bitCount = ret;

		return ret;
	}
	
	private void performBrace(ArrayList<OperToken> al, int startIndex, int level) {
		int cnt = 0;
		for (int i = startIndex; i < al.size(); i++) {
			OperToken ot = al.get(i);
			if (ot.level == level) {
				cnt++;
				if (ot.brace == OperToken.Brace.CLOSE) {
					break;
				}
			}
		}
		if (cnt == 1) { // (a)
			al.get(startIndex).level--;
			return;
		}
		OperToken ot1 = al.get(startIndex);
		ot1.slot = ot1.slot.clone(); // HACK
		for (int i = startIndex+1; i < startIndex + cnt; i++) {
			OperToken ot2 = al.get(i);
			int[] erg = performOper(ot1.slot.getBits(), ot2.slot.getBits(), ot2.oper);
			ot1.slot.setBits(erg);
		}
		for (int i = startIndex + cnt -1; i>startIndex; i--) {
			al.remove(i);
		}
		ot1.level--; // keine Klammer mehr
		ot1.brace = OperToken.Brace.NONE;
	}
	/**
	 * Es wird ein Token mit einem Slot übergeben;
	 * die Ergebnismenge landet wieder im internen Slot dieser Session
	 * @param itemname
	 * @param oper
	 * @return Größe der Ergebnismenge
	 * @throws Exception
	 */
	int performOper(OperToken ot) {
		if (ot.slot == null) {
			logger.warn("Missing Slot: " + ot.token);
			missingItems += ot.token + " ";
			return 0;
		}
		items.add(ot.token);
		if (ot.oper == Oper.NONE || calls == 0) { // Der Erste
			this.slot = ot.slot.clone(); // Clone ist hier wichtig, sonst wird Cache vermüllt!
		} else {
				int[] arr1 = this.slot.getBits();
				int[] arr2 = ot.slot.getBits();
				this.slot.setBits(performOper(arr1, arr2, ot.oper));
		} 
		this.calls++;
		// Count bits
		int[] arr = slot.getBits();
		int ret = countBitsSet(arr);
		this.bitCount = ret;
		return ret;	
	}
	
	/**
	 * BitOperation ausführen
	 * Wenn arr1 == null, wird arr2 in Returnvalue umkopiert.
	 * @param arr1 Operand1
	 * @param arr2 Operand2
	 * @param oper Operator
	 * @return Ergebnis der Operation
	 */
	private static int[] performOper(int[] arr1, int[] arr2, Oper oper) {
		if (arr2 == null) {
			throw new IllegalArgumentException("Argument null");
		}
		int[] arrErg = new int[arr2.length];
		if (arr1 == null) {
			oper = Oper.NONE; // Kopieren
		} else {
			if (arr1.length != arr2.length) {
				throw new IllegalArgumentException("Array size mismatch: " + arr1.length + " != " + arr2.length);
			}
		}
		switch (oper) {
			case NONE: { 
				for (int j = 0; j < arr2.length; j++) {
					arrErg[j] = arr2[j];
				}
			}
				break;
			case AND: {
				for (int j = 0; j < arr2.length; j++) {
					arrErg[j] = arr1[j] & arr2[j];
				}
			}
				break;
			case OR: {
				for (int j = 0; j < arr2.length; j++) {
					arrErg[j] = arr1[j] | arr2[j];
				}
			}
				break;
			case XOR: {
				for (int j = 0; j < arr2.length; j++) {
					arrErg[j] = arr1[j] ^ arr2[j];
				}
			}
				break;
			case NOT: {
				for (int j = 0; j < arr2.length; j++) {
					arrErg[j] = arr1[j] & ~arr2[j]; // AND NOT
				}
			}
				break;
		} // End Switch
		return arrErg;
	}
	
	int getResultSetSize() {
		return bitCount;
	}
	long[] getResultSet() {
		if (this.bitCount > pl.getMaxResultSet()) {
			throw new IllegalStateException("Maximum ResultSet Size exceeded: " + Integer.toString(bitCount));
		}
		return getResult(0, bitCount);
	}
	long[] getResult(int start, int cnt) {
		if (start + cnt > bitCount) {
			cnt = bitCount - start;
		}
		long[] ret = new long[cnt];
		int poi = 0; // Pointer zum Array
			if (slot != null) {
				int[] arr = slot.getBits();
				for (int j = 0; j < Const.SLOT_INT; j++) {
					int ii = arr[j];
					if (ii != 0) {
						for ( int k=0; k<32; k++ ) {
							int erg = ii & 1;
							if (erg == 1) {
								if (poi >= start) {
									long oid = j * 32 + k; 
									ret[poi - start] = oid;
								} 	
								poi++;
								// Ende-Bedingung
								if (poi - start == cnt) {
									ret = Arrays.copyOf(ret, cnt);
									return ret;
								}
							}	 	
							ii >>>= 1;
						}	// Next k				
					} // ii != 0
				} // Next j
			} // Slot != null
		return ret;
	}
	boolean hasNext() {
		if (posi < bitCount -1) {
			return true;
		} else {
			return false;
		}
	}
	
	JDataSet getFirstPage() {
		posi = 0;
		int anz = pl.getResultSetPage();
		long[] oids = getResult(posi, anz);
      try {
	      JDataSet ds = pl.getObjectPage(oids);
	      return ds;
      } catch (Exception e) {
	      e.printStackTrace();
	      throw new IllegalStateException(e);
      }
	}
	
	long[] getNext() {
		if (posi >= bitCount -1) {
			throw new IllegalStateException("End of ResultSet reached");
		}
		int anz = pl.getResultSetPage();
		if (posi + anz > bitCount) {
			anz = bitCount - posi;
		}
		posi = posi + anz;
		long[] ret = getResult(posi, anz);
		return ret;
	}
	
	public JDataSet getNextPage() {
		if (this.hasNext()) {
			long[] oids = this.getNext();
	      try {
		      JDataSet ds = pl.getObjectPage(oids);
		      ds.setOid(posi);
		      return ds;
	      } catch (Exception e) {
		      e.printStackTrace();
		      throw new IllegalStateException(e);
	      }
		} else {
			throw new IllegalStateException("End of ResultSet reached");
		}
	}
	public JDataSet getPrevPage() {
		if (posi == 0) {
			throw new IllegalStateException("Begin of ResultSet reached");
		}
		int anz = pl.getResultSetPage();
		posi = posi - anz;
		long[] oids = getResult(posi, anz);
		if (posi < 0) posi = 0;
      try {
	      JDataSet ds = pl.getObjectPage(oids);
	      ds.setOid(posi);
	      return ds;
      } catch (Exception e) {
	      e.printStackTrace();
	      throw new IllegalStateException(e);
      }
	}
		
	/**
	 * Liefert die bisher verwendeten Items.
	 * @return ArrayList
	 */
	ArrayList<String> getItems() {
		return items;
	}
	/**
	 * Menge der nicht vorhandenen Suchbegriffe
	 * @return
	 */
	String getMissingItems() {
		return missingItems;
	}
	
	long getDuration() {
		return this.duration;
	}
	void reset() {
		this.calls = 0;
		this.bitCount = 0;
		this.posi = 0;
		this.duration = 0;
		this.slot = null;
		this.items = new ArrayList<String>();
		this.missingItems = "";
	}
	public static int countBitsSet(int[] values) {
		int count = 0;
		for(int i = 0; i < values.length; i++) {
			count += countBitsSet(values[i]);
		}
		return count;
	}
	/**
	 * This method counts the number of bits set within the given
	 * integer. Given an n-bit value with k of those bits set, the
	 * efficiency of this algorithm is O(k) rather than the O(n) of
	 * an algorithm that simply looped through all bits counting non
	 * zero ones.
	 * @return Number of non-zero bits in value
	 * @param value Value whose bits are to be counted
	 */
	static int countBitsSet( int value )
	{
			int count = 0;

			while( value != 0 )
			{
					// The result of this operation is to subtract off
					// the least significant non-zero bit. This can be seen
					// from noting that subtracting 1 from any number causes
					// all bits up to and including the least significant
					// non-zero bit to be complemented.
					//
					// For example:
					//               value = 10101100
					//           value - 1 = 10101011
					// (value - 1) & value = 10101000

					value &= value - 1;

					count++;
			}
			return count;
	}	

 /**
    * Counts number of 1 bits in a 32 bit unsigned number.
    *
    * @param x unsigned 32 bit number whose bits you wish to count.
    *
    * @return number of 1 bits in x.
    */
 	/*
   static int countBits1(int x)
   {
      // collapsing partial parallel sums method  
      // collapse 32x1 bit counts to 16x2 bit counts, mask 01010101
      x = (x >>> 1 & 0x55555555) + (x & 0x55555555);
      // collapse 16x2 bit counts to 8x4 bit counts, mask 00110011
      x = (x >>> 2 & 0x33333333) + (x & 0x33333333);
      // collapse 8x4 bit counts to 4x8 bit counts, mask 00001111
      x = (x >>> 4 & 0x0f0f0f0f) + (x & 0x0f0f0f0f);
      // collapse 4x8 bit counts to 2x16 bit counts
      x = (x >>> 8 & 0x00ff00ff) + (x & 0x00ff00ff);
      // collapse 2x16 bit counts to 1x32 bit count
      return(x >>> 16) + (x & 0x0000ffff);
   }
   */
   /**
    * Counts number of 1 bits in a 32 bit unsigned number.
    *
    * @param x unsigned 32 bit number whose bits you wish to count.
    *
    * @return number of 1 bits in x.
    */
   /*
   static int countBits2(int x)
   {
      // classic shift method
      int result = 0;
      for ( int i=0; i<32; i++ )
      {
         result += x & 1;
         x >>>= 1;
      }
      return result;
   } 
   */	
	
	public static Oper toOper(int i) {
		switch(i) {
		case 0:
			return Oper.NONE;
		case 1:
			return Oper.AND;
		case 2:
			return Oper.OR;
		case 3:
			return Oper.XOR;
		case 4:
			return Oper.NOT;
			
			default:
			return Oper.NONE;
		}
	}
}
