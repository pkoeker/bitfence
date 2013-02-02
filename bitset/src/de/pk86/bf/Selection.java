package de.pk86.bf;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

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
	private int index; // Pointer im Bitset
	private ArrayList<String> items = new ArrayList<String>();
	private String missingItems = ""; 
	
   public List<TraceElement> trace = new ArrayList<TraceElement>();

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
		
		this.slot = al.get(0).slot;
		int ret;
		if (slot == null) {
			ret = 0;
		} else {
			BitSet bs = slot.getBitset();
			ret = bs.cardinality();
			this.bitCount = ret;
		}
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
		StringBuilder sbe = new StringBuilder();
		sbe.append(ot1.token + " ");
		ot1.slot = ot1.slot.clone(); // HACK
		for (int i = startIndex+1; i < startIndex + cnt; i++) {
			OperToken ot2 = al.get(i);
			//System.out.println(ot1+ "--" + ot2);
			if (ot2.slot == null) {
				logger.warn("Missing Slot: " + ot2.token);
				missingItems += ot2.token + " ";
			} else {
				BitSet erg = performOper(ot1.slot.getBitset(), ot2.slot.getBitset(), ot2.oper);
				sbe.append(ot2.oper);
				sbe.append(" " + ot2.token);
				ot1.token = "[" + getTraceSize() + "]";
				ot1.slot.setBitset(erg); // TODO: gefährlich! Wenn der Cache diese Daten zurückschreibt! 
				sbe.append(" ");
				addTraceElement(sbe.toString(), ot1.slot.getBitset().cardinality());
				sbe = new StringBuilder();
			}
		}
		for (int i = startIndex + cnt -1; i>startIndex; i--) {
			al.remove(i);
		}
		ot1.level--; // keine Klammer mehr
		ot1.brace = OperToken.Brace.NONE;
	}
	/**
	 * @deprecated @see {@link #performBrace(ArrayList, int, int)}
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
				BitSet b1 = this.slot.getBitset();
				BitSet b2 = ot.slot.getBitset();
				this.slot.setBitset(performOper(b1, b2, ot.oper));
		} 
		this.calls++;
		// Count bits
		int ret = slot.countBits();
		this.bitCount = ret;
		return ret;	
	}
	
	private static BitSet performOper(BitSet b1, BitSet b2, Oper oper) {
		if (b2 == null) {
			throw new IllegalArgumentException("Argument null");
		}
		if (b1 == null) {
			oper = Oper.NONE; // Kopieren
		}
		BitSet bErg = (BitSet)b1.clone();
		switch (oper) {
			case NONE: { 
				bErg = (BitSet)b1.clone();
			}
				break;
			case AND: {
				bErg.and(b2);
			}
				break;
			case OR: {
				bErg.or(b2);
			}
				break;
			case XOR: {
				bErg.xor(b2);
			}
				break;
			case NOT: {
				bErg.andNot(b2);
			}
				break;
		} // End Switch
		return bErg;		
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
		if (slot == null) {
			return null;
		}
		BitSet bs = slot.getBitset();
		index = start;
		while(poi < cnt) {
			index = bs.nextSetBit(index);
			ret[poi] = index;
			index++;
			poi++;
		}				
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
		posi = 0; index = 0;
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
	
   private void addTraceElement(String expression, int cnt) {
   	TraceElement ele = new TraceElement(expression, cnt);
   	trace.add(ele);
   }
   
   private int getTraceSize() {
   	return trace.size();
   }
   
   public String getTrace() {
   	StringBuilder sb = new StringBuilder();
   	for (int i = 0; i < trace.size(); i++) {
   		TraceElement ele = trace.get(i);
   		sb.append("[" + i + "]: ");
   		sb.append(ele.expression+ " --> ");
   		sb.append(ele.elements);
   		sb.append('\n');
   	}
   	return sb.toString();
   }
   
   private static class TraceElement {
   	public String expression;
   	public int elements;
   	
   	TraceElement(String expression, int cnt) {
   		this.expression = expression;
   		this.elements = cnt;
   	}
   }

}
