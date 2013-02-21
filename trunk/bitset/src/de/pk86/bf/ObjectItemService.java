package de.pk86.bf;

import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import de.jdataset.JDataColumn;
import de.jdataset.JDataRow;
import de.jdataset.JDataSet;
import de.jdataset.JDataTable;
import de.pk86.bf.pl.BfPL;
import electric.xml.Element;
//import electric.registry.Registry;
//import electric.server.http.HTTP;
//import electric.util.Context;

/**
 * Dieser Dienst bietet die Möglichkeiten einer "Suchmaschine" im Kleinen.
 * <UL>
 * <li>Es können "Objekte" definiert werden.
 * <li>Es können "Eigenschaften" definiert werden.
 * <li>Die Objekte können mit beliebig vielen "Eigenschaften" versehen werden.
 * <li>Mit logischen Ausdrücken (AND, OR, NOT) wird die Menge von Objekte
 * ermittelt, welche bestimmte Eigenschaften haben.
 * </UL>
 * Die so definierten "Objekte" sind hier nichts weiter als ein Identifier für
 * eine beliebige Information; um diesem Objekt einen semantischen Inhalt zu
 * geben wird entweder die Tabelle "Object" um weitere Spalten ergänzt oder
 * dieser Identifiert für den Zugriff auf eine andere Datenquelle verwendet.
 * <p>
 * Eine "Eigenschaft" ist eine beliebige Zeichenkette von max. 128 Zeichen.<p>
 * <h2>Vorgehen bei einer Abfrage</h2>
 * Es gibt grundsätzlich zwei Möglichkeiten, eine Abfrage zu erstellen:
 * <ul>
 * <li>Durch Angabe eines Ausdrucks
 * <li>Schritt für Schritt
 * </ul>
 * <h3>Ausdruck</h3>
 * Der Methode execute() wird ein Ausdruck in einem String übergeben 
 * und die Ergebnismenge als ein Array von Object-IDs geliefert. Der String
 * enthält beliebig viele gültige Eigenschaften, die jeweils mit einem Operator
 * verknüpft sind. Die Eigenschaften und Operatoren sind durch White Space
 * voneinander getrennt.<p>
 * Gültige Operatoren sind:
 * <ul>
 * <li>| --> OR
 * <li>- --> NOT
 * <li>^ --> XOR
 * <li>+ --> AND
 * </ul>
 * Der Operator AND kann auch weggelassen werden.<p>
 * Beispiel:
 * <br> Heiß | weiß<p>
 * Z.Z. wird der Ausdruck von links nach rechts ausgewertet; es dürfen also
 * keine Klammern angegeben werden.
 * <h3>Schritt für Schritt</h3>
 * Zuerst ist eine Transaktion zu starten (Siehe startTrans()). Eine Transaktion
 * (oder Session) erhält dabei einen beliebigen Namen, unter dem im weiteren
 * Verlauf auf sie Bezug genommen werden muß.<p> 
 * Anschließend können beliebig
 * viele Abfragen durchgeführt werden (siehe performOper()). Jede Abfrage
 * besteht aus einer Eigenschaft und einem Operator (AND, OR, NOT, XOR). Mit dem
 * Operator wird das bisherige Zwischenergebnis logisch verknüpft.
 * <ul>
 * <li>AND schränkt also die Ergebnismenge weiter ein (gib mir alle Objekte die
 * "heiß" UND "weiß" sind).
 * <li>Ein OR erweitert die Ergebnismenge (gib mir alle Objekt die "heiß" ODER
 * "weiß" sind). 
 * <li>Ein NOT trägt alle Objekte mit dieser Eigenschaft aus der Ergebnismenge
 * aus (laß alle Objekte weg, die "weiß" sind).
 * <li>Ob XOR zu irgendetwas gut ist ?
 * </ul>
 * Bei jeder Operation wird die Größe der Ergebnismenge geliefert.
 * Es macht zumeist keinen Sinn, sich große Ergebnismengen anzusehen,
 * sondern diese durch weitere Operationen einzuschränken.<p>
 *  Hat die
 * Ergebnismenge die gewünschte Größe, können die Objekte mit getResultSet()
 * abgerufen werden.<p>
 * Mit endTrans() wird eine Abfrage beendet und der Speicher wieder frei
 * gegeben.<p>
 * <h2>Magic Method</h2> 
 * In manchen Fällen ist bei einer Recherche unklar, auf welche Art die
 * Ergebnismenge weiter eingeschränkt werden kann, also welche Eigenschaften die
 * Objekte der derzeitigen Ergebnismenge haben, aber noch nicht abgefragt
 * wurden. Hierzu dient die Methode getOtherItems().
 * <h2>Performanz, Ressourcen, Begrenzungen</h2>
 * 
 * Der hier implementierte Suchalgorithmus ist extrem schnell. Auch dann, wenn
 * große Datenmengen mit sehr komplexen Ausdrücken durchsucht werden. Insbesonders besteht keine Veranlassung, bei der
Zuordnung von Eigenschaften zu einem Objekt zurückhaltend zu sein.
Es kostet keine Performanz, wenn ein Objekt mehrere Tausend Eigenschaften hat.
<p> Ein
Beispieldatenbestand von 40000 Objekten mit 10000 Eigenschaften wobei jedes
Objekt im Durchschnitt neun Eigenschaften hat benötigt ca 120MB Daten (MySQL-
InnoDB).<p> Die Zeit für eine beliebige Abfrage liegt unter einer Millisekunde.<p>
<p>
<h2>Hinweis</h2>
Der benötigte Speicherbedarf der Datenbank ist im Wesentlichen durch
die Größe des Intervalls der Objekt-IDs definiert; es macht also Sinn,
"Lücken" bei den Objekt-IDs möglichst zu vermeiden.
<h2>ToDo</h2>
<ul>
<li>renameItem(String oldItem, String newItem);<br>
 geht im Moment nicht, da MySQL kein DROP CONSTRAINT kann :-(
<li>Datenbanktabellen automatisch anlegen.
<li>Object-Ids fortlaufend aus dem Nummernkreis vergeben (OK). 
(dabei auch Lücken wieder auffüllen!).
</ul>
 * @author Peter Köker
 */
