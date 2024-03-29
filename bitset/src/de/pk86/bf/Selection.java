package de.pk86.bf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;

import de.jdataset.JDataSet;
import de.pk86.bf.pl.BfPL;
import de.pk86.bf.pl.Item;

/**
 * Hält den Zustand einer Session
 */
public class Selection implements Serializable {
   private static final long serialVersionUID = 1L;
   
	private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Selection.class);

	public static enum Oper {NONE, AND, OR, XOR, NOT}; 
	private int sessionId; // Eindeutige ID
	private Date created = new Date(); // Erzeugt
	private Date timestamp = new Date(); // Zeitstempel
	
	private transient BfPL pl = BfPL.getInstance();
	private BfPL getPL() {
		if (pl == null) {
			pl = BfPL.getInstance();
		}
		return pl;
	}
	
	private Item item;
	private int bitCount;
	private int calls;
	private long duration;
	private int posi; // Pointer im Resultset
	private int index; // Pointer im Bitset
	private int indexTop;
	private int indexBottom;
	private ArrayList<String> items = new ArrayList<String>();
	private String missingItems = ""; 
	
   public List<TraceElement> trace = new ArrayList<TraceElement>();

	// Constructor
   Selection() {
   	pl = BfPL.getInstance();
   } 
   
	Selection (int id) {
		this.sessionId = id;
	}
	public int getSessionId() {
		return this.sessionId;
	}
	
	Date getTimestamp() {
		return this.timestamp;
	}
	
	int performOper(ArrayList<OperToken> al) {	
		if (al == null || al.size() == 0) 
			return 0;
		items = new ArrayList<String>();
		// Sich alle items merken
		for (OperToken ot:al) {
			items.add(ot.token);
		}		
		// Klammer; von der innersten Klammer das erste Token suchen
		int cnt = 0;
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
			cnt++;
			if (cnt > 2000) { // Notbremse Endlosschleife
				return 0;
			}
		} 
		
		this.item = al.get(0).item;
		int ret;
		if (item == null) {
			ret = 0;
		} else {
			BitSet bs = item.getBitset();
			ret = bs.cardinality();
			this.bitCount = ret;
		}
		timestamp = new Date();
		return ret;
	}
	
	private void performBrace(ArrayList<OperToken> al, int startIndex, int level) {
		//System.out.println(al+ " : " + startIndex + "/" +level);
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
		if (cnt == 1) { // (a) // überflüssig?
			OperToken ot = al.get(startIndex);
			ot.level--;
			//System.out.println(al+ " : " + ot.level);
			return;
		}
		OperToken ot1 = al.get(startIndex);
		addTraceElement(ot1.token, ot1.item.getBitset().cardinality());
		//ot1.item = ot1.item.clone(); // HACK
		StringBuilder sbe = new StringBuilder();
		for (int i = startIndex+1; i < startIndex + cnt; i++) {
			OperToken ot2 = al.get(i);
			//System.out.println(ot1+ "--" + ot2);
			if (ot2.item == null) {
				logger.warn("Missing Item: " + ot2.token);
				missingItems += ot2.token + " ";
			} else {
				int bitCount = ot2.item.getBitset().cardinality();
				BitSet erg = performOper(ot1.item.getBitset(), ot2.item.getBitset(), ot2.oper);
				sbe.append(ot2.oper);
				sbe.append(" " + ot2.token);
				ot1.token = "[" + getTraceSize() + "]";
				ot1.item.setBitset(erg); // TODO: gefährlich! Wenn der Cache diese Daten zurückschreibt! 
				//sbe.append(" ");
				addTraceElement(sbe.toString()+"{"+bitCount+"}", ot1.item.getBitset().cardinality());
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
		if (ot.item == null) {
			logger.warn("Missing Slot: " + ot.token);
			missingItems += ot.token + " ";
			return 0;
		}
		items.add(ot.token);
		if (ot.oper == Oper.NONE || calls == 0) { // Der Erste
			this.item = ot.item.clone(); // Clone ist hier wichtig, sonst wird Cache vermüllt!
		} else {
				BitSet b1 = this.item.getBitset();
				BitSet b2 = ot.item.getBitset();
				this.item.setBitset(performOper(b1, b2, ot.oper));
		} 
		this.calls++;
		// Count bits
		int ret = item.countBits();
		this.bitCount = ret;
		timestamp = new Date(); // Zeitstempel
		return ret;	
	}
	
	private static BitSet performOper(BitSet b1, BitSet b2, Oper oper) {
		if (b1== null || b2 == null) {
			throw new IllegalArgumentException("Argument null");
		}
		BitSet bErg = (BitSet)b1.clone(); // default für NONE
		switch (oper) {
			case NONE:  // Nix machen: Clone von b1 so wieder ausliefern
				//bErg = (BitSet)b1.clone();
				break;
			case AND: bErg.and(b2); 
				break;
			case OR:  bErg.or(b2);
				break;
			case XOR: bErg.xor(b2);			
				break;
			case NOT: bErg.andNot(b2);
				break;
		} // End Switch
		return bErg;		
	}
	
	int getResultSetSize() {
		return bitCount;
	}
	int[] getResultSet() {
		if (this.bitCount > getPL().getMaxResultSet()) {
			throw new IllegalStateException("Maximum ResultSet Size exceeded: " + Integer.toString(bitCount));
		}
		return getResult(0, bitCount, true);
	}
	private int[] getResult(int start, int cnt, boolean forward) {
		if (bitCount == 0) return null;
		if (start + cnt > bitCount) {
			cnt = bitCount - start;
		}
		if (cnt == 0) return null;
		int[] ret = new int[cnt];
		int poi;
		if (forward) {
			poi = 0; // Pointer zum Array
			index = indexBottom;
		} else {
			poi = cnt - 1;
			index = indexTop;
		}
		if (item == null) {
			return null;
		}
		BitSet bs = item.getBitset();
		for (int i = 0; i < cnt; i++) {
			if (forward) {			
				index++;
				index = bs.nextSetBit(index);
				ret[poi] = index;
				poi++;
			} else if (index >0){
				index--;
				index = bs.previousSetBit(index);
				ret[poi] = index;
				poi--;
			}
		}
		indexTop = ret[0];
		indexBottom = ret[cnt-1];
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
		timestamp = new Date(); // Zeitstempel
		posi = 0; index = 0;
		int anz = getPL().getResultSetPage(); // 20
		int[] oids = getResult(posi, anz, true);
      try {
	      JDataSet ds = getPL().getObjectPage(oids);
	      return ds;
      } catch (Exception e) {
	      e.printStackTrace();
	      throw new IllegalStateException(e);
      }
	}
	
	private int[] getNext() {
		timestamp = new Date(); // Zeitstempel
		if (posi >= bitCount -1) {
			throw new IllegalStateException("End of ResultSet reached");
		}
		int anz = getPL().getResultSetPage(); // 20
		if (posi + anz > bitCount) {
			anz = bitCount - posi;
		}
		posi = posi + anz;
		int[] ret = getResult(posi, anz, true);
		return ret;
	}
	
	public JDataSet getNextPage() {
		timestamp = new Date(); // Zeitstempel
		if (this.hasNext()) {
			int[] oids = this.getNext();
	      try {
		      JDataSet ds = getPL().getObjectPage(oids);
		      if (ds == null) {
					throw new IllegalStateException("End of ResultSet reached");
		      }
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
		timestamp = new Date(); // Zeitstempel
		if (posi == 0) {
			throw new IllegalStateException("Begin of ResultSet reached");
		}
		int anz = getPL().getResultSetPage(); // 20
		posi = posi - anz;
		int[] oids = getResult(posi, anz, false);
		if (posi < 0) posi = 0;
      try {
	      JDataSet ds = getPL().getObjectPage(oids);
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
	public void reset() {
		this.calls = 0;
		this.bitCount = 0;
		this.posi = 0;
		this.duration = 0;
		this.item = null;
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
   
   private static class TraceElement implements Serializable {
      private static final long serialVersionUID = 1L;

      public String expression;
   	public int elements;
   	
   	TraceElement(String expression, int cnt) {
   		this.expression = expression;
   		this.elements = cnt;
   	}
   }
   public String toString() {
   	return "SessionID: " + this.sessionId;
   }
}
