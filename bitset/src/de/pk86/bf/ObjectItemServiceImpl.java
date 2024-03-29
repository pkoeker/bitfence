package de.pk86.bf;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import net.sf.ehcache.management.CacheStatistics;
import de.jdataset.JDataColumn;
import de.jdataset.JDataRow;
import de.jdataset.JDataSet;
import de.jdataset.JDataTable;
import de.pk86.bf.client.ServiceFactory;
import de.pk86.bf.pl.BfPL;
import electric.xml.Element;

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
/*
@WebService
(    
  serviceName = "bitset",
  portName = "bitset", 
  targetNamespace = "http://pk86.de/bitset", // Wie beim Client-Interface!
  endpointInterface = "de.pk86.bf.ObjectItemSOAPService"
)
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebListener
*/
public final class ObjectItemServiceImpl implements ObjectItemServiceIF, ServletContextListener {
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ObjectItemServiceImpl.class);
	private transient BfPL pl = BfPL.getInstance();
	private transient Spider spider;
	private transient SessionRemover remover;
	private static ObjectItemServiceImpl me;
	/**
	 * Session werden nach 15 Minuten timeout gelöscht
	 */
	private ConcurrentHashMap<Integer, Selection> sessions = new ConcurrentHashMap<Integer, Selection>();
	
	private int sessionCounter; // Beim runterfahren persistent machen
	// Constructor
	/**
	 * Erzeugt einen neuen Dienst.
	 */
	public ObjectItemServiceImpl() { // TODO: wird doppelt aufgerufen :-( [JAX-WS-SOAP (sun-jaxws.xml) + Spring (BfServerConfig) ]
		this(true);
	}
	public ObjectItemServiceImpl(boolean remover) {
		if (ServiceFactory.getService() == null) {
			ServiceFactory.setService(this);
		}
		this.initSpider();
		if (remover) {
			this.initRemover();
		} else {
			// Tomcat
		}
	}
	public static ObjectItemServiceIF getInstance() {
		if (me == null) {
			synchronized (ObjectItemServiceImpl.class) {
	         if (me == null) {
	         	me = new ObjectItemServiceImpl();
	         }
         }
		}
		return me;
	}
	private void initRemover() {
		remover = new SessionRemover(this);
		remover.setDaemon(true);
		remover.start();
	}
	/** 
	 * Für neue Sessions
	 * Muß synchronized sein, damit keine doppelten SessionsIds erzeugt werden.
	 * @return
	 */
	public synchronized int getNewSessionId() {
		return sessionCounter++;	      
	}
	public int countActiveSessions() {
		return sessions.size();
	}
	public int createObject(String content) {
		try {
			int oid = pl.createObject(content);
			return oid;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			return -1;
		}
	}

	/**
	 * Erzeugt ein Objekt mit der angegebenen Nummer.<p>
	 * Diese Objekt-ID ist nicht weiter als ein Identifier für
	 * eine beliebige Information. In der Datenbank kann die Tabelle
	 * "Objekt" um beliebige weitere Spalten ergänzt werden oder
	 * diese Object-ID wird als Identifier für eine andere Datenquelle verwendet.
	 * @param oid Object-ID
	 * @param content
	 * @throws IllegalArgumentException wenn oid < 0 oder größer MAX_OID
	 */
	public void createObject(int oid, String content) {
		try {
			pl.createObject(oid, content);
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
	 * @deprecated unused
	 * Prüft, ob das angegebene Objekt den angegebene Eigenschaft hat.
	 * @param oid
	 * @param itemname
	 * @return boolean true, wenn das Objekt die angegebene Eigenschaft hat.
	 */
	public boolean hasItem(int oid, String itemname) {
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
	 * Liefert alle Eigenschaften, die ähnlich dem angegebenen Pattern sind (SQL LIKE).
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
		if (sessions.size() >= ObjectItemServiceIF.MAX_SESSIONS) {
			String msg = "Maximum number of sessions exceeded";
			logger.error(msg);
			throw new IllegalArgumentException(msg);
		}
		int sessionId = this.getNewSessionId();
		Selection sel = new Selection(sessionId);
		while (sessions.get(sessionId) != null) {
			String msg = "SessionId already exists: " + sessionId;
			logger.error(msg);
			sessionId = this.getNewSessionId();
		}
		sessions.put(sessionId, sel);
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
      	// Items
      	ArrayList<OperToken> al = new ArrayList<OperToken>(1);
      	al.add(ot);
      	pl.findItems(al);
	      erg = sel.performOper(ot);
      } catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
	      ex.printStackTrace();	      
      }
		return erg;
	}
	
	public ExpressionResult performOper(ArrayList<OperToken> al) {
		long start = System.currentTimeMillis();
		Selection sel = this.startSession(); // Hier wird die neue Session erzeugt
		int cnt = sel.performOper(al);
		long end1 = System.currentTimeMillis();
		ExpressionResult ret = new ExpressionResult(sel);
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
	
	public String getObjekts(int[] oids) {
		try {
	      return pl.getObjekts(oids);
      } catch (Exception e) {
      	logger.error(e.getMessage(), e);
	      return null;
      }
	}
	public String getObjekts(long[] oids) {
		try {
	      return pl.getObjekts(oids);
      } catch (Exception e) {
      	logger.error(e.getMessage(), e);
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
	public boolean hasSession(int sessionId) {
		return sessions.containsKey(sessionId);
	}
	/**
	 * Beendet eine Session und gibt den Speicher wieder frei.
	 * @param sessionId
	 * @return true, wenn Session beendet wurde.
	 */
	public boolean endSession(int sessionId) {
		Selection sel = sessions.get(sessionId);
		if (sel == null) {
			logger.error("ObjectItemService#endSession()\nMissing Session: " + sessionId);
			return false;
		}
		sessions.remove(sessionId);		
		sel.reset();
		return true;
	}
	/**
	 * Löscht alle Sessions
	 */
	public void resetAllSessions() {
		sessions = new ConcurrentHashMap<Integer, Selection>();
	}
	public int createSession(String expression) throws RemoteException {
		ExpressionResult res = this.execute(expression);
		if (res == null) {
			return -1;
		} else {
			return res.sessionId;
		}
	}
	/**
	 * Startet eine Session und erstellt eine Ergebnismenge aus einem Ausdruck.<p> 
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
	public ExpressionResult execute(String expression) throws RemoteException {
//		if (expression.startsWith("repair")) {
//			try {
//				int start = 0, end = Integer.MAX_VALUE;
//				String s = expression;
//				if (s.endsWith("0")) {
//					start = 0;end=100000;
//				} else if (s.endsWith("1")) {					
//					start = 100000;end=200000;
//				} else if (s.endsWith("2")) {
//					start = 200000;end=300000;
//				} else if (s.endsWith("3")) {
//					start = 300000;end=400000;
//				} else if (s.endsWith("4")) {
//					start = 400000;end=500000;
//				} else if (s.endsWith("5")) {
//					start = 500000;end=600000;
//				} else if (s.endsWith("6")) {
//					start = 600000;end=700000;
//				} else if (s.endsWith("7")) {
//					start = 700000;end=800000;
//				} else if (s.endsWith("8")) {
//					start = 800000;end=900000;
//				} else if (s.endsWith("9")) {
//					start = 900000;end=1000000;
//				}
//				pl.repair(start,end);
//			} catch (Exception ex) {
//				logger.error(ex.getMessage(), ex);
//			}
//			return null;
//		}
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
			// Prüfen Level
			if (level != 0) {
				throw new RemoteException("Unterschiedliche Anzahl öffnender und schließender Klammern.");
			}
			StringBuilder sbMissing = new StringBuilder();
			pl.findItems(al); // Reichert mit Items aus der Datenbank/dem Cache an; wenn Item null, dann gibts den Begriff nicht
			// Nicht existierende Begriffe aus der ItemMenge löschen
			{
				Iterator<OperToken> it = al.iterator();
				while (it.hasNext()) {
					OperToken ot = it.next();
					if (ot.item == null) {
						sbMissing.append(ot.token + " ");
						it.remove();
					}
				}
			}
			long end1 = System.currentTimeMillis();
			res = this.performOper(al); // Ergebnismenge bilden
			if (sbMissing.length() > 0) {
				res.missingItems = sbMissing.toString();
			}
			res.duraDB1 = end1 - startTime;
			//##logger.info(expression);
			
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			if (ex instanceof RemoteException) {
				throw (RemoteException)ex;
			} else {
				throw new RemoteException(ex.getMessage(), ex);
			}
		}
		return res;
	}
	
	public ExpressionResult select(String expression) throws RemoteException {
		try {
			long start = System.currentTimeMillis();
			Selection sel = this.startSession(); // Hier wird die neue Session erzeugt
			JDataSet ds = pl.getObjekts(expression);
			long end1 = System.currentTimeMillis();
			ExpressionResult ret = new ExpressionResult(sel);
			//int anz = pl.getResultSetPage();
			ret.firstPage = ds;
			ret.resultsetSize = sel.getResultSetSize();
			ret.missingItems = sel.getMissingItems();
			long end2 = System.currentTimeMillis();
			ret.duraAlg = end1-start;
			ret.duraDB2 = end2-end1;
			ret.trace = sel.getTrace();
			return ret;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			if (ex instanceof RemoteException) {
				throw (RemoteException)ex;
			} else {
				throw new RemoteException(ex.getMessage(), ex);
			}
		}
	}
	
	public JDataSet getFirstPage(int sessionId) {
		Selection sel = sessions.get(sessionId);
		if (sel == null) return null;
		return sel.getFirstPage();
	}
	public String getFirstPageString(int sessionId) {
		JDataSet ds = this.getFirstPage(sessionId);	
		String s = ExpressionResult.pageToString(ds);
		return s;
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
	public String getNextPageString(int sessionId) {
		JDataSet ds = this.getNextPage(sessionId);	
		String s = ExpressionResult.pageToString(ds);
		return s;
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
	public String getPrevPageString(int sessionId) {
		JDataSet ds = this.getPrevPage(sessionId);	
		String s = ExpressionResult.pageToString(ds);
		return s;
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
	public void indexObject(int oid, String text, boolean createItems, boolean lowercase) {
		this.indexObject(oid, new StringReader(text), createItems, lowercase);
	}
	void indexObject(int oid, Reader reader, boolean createItems, boolean lowercase) {
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
					// TODO
					//pl.addObjectItem(oid, tok);
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
	 * Repariert die Datenbank.
	 */
	public void indexDatabase() {
		try {
			pl.repair(0,Integer.MAX_VALUE);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
			
	public int importDatabaseCSV(String data) {
		StringTokenizer toks = new StringTokenizer(data, "\n\r");
		JDataSet ds = new JDataSet("objekt");
		JDataTable tbl = new JDataTable("objekt");
		ds.addRootTable(tbl);
		JDataColumn colId = tbl.addColumn("obid", Types.INTEGER);
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
	
	public String getItemCacheStatistics() {
		CacheStatistics cs = pl.getItemCacheStatistics();
		StringBuilder sb = new StringBuilder();
		sb.append("Object Count: " + cs.getObjectCount());
		sb.append('\n');
		sb.append("Cache Hits: " + cs.getCacheHits());
		sb.append(" Cache Hits%: " + cs.getCacheHitPercentage());
		sb.append('\n');
		sb.append("Cache Miss: " + cs.getCacheMisses());
		sb.append(" Cache Miss%: " + cs.getCacheMissPercentage());
		sb.append('\n');
		sb.append("WriterMaxQueueSize: " + cs.getWriterMaxQueueSize());
		sb.append(" Length: " + cs.getWriterQueueLength());
		sb.append('\n');
		return sb.toString();
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
			long timestamp = sel.getTimestamp().getTime();
			if (now - timestamp > 1000 * 60 * 15) {
				boolean terminated = this.endSession(sel.getSessionId());
				if (terminated) {
					logger.warn("Session timed out: " + sel);
					cnt++;
				}
			}
		}		
		return cnt;
	}
	
	public String sayHello() {
		return "Hello World";
	}
	public String echo(String s) {
		return s;
	}
	// ContextListener
	@SuppressWarnings("unchecked")
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		//initRemover();   		
		// sessions laden falls vorhanden
      try {
    	  FileInputStream f_in = new FileInputStream("sessions.ser");
	      // Read object using ObjectInputStream
	      ObjectInputStream obj_in = new ObjectInputStream (f_in);
	      
	      // Read an object
	      Object obj = obj_in.readObject();
	      obj_in.close();
	      if (obj instanceof ConcurrentHashMap) {
	      	// Cast object 
	      	sessions = (ConcurrentHashMap<Integer, Selection>) obj;
	      	logger.debug("Persistent Sessions: " + sessions.size());
	      	for (Selection sel:sessions.values()) {
	      		if (sel.getSessionId() > sessionCounter) {
	      			sessionCounter = sel.getSessionId() +1;
	      		}
	      	}
	      }
      } catch (FileNotFoundException e) {
    	  logger.error(e.getMessage(), e);
	      e.printStackTrace();
      } catch (IOException e) {
    	  logger.error(e.getMessage(), e);
	      e.printStackTrace();
      } catch (ClassNotFoundException e) {
    	  logger.error(e.getMessage(), e);
	      e.printStackTrace();
      }

	}
	@Override
   public void contextDestroyed(ServletContextEvent arg0) {
		if (remover != null) {
			remover.setWorking(false); // Führt auch interrupt() beim Thread aus
		}
      BfPL.getInstance().finalize();
		// sessions serialisieren (wenn vorhanden)
      if (sessions.size() > 0) {
	      try {
	      	// Write to disk with FileOutputStream    
	      	FileOutputStream f_out = new FileOutputStream("sessions.ser");
		      // Write object with ObjectOutputStream
		      ObjectOutputStream obj_out = new ObjectOutputStream (f_out);		      
		      // Write object out to disk
		      obj_out.writeObject ( sessions );
		      obj_out.close();
		      logger.debug("Persistent Sessions: " + sessions.size());
	      } catch (Exception e) {
	    	  logger.error(e.getMessage(), e);
		      e.printStackTrace();
	      }
      }
   }

	
	//########################################################
	private static final class SessionRemover extends Thread {
		private static int sleep = 60 * 1000; // Eine Minute
		private ObjectItemServiceImpl srv;
		private static boolean brun = true;

		// Constructor
		private SessionRemover(ObjectItemServiceImpl srv) {
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
				logger.info("Thread interrupted#1: " + e.getMessage());
			}
			while (brun) {
				if (cnt % 120 == 0) { // Einmal pro Stunde ein Lebenszeichen
					logger.info("SessionRemover#run; Active Sessions: " + srv.countActiveSessions());
				}
				cnt++;
				srv.checkSessionTimeout();
				try {
					sleep(sleep);
				} catch (InterruptedException e) {
					logger.info("Thread interrupted#2: " + e.getMessage());
				}
			}
		}
		
		void setWorking(boolean b) {
			brun = b;
			if (!b) {
				this.interrupt();
			}
		}
	}
}
