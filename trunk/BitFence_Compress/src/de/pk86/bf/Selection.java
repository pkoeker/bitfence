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

	public static final int NONE = 0;
	public static final int AND = 1;
	public static final int OR = 2;
	public static final int XOR = 3;
	public static final int NOT = 4;
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
	/**
	 * 
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
		if (ot.oper == NONE || calls == 0) {
			this.slot = ot.slot;
		} else {
				Slot slot2 = ot.slot;
				int[] arr1 = null;
				int[] arr2 = null;
				if (slot != null) {
					arr1 = slot.getBits();
				} else {
					arr1 = new int[Const.SLOT_INT]; 
				}
				if (slot2 != null) {
					arr2 = slot2.getBits();
				} else {
					arr2 = new int[Const.SLOT_INT]; 
				}
				if (arr1 != null || arr2 != null) {
					switch (ot.oper) {
						case NONE : {
							if (arr2 != null) {
								for (int j = 0 ; j < arr1.length; j++) {
									arr1[j] = arr2[j];	
								}
							}						
						}
						break;
						case AND : { 
							if (arr1 != null && arr2 != null) {
								for (int j = 0 ; j < arr1.length; j++) {
									arr1[j] = arr1[j] & arr2[j];	
								}
							}
						}
						break;
						case OR : { 
							if (arr1 != null && arr2 != null) {
								for (int j = 0 ; j < arr1.length; j++) {
									arr1[j] = arr1[j] | arr2[j];	
								}
							}
						}
						break;
						case XOR : { 
							if (arr1 != null && arr2 != null) {
								for (int j = 0 ; j < arr1.length; j++) {
									arr1[j] = arr1[j] ^ arr2[j];	
								}
							}
						}
						break;
						case NOT : { 
							if (arr1 != null && arr2 != null) {
								for (int j = 0 ; j < arr1.length; j++) {
									//arr2[j] =~arr2[j];
									arr1[j] = arr1[j] & ~arr2[j];	
								}
							}
						}
						break;
					} // End Switch
				} // End If != null
		} // End If NONE
		this.calls++;
		// Count bits
		int[] arr = slot.getBits();
		int ret = countBitsSet(arr);
		this.bitCount = ret;
		return ret;	
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
}