public final class ObjectItemService implements ObjectItemServiceIF {
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ObjectItemService.class);

	private BfPL pl = BfPL.getInstance();
	private Spider spider;
	/**
	 * Session werden nach 15 Minuten timeout gelöscht
	 */
	private Hashtable<Integer, Selection> sessions = new Hashtable<Integer, Selection>();
	
	private int sessionCounter; // TODO: Beim runterfahren persistent machen
	// Constructor
	/**
	 * Erzeugt einen neuen Dienst.
	 */
	public ObjectItemService() {
		this.initWebservice();
		this.initSpider();
		SessionRemover remover = new SessionRemover(this);
		remover.setDaemon(true);
		remover.start();
	}
	/** 
	 * Für neue Sessions
	 * @return
	 */
	public int getNewSessionId() {
		return sessionCounter++;
	}
	/**
	 * Erzeugt ein Objekt mit der angegebenen Nummer.<p>
	 * Diese Objekt-ID ist nicht weiter als ein Identifier für
	 * eine beliebige Information. In der Datenbank kann die Tabelle
	 * "Object" um beliebige weitere Spalten ergänzt werden oder
	 * diese Object-ID wird als Identifier für eine andere Datenquelle verwendet.
	 * @param oid Object-ID
	 * @throws IllegalArgumentException wenn oid < 0 oder größer MAX_OID
	 */
	public void createObject(long oid) {
		try {
			pl.createObject(oid);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
		}
	}
	/**
	 * Erzeugt eine neues Objekt und liefert dabei die vergebene oid.
	 * @return long Die oid des neuen Objekts oder -1 wenn dabei ein Fehler
	 * aufgetreten ist.
	 */
	public long createObject() {
		long ret = -1;
		try {
			ret = pl.getOid();
			this.createObject(ret);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
		}
		return ret;
	}
	/**
	 * Löscht das Object mit der angegebenen Nummer.<p>
	 * Die Eigenschaften zu diesem Objekt werden mit gelöscht.
	 * @param oid
	 */
	public void deleteObject(long oid) {
		try {
			pl.deleteObject(oid);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
		}
	}
	/**
	 * Erzeugt eine neue Eigenschaft.<p>
	 * Achtung! Die Eigenschaften sind case-sensitiv und dürfen auch White Space
	 * (Leerzeichen) enthalten.
	 * @param name Name der Eigenschaft; max 128 Zeichen.
	 */
	public void createItem(String name) {
		try {
			pl.createItem(name);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
		}
	}
	/**
	 * Löscht eine Eigenschaft. 
	 * @param name
	 */
	public void deleteItem(String name) {
		try {
			pl.deleteItem(name);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
		}
	}
	/**
	 * Versieht ein Objekt mit einer weiteren Eigenschaft.
	 * @param oid
	 * @param itemname
	 */
	public void addObjectItem(long oid, String itemname) {
		try {
//			boolean set = Item.setBit(oid, itemname);
//			if (set) {
				pl.addObjectItem(oid, itemname);
//			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
		}
	}
	/**
	 * Entfernt eine Eigenschaft vom Objekt.
	 * @param oid
	 * @param itemname
	 */
	public void removeObjectItem(long oid, String itemname) {
		try {
			pl.removeObjectItem(oid, itemname);
//			Item.removeBit(oid, itemname);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
		}
	}
	/**
	 * Prüft, ob das angegebene Objekt den angegebene Eigenschaft hat.
	 * @param oid
	 * @param itemname
	 * @return boolean true, wenn das Objekt die angegebene Eigenschaft hat.
	 */
	public boolean hasItem(long oid, String itemname) {
		try {
	      return pl.hasItem(oid, itemname);
      } catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
	      ex.printStackTrace();
	      return false;
      }
	}
	/**
	 * Prüft, ob der angegebene Eigenschaft existiert.
	 * @param itemname
	 * @return boolean
	 */
	public boolean hasItem(String itemname) {
		try {
			return pl.hasItem(itemname);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			return false;
		}
	}
	/**
	 * Liefert die Anzahl der Objekt, die mit dieser Eigenschaft verknüpft sind.
	 * @param itemname Die Eigenschaft
	 * @return int Die Anzahl der Objekte mit dieser Eigenschaft 
	 * oder -1 wenn die Eigenschaft nicht existiert.
	 */
	public int getItemCount(String itemname) {
		try {
			return pl.getItemCount(itemname);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			return -1;
		}
	}
	/**
	 * Liefert alle Eigenschaften, die ähnlich dem angegebenen Pattern sind (SQL
	 * LIKE).
	 * @param pattern
	 * @return String[] Die Ergebnismenge aus der Datenbank sortiert nach
	 * Alphabet.
	 */
	public String[] findItems(String pattern) {
		try {
			return pl.findItems(pattern);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			return null;
		}
	}
	/**
	 * Startet eine neue Session.
	 * @return sessionId, unter dieser id kann auf die Session zugegriffen werden
	 */
	public Selection startSession() {
		int sessionId = this.getNewSessionId();
		Selection sel = new Selection(sessionId);
		if (sessions.put(sessionId, sel) != null) {
			String msg = "Transaction already exists: " + sessionId;
			logger.error(msg);
			throw new IllegalArgumentException(msg);
		}
		return sel;
	}
	/**
	 * @deprecated Dieses schrittweise Vorgehen ist nicht wirklich optimal: Klammern sind so nicht möglich.
	 * Ausführen einer Operation. Diese Operation wird mit dem bisherigen
	 * Zwischenergebnis der Transaktion ausgeführt; die Größe der
	 * Ergebnismenge wird geliefert.<p>
	 * Mit getResultSet werden die Objekte der Ergebnismenge geliefert.
	 * @param name Name der Session die mit startTrans() erzeugt wurde.
	 * @param itemname Name der Eigenschaft.
	 * @param operand Einer der in der Klasse Selection definierten Operatoren
	 * (AND, OR, NOT, XOR); beim ersten Aufruf dieser Methode in einer
	 * Transaktion darf auch der Operator NONE angegeben werden.
	 * @return int Die Größe der Ergebnismenge.
	 */
	public int performOper(int sessionId, String itemname, Selection.Oper operand) {
		Selection sel = sessions.get(sessionId);
		if (sel == null) {
			throw new IllegalArgumentException("ObjectItemService#performOper()\nMissing Session: "+sessionId);
		}
		int erg = 0;
      try {
      	OperToken ot = new OperToken(itemname, operand);
      	// Slots
      	ArrayList<OperToken> al = new ArrayList<OperToken>(1);
      	al.add(ot);
      	pl.findSlots(al);
	      erg = sel.performOper(ot);
      } catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
	      ex.printStackTrace();	      
      }
		return erg;
	}
	
	public ExpressionResult performOper(ArrayList<OperToken> al) {
		long start = System.currentTimeMillis();
		int sessionId = this.startSession().getSessionId(); // Hier wird die neue Session erzeugt
		Selection sel = sessions.get(sessionId);
		int cnt = sel.performOper(al);
		long end1 = System.currentTimeMillis();
		ExpressionResult ret = new ExpressionResult(sessionId);
		//int anz = pl.getResultSetPage();
		JDataSet ds = sel.getFirstPage();
		ret.firstPage = ds;
		ret.resultsetSize = sel.getResultSetSize();
		ret.missingItems = sel.getMissingItems();
		long end2 = System.currentTimeMillis();
		ret.duraAlg = end1-start;
		ret.duraDB2 = end2-end1;
		ret.trace = sel.getTrace();
		return ret;
	}
	/**
	 * Liefert die Ergebnismenge einer Session.
	 * @param name
	 * @return ExpressionResult Die Objekt-IDs aufsteigend sortiert.
	 */
	public ExpressionResult getResultSet(int sessionId) {
		ExpressionResult ret = new ExpressionResult(sessionId);
		Selection sel = sessions.get(sessionId);
		if (sel == null) {
			throw new IllegalArgumentException("ObjectItemService#getResultSet()\nMissing Transaction: "+sessionId);
		}
		//ret.objekts = sel.getResultSet();
		return ret;
	}
	
	public String getObjekts(long[] oids) {
		try {
	      return pl.getObjekts(oids);
      } catch (Exception e) {
	      e.printStackTrace();
	      return null;
      }
	}

	/**
	 * Liefert true, solange getNext() noch Ergebnisse liefert.<p>
	 * <pre>
	 * ObjectItemService sv = new ObjectItemService();
	 * String name = "MySession";
	 * int size = sv.execute(name, "bla blub - blob"); 
	 * while (sv.hasNext(name)) { 
	 *   long[] resultSet = sv.getNext(name); 
	 * }
	 * </pre>
	 * @param name
	 * @return boolean
	 * @see #getNext
	 */
	public boolean hasNext(int sessionId) {
		Selection sel = sessions.get(sessionId);
		if (sel == null) {
			throw new IllegalArgumentException("ObjectItemService#hasNext()\nMissing Session: "+sessionId);
		}
		return sel.hasNext();
	}
	/**
	 * Liefert die Menge der in der Ergebnismenge noch nicht angesprochenen
	 * Eigenschaften.<p>
	 * Vorsicht!<br>
	 * Diese Methode nur aufrufen, wenn die Ergebnismenge klein ist!
	 * @param name Eine laufende Session
	 * @return ArrayList Menge der noch verfügbaren Eigenschaften.
	 */
	public Map<String,Integer> getOtherItems(int sessionId) {
		Selection sel = sessions.get(sessionId);
		if (sel == null) {
			throw new IllegalArgumentException("ObjectItemService#getOtherItems()\nMissing Session: "+sessionId);
		}
		try {
			int[] oids = sel.getResultSet();
			ArrayList<String> items = sel.getItems(); 
			return pl.getOtherItems(oids, items);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
		}
		return null;
	}
	/**
	 * Beendet eine Session und gibt den Speicher wieder frei.
	 * @param name
	 */
	public void endSession(int sessionId) {
		Selection sel = sessions.get(sessionId);
		if (sel == null) {
			throw new IllegalArgumentException("ObjectItemService#endTrans()\nMissing Session: "+sessionId);
		}
		sessions.remove(sessionId);		
		sel.reset();
	}
	/**
	 * Löscht alle Sessions
	 */
	public void resetAllSessions() {
		sessions = new Hashtable<Integer, Selection>();
	}
	/**
	 * Startet eine Session und erstellt eine Ergebnismenge aus einem
	 * Ausdruck.<p> 
	 * Eigenschaften können selbst auch White Space enthalten; dann sind sie in
	 * Anführungszeichen "Meine Eigenschaft" einzuschließen.<br> Der Ausdruck darf
	 * Java-Style-Kommentare enthalten (// oder /*)
	 * Die Ergebnismenge selbst kann anschließend mit getResultSet()
	 * abgerufen werden, oder seitenweise mit getNext().
	 * @param name Der Name einer Transaktion.
	 * @param expression Ein Ausdruck, bei dem gültigen Eigenschaften und
	 * Operatoren; alles durch White Space getrennt.
	 * @return int Die Größe der Ergebnismenge
	 * @see #hasNext
	 * @see #getNext
	 */
	public ExpressionResult execute(String expression) {
		expression = expression.toLowerCase();
		ExpressionResult res = null;

		long startTime = System.currentTimeMillis(); // Zeit messen
		Selection.Oper oper = Selection.Oper.AND;
		
		StreamTokenizer stoks = new StreamTokenizer(new StringReader(expression));
		stoks.resetSyntax();
		stoks.wordChars('!', '~');
		stoks.wordChars('À', 'ÿ'); // ISO-8859-1 (äöüß), aber kein Unicode
		stoks.whitespaceChars('\t', ' ');
		stoks.whitespaceChars('-', '.'); // Datum
		stoks.quoteChar('\"');
		stoks.ordinaryChar('/');
		stoks.ordinaryChar('(');
		stoks.ordinaryChar(')');
		//stoks.ordinaryChar('-');
		stoks.ordinaryChar('!');
		stoks.ordinaryChar('+');
		stoks.ordinaryChar('^');
		stoks.ordinaryChar('|');
		stoks.slashSlashComments(true);
		stoks.slashStarComments(true);
		stoks.eolIsSignificant(true); // ??
		ArrayList<OperToken> al = new ArrayList<OperToken>();
		try {
			int level = 0;
			OperToken.Brace brace = OperToken.Brace.NONE;
			OperToken prevOp = null;
			while (stoks.nextToken() != StreamTokenizer.TT_EOF) {
				String tok = stoks.sval;
				// Klammern
				switch (stoks.ttype) {
					case '(':
						level++;
						brace = OperToken.Brace.OPEN;
						break;
					case ')':
						if (level > 0) { // Problem: mehr ")" als "("
							level--;
							brace = OperToken.Brace.NONE;
							if (prevOp != null) {
								if (prevOp.brace == OperToken.Brace.OPEN) { // Problem: wenn (x), dann beide Klammern raus, Level-- 
									prevOp.brace = OperToken.Brace.NONE;
								} else {
									prevOp.brace = OperToken.Brace.CLOSE; 
								}
							}
						}
						break;
					case '|':
						oper = Selection.Oper.OR;
						break;
					case '-':
					case '!':
						oper = Selection.Oper.NOT;
						break;
					case '^':
						oper = Selection.Oper.XOR;
						break;
					case '+':
						oper = Selection.Oper.AND;
						break;
				}
				if (tok != null) { // passiert das jemals? Ja! Bei ordinary Chars
						OperToken op = new OperToken(tok, oper);
						op.brace = brace;
						op.level = level;
						al.add(op);
						oper = Selection.Oper.AND; // default, falls kein Operator angegeben
						prevOp = op; // für close
						brace = OperToken.Brace.NONE;
					}				
			}
			StringBuilder sb = new StringBuilder();
			pl.findSlots(al); // Reichert mit Slots aus der Datenbank/dem Cache an; wenn Slot null, dann gibts den begriff nicht
			for (int i = al.size()-1; i>=0; i--) {
				OperToken ot = al.get(i);
				if (ot.slot == null) {
					sb.append(ot.token + " ");
					al.remove(i);
				}
			}
			long end1 = System.currentTimeMillis();
			res = this.performOper(al);
			if (sb.length() > 0) {
				res.missingItems = sb.toString();
			}
			res.duraDB1 = end1 - startTime;
			logger.info(expression);
			
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
		}
		return res;
	}
	
	public JDataSet getFirstPage(int sessionId) {
		Selection sel = sessions.get(sessionId);
		if (sel == null) return null;
		return sel.getFirstPage();
	}
	public JDataSet getNextPage(int sessionId) {
		Selection sel = sessions.get(sessionId);
		if (sel == null) return null;
		try {
			JDataSet ds = sel.getNextPage();
			return ds; 
		} catch (IllegalStateException ex) {
			throw ex;
		}
	}
	public JDataSet getPrevPage(int sessionId) {
		Selection sel = sessions.get(sessionId);
		if (sel == null) return null;
		try {
			JDataSet ds = sel.getPrevPage();
			return ds;
		} catch (IllegalStateException ex) {
			throw ex;
		}
	}
	public int updateObjects(JDataSet ds) {
		int cnt;
      try {
	      cnt = pl.updateObjects(ds);
	      return cnt;
      } catch (Exception ex) {
	      ex.printStackTrace();
	      logger.error(ex.getMessage(), ex);
	      return 0;
      }
	}
	
	
	/**
	 * Importiert durch White Space getrennte Eigenschaften.<p> 
	 * Hierbei können die Eigenschaften auch in Anführungszeichen eingeschlossen
	 * sein: "Meine Eigenschaft".
	 * @param text
	 * @param lowercase Wenn true wird Rudi zu rudi.
	 */
	public void importItems(String text, boolean lowercase) {
		int cnt = 0;
		StreamTokenizer stoks = new StreamTokenizer(new StringReader(text));
		stoks.wordChars('!', '~');
		stoks.wordChars('À', 'ÿ');
		stoks.lowerCaseMode(lowercase);
		stoks.whitespaceChars('\t', ' ');
		stoks.quoteChar('\"');
		stoks.ordinaryChar('/');
		stoks.slashSlashComments(true);
		stoks.slashStarComments(true);
		try {
			while (stoks.nextToken() != StreamTokenizer.TT_EOF) {
				String tok = stoks.sval;
				try {
					pl.createItem(tok);
					cnt++;
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
				}
			}	
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
		}
		System.out.println(cnt + " Items imported");
	}
	/**
	 * Indiziert ein Objekt mit durch White Space getrennte Worte.<br> 
	 * Hierbei muß ein Wort mit einem Buchstaben anfangen.<p> 
	 * Diese Methode könnte
	 * von einem "Spider" aufgerufen werden, der ein Text-Dokument indizieren
	 * möchte.
	 * <br>
	 * Wird die Eigenschaft "createItems" auf "true" gesetzt, werden die im
	 * Text enthaltenen Worte gleichzeitig als Eigenschaften in die Datenbank
	 * übernommen (Volltext-Indizierung); wenn "false", dann wird das Objekt nur
	 * mit den bereits in der Datenbank vorhandenen Eigenschaften indiziert.
	 * @param oid Ein neu zu erzeugendes Objekt
	 * @param text Ein Text zu diesem Objekt, der indiziert werden soll.
	 * @param createItems wenn true, dann werden auch neue Eigenschaften erzeugt.
	 * @param lowercase Wenn true wird Rudi zu rudi.
	 */
	public void indexObject(long oid, String text, boolean createItems, boolean lowercase) {
		this.indexObject(oid, new StringReader(text), createItems, lowercase);
	}
	void indexObject(long oid, Reader reader, boolean createItems, boolean lowercase) {
		HashSet<String> hs = new HashSet<String>(200);
		try {
			StreamTokenizer stoks = new StreamTokenizer(reader);
			stoks.wordChars('a', 'z');
			stoks.wordChars('A', 'Z');
			stoks.wordChars('À', 'ÿ');
			stoks.lowerCaseMode(lowercase);
			stoks.whitespaceChars('\t', ' ');
			stoks.whitespaceChars('#','/');
			stoks.whitespaceChars(':','@');
			stoks.whitespaceChars('[','_');
			stoks.whitespaceChars('{','~');
			stoks.quoteChar('\"');
			//stoks.ordinaryChar('/');
			//stoks.slashSlashComments(true);
			//stoks.slashStarComments(true);
			// 1. Zuerst alle Worte in ein HashSet
			// das spart doppelte Worte
			while (stoks.nextToken() != StreamTokenizer.TT_EOF) {
				String tok = stoks.sval;
				if (stoks.ttype == StreamTokenizer.TT_WORD && tok.length() <= 128) {
					hs.add(tok);
				}
			}
			// 2. Die ermittelten Worte durchschleifen
			Iterator<String> i = hs.iterator();
			while (i.hasNext()) {
				String tok = i.next();
				boolean exists = this.hasItem(tok);
				// 2.a Nur vorhandene Items indizieren
				if (exists == false && createItems == false) {
					continue;
				}
				// 2.b Neue Items erzeugen
				if (exists == false && createItems == true) {
					pl.createItem(tok);
				}
				// 2.c Bit setzen
				boolean set = pl.hasItem(oid, tok);
				if (!set) {
					pl.addObjectItem(oid, tok);
				}				
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
		}
		// End Trans
	}
	/**
	 * Startet den experimentelle Spider.<br>
	 * In der Konfigurationsdatei ist festgelegt, 
	 * welches Direktory durchsucht wird
	 * und welche File-Extensions berücksichtigt werden.
	 */
	public void startSpider() {
		if (spider != null) {
			spider.index();
		}
	}
	/**
	 * @deprecated macht nix sinnvolles
	 * Überprüft die Konsistenz der Datenbank.
	 */
	public void validate() {
		try {
			int err = pl.validate();
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
		}
	}
	/**
	 * Repariert die Datenbank.
	 */
	public void indexDatabase() {
		try {
			pl.repair();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void clearDatabase() {
		
	}
	
	public void createDatabase() {
		
	}
	
	public int importDatabaseCSV(String data) {
		StringTokenizer toks = new StringTokenizer(data, "\n\r");
		JDataSet ds = new JDataSet("objekt");
		JDataTable tbl = new JDataTable("objekt");
		ds.addRootTable(tbl);
		JDataColumn colId = tbl.addColumn("oid", Types.INTEGER);
		colId.setPrimaryKey(true);
		tbl.addColumn("content", Types.VARCHAR);
		while (toks.hasMoreTokens()) {
			String tok = toks.nextToken();
			JDataRow row = ds.createChildRow();
			row.setValue("content", tok);
		}
		try {
			int cnt = pl.importObjects(ds);
			logger.info(cnt + " objects imported");
			return cnt;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return 0;
		}
	}
	public int importDatabaseDataset(JDataSet data) {
		
		
		return 0;
	}
	private void initWebservice() {
//		if (webserviceCreated) return;
//		Element ele = pl.getWebServiceConfig();
//		if (ele == null)
//			return;
//		Element urlEle = ele.getElement("URL");
//		Element srvEle = ele.getElement("Service");
//		if (urlEle == null || srvEle == null)
//			return;
//		String url = urlEle.getTextString();
//		String srv = srvEle.getTextString();
//		System.out.println("*** Starting WebService ***\nURL: "+url+"\nService: "+srv);
//		try {
//			Context context = new Context();
//			// Geht nicht ???
//			// session has expired. cannot create new session object
//			//context.addProperty( "activation", "session" );
//			context.addProperty( "description", "Object Item Web Service" );
//			HTTP.startup(url);
//			Registry.publish(srv, this, ObjectItemServiceIF.class, context);
//			webserviceCreated = true;
//		} catch (Exception ex) {
//			logger.error(ex.getMessage(), ex);
//			System.err.println(
//				"Unable to start WebService:\n" + ex.getMessage());
//		}
	}
	private void initSpider() {
		Element ele = pl.getSpiderConfig();
		if (ele != null) {
			spider = new Spider(this, ele);
		}
	}	
	
	/**
	 * Nach 15 Minuten Session beenden
	 * @return
	 */
	int checkSessionTimeout() {
		int cnt = 0;
		long now = System.currentTimeMillis();
		for (Selection sel:sessions.values()) {
			long timestamp = sel.getTimestampd().getTime();
			if (now - timestamp > 1000 * 60 * 15) {
				logger.warn("Session timed out: " + sel);
				this.endSession(sel.getSessionId());
				cnt++;
			}
		}		
		return cnt;
	}
	
	//########################################################
	private static final class SessionRemover extends Thread {
		private static int sleep = 60 * 1000; // Eine Minute
		private ObjectItemService srv;
		private static boolean brun = true;

		// Constructor
		private SessionRemover(ObjectItemService srv) {
			logger.info("SessionRemover created");
			this.srv = srv;
			this.setPriority(MIN_PRIORITY);
		}

		// Methods
		@Override
		public void run() {
			// erzwingt schlafen...check...schlafen...
			int cnt = 0;
			try {
				sleep(sleep);
			} catch (InterruptedException e) {
				// nix
			}
			while (brun) {
				if (cnt % 3600 == 0) { // Einmal pro Stunde ein Lebenszeichen
					logger.info("SessionRemover.run " + cnt);
				}
				cnt++;
				srv.checkSessionTimeout();
				try {
					sleep(sleep);
				} catch (InterruptedException e) {
					// nix
				}
			}
		}
	}
}
