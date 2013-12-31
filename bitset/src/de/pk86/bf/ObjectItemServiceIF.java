package de.pk86.bf;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;

import de.jdataset.JDataSet;
/**
 * @author peter
 */
public interface ObjectItemServiceIF extends ObjectItemSOAPService {
	/**
	 * Default White Space (Sparatoren für Items im Content)
	 */
	public static String DEFAULT_DELIM = " ;.,()/-";
	/**
	 * Maximale Anzahl der gleichzeitig angemeldeten User
	 */
	public static int MAX_SESSIONS = 1000;
	
	/**
	 * Erzeugt ein beliebiges Text-Objekt mit dem angegebenen Inhalt.
	 * Die Objekt-Id wird vom System vergeben.
	 * @param content
	 * @return Die vergebene Objekt-Id
	 */
	public int createObject(String content);
	/**
	 * Erzeugt ein beliebiges Text-Objekt mit dem angegebenen Inhalt
	 * unter der angegebenen Objekt-Id
	 * @param oid Eindeutige Id für Objekte
	 * @param content Mit White Space getrennter Inhalt
	 */
	public void createObject(int oid, String content);
	/**
	 * Erzeugt ein neues Schlüsselwort
	 * @param itemname
	 */
	public void createItem(String itemname);
	/**
	 * Löscht ein Schlüsselwort
	 * @param itemname
	 */
	public void deleteItem(String itemname);
	/**
	 * Prüft die Existenz eines Schlüsselworts
	 * @param itemname
	 * @return
	 */
	public boolean hasItem(String itemname);
	/**
	 * Ermittelt die Anzahl der Objekte, die mit diesem Eintrag verknüpft sind
	 * @param itemname
	 * @return
	 */
	public int getItemCount(String itemname);
	/**
	 * Liefert eine Menge von Schlüsselwörtern zu dem angegebenen SQL-Pattern
	 * @param pattern
	 * @return
	 */
	public String[] findItems(String pattern);
	/**
	 * Wirft eine IllegalArgumentException, wenn MAX_SESSIONS (1000) überschritten
	 * @return
	 */
	public Selection startSession();
	/**
	 * Löscht alle Sessions
	 */
	public void resetAllSessions();
	/** @deprecated @see #performOper(List)*/
	public int performOper(int sessionId, String itemname, Selection.Oper operand);
	public ExpressionResult performOper(ArrayList<OperToken> al);
	public ExpressionResult getResultSet(int sessionId);
	public String getObjekts(int[] oids);
	/**
	 * Liefert einen String mit dem Inhalt der angegebenen Objekt; jeweils durch LF getrennt.
	 * @param oids
	 * @return
	 */
	public String getObjekts(long[] oids);
	/**
	 * Liefert true, wenn es unter der angegebenen SessionId weitere Ergebnisse gibt
	 * @see #getNextPage(int)
	 * @param sessionId
	 * @return
	 */
	public boolean hasNext(int sessionId);
	public Map<String,Integer> getOtherItems(int sessionId);
	/**
	 * Liefert true, wenn eine Session mit der angegebenen Id existiert
	 * vor {@link #endSession(int)} aufrufen.
	 * @param sessionId
	 * @return
	 */
	public boolean hasSession(int sessionId);
	/**
	 * Beendet die angegebene Session; die von ihr gehaltenen Ressourcen werden freigegeben.
	 * @param sessionId
	 * @return false, wenn die angegebene Session nicht (mehr) existiert.
	 */
	public boolean endSession(int sessionId);
	/**
	 * Führt den angegebenen Ausdruck aus und liefert einen Handle auf die Session/Ergebnismenge.
	 * @param expression
	 * @return
	 * @throws RemoteException
	 */
	public ExpressionResult execute(String expression) throws RemoteException;
	/**
	 * Erzeugt eine Session mit einer Ergebnismenge zu dem angegebenen Ausdruck.
	 * Anschließend kann mit der gelieferten sessionId die Ergebnismenge angerufen werden.
	 * @param expression
	 * @return
	 * @throws RemoteException
	 */
	public int createSession(String expression) throws RemoteException;
	/**
	 * Liefert die erste Seite der Ergebnismenge zu der angegebenen SessionId
	 * @param sessionId
	 * @return
	 */
	public JDataSet getFirstPage(int sessionId);
	public String getFirstPageString(int sessionId);
	/**
	 * Blättert vorwärts in der Ergebnismenge
	 * @param sessionId
	 * @return
	 */
	public JDataSet getNextPage(int sessionId);
	public String getNextPageString(int sessionId);
	/**
	 * Blättert rückwärts in der Ergebnismenge
	 * @param sessionId
	 * @return
	 */
	public JDataSet getPrevPage(int sessionId);
	public String getPrevPageString(int sessionId);
	/**
	 * Speichert den übergebenen Content
	 * @param ds
	 * @return
	 */
	public int updateObjects(JDataSet ds);
	/**
	 * Importiert durch White Space getrennte Eigenschaften.<p> 
	 * Hierbei können die Items auch in Anführungszeichen eingeschlossen
	 * sein: "Meine Eigenschaft".
	 * @param text
	 * @param lowercase Wenn true wird Rudi zu rudi.
	 */
	public void importItems(String text, boolean lowercase);
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
	public void indexObject(int oid,String text,boolean createItems,boolean lowercase);
	/**
	 * Startet den experimentelle Spider.<br>
	 * In der Konfigurationsdatei ist festgelegt, 
	 * welches Direktory durchsucht wird
	 * und welche File-Extensions berücksichtigt werden.
	 */
	public void startSpider();
	/**
	 * Importiert Objekte in die Datenbasis;
	 * Objekte werden zeilenweise erwartet; 
	 * die Eigenschaften der Objekte durch white space getrennt.
	 * @param data
	 * @return Anzahl importierter Objekte
	 */
	public int importDatabaseCSV(String content);
	public int importDatabaseDataset(JDataSet data);
	public String getItemCacheStatistics();
	/**
	 * Indiziert die Datenbasis; der Index (BitZaun) wird aus den Objekten neu aufgebaut
	 */
	public void indexDatabase();
	public String sayHello();
	public String echo(String s);

}