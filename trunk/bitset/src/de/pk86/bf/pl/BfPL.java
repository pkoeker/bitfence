package de.pk86.bf.pl;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import net.sf.ehcache.CacheManager;
import de.jdataset.JDataRow;
import de.jdataset.JDataSet;
import de.jdataset.ParameterList;
import de.pk86.bf.ObjectItemServiceIF;
import de.pk86.bf.OperToken;
import de.pkjs.pl.IPLContext;
import de.pkjs.pl.PL;
import de.pkjs.pl.PLException;
import electric.xml.Document;
import electric.xml.Element;
/**
 * @author peter
 */
public class BfPL {
	  private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BfPL.class);

	// Database
	private static BfPL me;
	// Config
	private static int maxResultSet = 20000;
	private static int resultSetPage = 20;
	// Statements
	// Object
	private final String insertObject = "INSERT INTO OBJEKT Values(?,?)";
	private final String findObject 	= "SELECT Content FROM Objekt WHERE oid = ?";
	private final String deleteObject = "DELETE FROM OBJEKT WHERE oid = ?";
	//private String getMaxOid = "SELECT MAX(oid)+1 FROM OBJEKT";	
	// Item
	private final String findItems = "SELECT Itemname FROM ITEM WHERE Itemname LIKE ? ORDER BY Itemname";
	// für hasItem
	//private final String findItemname = "SELECT Itemname FROM ITEM WHERE Itemname = ?";
	//private final String loadItem 	= "SELECT Itemname, Bits FROM ITEM WHERE Itemname = ?";
	private final String insertItem 	= "INSERT INTO ITEM Values(?, ?)";
	private final String updateItem 	= "UPDATE ITEM SET Bits = ? WHERE Itemname = ?";
	private final String deleteItem 	= "Delete FROM ITEM WHERE Itemname = ?";
	private final String deleteItemsLike = "Delete FROM ITEM WHERE Itemname LIKE ?";
	
	private Element webServiceConfig;
	private Element spiderConfig;
	
	private PL pl;
	private int missing; // #cache missings
	
	// Private Constructor
	private BfPL(String configFilename) {
		this.init(configFilename);
	}
	
	public static BfPL getInstance() {
		me = getInstance("BF_PLConfig.xml");
		return me;
	}
	public static BfPL getInstance(String configFilename) {
		if (me == null) {
			synchronized (BfPL.class) {
				if (me == null) {
					me = new BfPL(configFilename);
					logger.info("Database initialized: " + configFilename);
				}
			}
		}
		return me;
	}
	// Cache ########################################
	private static CacheManager cacheManager;
	  
	static CacheManager getCacheManager() {
		synchronized (CacheManager.class) {
			if (cacheManager == null) {
				try {
					//##URL url = PL.class.getResource("/ehcache.xml");
					//##cacheManager = CacheManager.create(url);
					cacheManager = CacheManager.create();
				} catch (Throwable ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
		}
		return cacheManager;
	}
	
	private ItemCache iCache;

	// Objects ###################################
	public int importObjects(JDataSet ds) throws Exception {
		if (ds == null || ds.getRowCount() == 0) return 0;		
		IPLContext ipl = pl.startNewTransaction("importObjects");
		// 1. Primary keys
		{
			long oid = pl.getOID(); 
			Iterator<JDataRow> ito = ds.getChildRows();
			while(ito.hasNext()) {
				JDataRow row = ito.next();
				row.setValue("oid", oid);
				oid++;
			}
		}
		try {
			// 2. DataSet
			int cnt = ipl.setDataset(ds);
			ds.commitChanges();
			// 3. Items
			// 3.1 Cache vorher löschen
			iCache.removeAll();
			Iterator<JDataRow> itc = ds.getChildRows();
			while(itc.hasNext()) {
				JDataRow row = itc.next();
				String content = row.getValue("content");
				long oid = row.getValueLong("oid");
				this.createObjectItems(oid, content, ipl);
			}		
			// 4. Cache durchschreiben
			this.writeAll(ipl);
			ipl.commitTransaction("importObjects");
			return cnt;
		} catch (PLException ex) {
			ipl.rollbackTransaction("importObjects");
			throw ex;
		}
	}
	/**
	 * Erzeugt ein Objekt mit dem angegebenen inhalt
	 * @param content
	 * @return Die vom System vergebene oid
	 * @throws Exception
	 */
	public long createObject(String content) throws Exception {
		long oid = pl.getOID();
		this.createObject(oid, content);
		return oid;
	}
	/**
	 * Erzeugt ein Objekt mit der angebene oid und dem angegebenen Inhalt
	 * @param oid
	 * @param content
	 * @throws Exception
	 */
	public void createObject(long oid, String content) throws Exception {
		IPLContext ipl = null;
		try {
			ipl = pl.startNewTransaction("createObject");
			ParameterList list = new ParameterList();
			list.addParameter("oid", oid);
			list.addParameter("content", content);
			int cnt = ipl.executeSql(insertObject, list);
			if (cnt != 1) {
				ipl.rollbackTransaction("createObject");
				throw new IllegalArgumentException("PL#createObject: INSERT Failed");
			} else {
				this.createObjectItems(oid, content, ipl);
				ipl.commitTransaction("createObject");
			}
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction("createObject");
			}
			throw ex;
		}
	}
	
	/**
	 * Löscht das Objekt mit der angegebenen oid
	 * @param oid
	 * @return
	 * @throws Exception
	 */
	public boolean deleteObject(long oid) throws Exception {
		IPLContext ipl = null;
		final String transName = "deleteObjekt";
		try {
			ipl = pl.startNewTransaction(transName);
			ParameterList list = new ParameterList();			
			list.addParameter("oid", oid);
			JDataSet ds = pl.getDatasetSql("Content", findObject, list);
			if (ds.getRowCount() != 1) {
				ipl.rollbackTransaction(transName);
				return false;
			}
			int cnt = pl.executeSql(deleteObject, list);
			if (cnt != 1) {
				ipl.rollbackTransaction(transName);
				return false;
			}
			// Items
			String content = ds.getRow().getValue("content");
			Set<String> al = getObjectItems(content);
			for (String itemname:al) {
				removeBit(oid, itemname, ipl);
			}			
			ipl.commitTransaction(transName);
			return true;
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transName);
			}
			throw ex;
		}
	}
	
	// ObjectItems ##########################################
	/**
	 * Der Bitzaun zu dem angegebenen neuen Objekt wird versorgt
	 * @param oid
	 * @param content
	 * @param ipl
	 * @throws Exception
	 */
	private void createObjectItems(long oid, String content, IPLContext ipl) throws Exception {
		Set<String> al = getObjectItems(content);
		for (String itemname:al) {
			setBit(oid, itemname, ipl);
		}
	}
	
	/**	 
	 * 
	 * @param oids
	 * @param items
	 * @return Map<itemname,anzahlTreffer>
	 * @throws Exception
	 */
	public Map<String,Integer> getOtherItems(int[] oids, ArrayList<String> items) throws Exception {
		LinkedHashMap<String,Integer> map = new LinkedHashMap<String,Integer>();
		try {
			String sql = "SELECT oid, content FROM Objekt WHERE oid IN(?)";
			ParameterList list = new ParameterList();
			ArrayList<Integer> al = new ArrayList<Integer>(oids.length);
			for(int i = 0; i < oids.length; i++) {
				al.add(oids[i]);
			}
			list.addParameter("oids", al);
			JDataSet ds = pl.getDatasetSql("otherItems", sql, list);
			Iterator<JDataRow> it = ds.getChildRows();
			if (it != null) {
				while (it.hasNext()) {
					JDataRow row = it.next();
					String content = row.getValue("content");
					Set<String> cItems = getObjectItems(content);
					for(String item:cItems) {
						Integer i = map.get(item);
						if (i == null) {
							map.put(item, new Integer(1));
						} else {
							map.put(item, new Integer(i.intValue()+1));
						}
					}
				}
			}
		} catch (PLException ex) {
			throw ex;	
		}
		return map;
	}
	// Items #############################################
	public String[] findItems(String pattern) throws Exception {
		String[] ret = null;
		try {
			ParameterList list = new ParameterList();
			list.addParameter("pattern", pattern);
			JDataSet ds = pl.getDatasetSql("", findItems, list);
			ret = new String[ds.getRowCount()];
			Iterator<JDataRow> it = ds.getChildRows();
			if (it != null) {
				int i = 0;
				while (it.hasNext()) {
					JDataRow row = it.next();
					ret[i] = row.getValue("itemname");
					i++;
				}
			}
		} catch (PLException ex) {
			throw ex;
		}
		
		return ret;
	}
	public void createItem(String itemname) throws Exception{
		IPLContext ipl = null;
		final String transname ="createItem";
		try {
			Item item = new Item(itemname);
			ipl = pl.startNewTransaction(transname);			
			this.insertItem(item, ipl);
			ipl.commitTransaction(transname);
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
		}
	}
	public int deleteItem(String name) throws Exception {
		IPLContext ipl = null;
		final String transname = "deleteItem";
		try {
			// Item
			ParameterList list = new ParameterList();
			list.addParameter("name", name);
			ipl = pl.startNewTransaction(transname);
			int cnt2;
			if (name.indexOf("%") != -1) {
				cnt2 = ipl.executeSql(deleteItemsLike, list);
				// TODO: Items aus Cache löschen
			} else {
				cnt2 = ipl.executeSql(deleteItem, list);
				boolean removed = iCache.remove(name);
			}
			ipl.commitTransaction(transname);	
			return cnt2;
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
		}
	}
	/**
	 * @param oldItemname
	 * @param newItemname
	 * @return Anzahl der geänderten Datensätze
	 */
	public int renameItem(String oldItemname, String newItemname)  throws Exception {
		// TODO: Content der Objekte ändern
		String upd2 = "UPDATE Item set Itemname = ? WHERE Itemname = ?";
		final String transname = "renameItem";
		IPLContext ipl = pl.startNewTransaction(transname);
		try {
			ParameterList list = new ParameterList();
			list.addParameter("newItemname", newItemname);
			list.addParameter("oldItemname", oldItemname);
			int anz2 = ipl.executeSql(upd2, list);
			boolean removed = iCache.remove(oldItemname);
			ipl.commitTransaction(transname);
			return anz2;
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
		}
	}
	public boolean hasItem(final String itemname) throws Exception {
		if (itemname == null || itemname.length() == 0) {
			return false;
		}
		boolean ret = false;
		String iName = itemname.toLowerCase();
		if (iCache != null) {
			Item item = iCache.get(iName);
			if (item != null) {
				return true;
			}
		}
		try {
			ParameterList list = new ParameterList();
			list.addParameter("itemname", iName);
			JDataSet ds = pl.getDatasetStatement("hasItem", list); 
			if (ds.getRowCount() == 1) 
				return true;
		} catch (PLException ex) {
			throw ex;
		}
		return ret;
	}
	public boolean hasItem(long oid, final String itemname) throws Exception {
		boolean b = testBit(oid, itemname, pl);
		return b;
	}
	/**
	 * Liefert die Anzahl der Objekte, die mit diesem Item verknüpft sind.
	 * Wenn das Item nicht existiert, wird 0 geliefert.
	 * @see #hasItem(String)
	 * @param itemname
	 * @return
	 * @throws Exception
	 */
	public int getItemCount(String itemname) throws Exception {
		try {
			Item item = this.loadItem(itemname);
			if (item == null) {
				return 0;
			} else {
				int ret = item.countBits();
				return ret;
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw ex;
		}
	}

	public Item loadItem(String itemname) throws Exception {
		final String transname = "loadItem";
		IPLContext ipl = pl.startNewTransaction(transname);
		try {
			Item ret = this.loadItem(itemname, false, ipl);
			ipl.commitTransaction(transname);
			return ret;
		} catch (PLException ex) {
			logger.error(ex.getMessage(), ex);
			System.out.println("SQL-Exception in PL#loadItem: " +ex.getMessage());
			if (ipl != null) {
				try {
					ipl.rollbackTransaction(transname);
				} catch (PLException pex) {
					logger.warn("Unable to rollback Transaction: " + transname, pex);
				}
			}
			throw ex;
		}
	}
	
	private Item loadItem(String itemname, IPLContext ipl) throws Exception {
		try {
			JDataSet ds = ipl.getDataset("item",  itemname);
			if (ds.getRowCount() != 1) {
				return null;
			}
			JDataRow row = ds.getChildRow(0);
			Item item = new Item(itemname, row);
			return item;
		} catch (PLException ex) {
			logger.error(ex.getMessage(), ex);
			System.out.println("SQL-Exception in PL#loadItem: " +ex.getMessage());
			throw ex;
		}
	}
	
	/**
	 * Lädt einem Item unter Angabe seines Namens.
	 * Der Item wird entweder aus dem Cache entnommen, 
	 * oder (falls dort nicht vorhanden) aus der Datenbank eingelesen und jetzt in den Cache geschrieben.
	 * @param itemname
	 * @param force wenn true wird die Neuanlage des Items in der Datenbank erzwungen
	 * @param ipl
	 * @return
	 * @throws Exception
	 */
	private Item loadItem(String itemname, boolean force, IPLContext ipl) throws Exception {
		Item item = iCache.get(itemname); // Cache
		if (item != null) 
			return item;
		missing++;
		if (missing % 100 == 0) {
			System.out.println("loadItem/#Cache Missings: " + missing + " " + itemname);
		}
		item = this.loadItem(itemname,  ipl); // DB
		if (force == true && item == null) {
			item = new Item(itemname);
		}
		if (item != null) {
			iCache.put(item);
		}
		return item;		
	}
	
	void insertOrUpdateItem(Item item) throws Exception {
		final String transname = "insertUpdateItem";
		IPLContext ipl = pl.startNewTransaction(transname);
		try {
			this.insertOrUpdateItem(item, ipl);
			ipl.commitTransaction(transname);
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
		}
	}

	private void insertOrUpdateItem(Item item, IPLContext ipl) throws Exception {
		if (item.isInserted()) {
			this.insertItem(item, ipl);
		} else if (item.isModified()) {
			this.updateItem(item, ipl);
		}
	}
	
	private int insertItem(Item item, IPLContext ipl) throws Exception {
		try {
			ParameterList list = new ParameterList();
			list.addParameter("itemname", item.itemname);
			byte[] bts = item.getBytes();
			list.addParameter("bts", bts);
			int cnt = ipl.executeSql(insertItem, list);
			item.setUpdated(); // reset
			return cnt;
		} catch (PLException ex) {
			throw ex;
		}
	}
	private void updateItem(Item item, IPLContext ipl) throws Exception {
		try {
			ParameterList list = new ParameterList();
			byte[] bts = item.getBytes();
			list.addParameter("bts", bts);
			list.addParameter("itemname", item.itemname);
			int cnt = ipl.executeSql(updateItem, list);
			item.setUpdated(); // reset
		} catch (PLException ex) {
			throw ex;
		}
	}
	
	public boolean isCached(String itemname) {
		if (iCache == null) {
			return false;
		}
		String iName = itemname.toLowerCase();
		Item item = iCache.get(iName);
		if (item != null) {
			return true;
		} else {
			return false;
		}
	}
	/**
	 * maxEntriesLocalHeap anpassen: Bei 1 Mio 25000 für ca. 6GB
	 * @throws Exception
	 */
	public void repair(int start, int stop) throws Exception {
		// 1. Alle Items wegwerfen
		//##int cnt = pl.executeSql("DELETE FROM ITEM");
		//##System.out.println("Items delete: " + cnt);
		// Items aus ObjectItems neu aufbauen
		int STEP = 50000;
		//int start = 0;
		int anzo = 0;
		int anzoi = 0;
		while (true) {
			ParameterList list = new ParameterList();
			list.addParameter("1", start);
			list.addParameter("2", start+STEP);
			JDataSet ds = pl.getDatasetSql("objekt", "SELECT OID, Content FROM Objekt WHERE OID between ? AND ?", list); 
			System.out.println("Objekt rowCount: " + ds.getRowCount());
			if (ds.getRowCount() == 0 || start > stop) {
				break;
			}
			start += STEP+1;
			Iterator<JDataRow> it = ds.getChildRows();
			if (it == null) return;
			IPLContext ipl = pl.startNewTransaction("repair");
			while(it.hasNext()) {
				JDataRow row = it.next();
				long oid = row.getValueLong("oid");
				String content = row.getValue("content");
				Set<String> al = getObjectItems(content);
				anzo++;
				for(String itemname:al) {
					setBit(oid, itemname, ipl); 
					anzoi++;
					if (anzoi % 1000 == 0) {
						System.out.println(anzo + "/" +anzoi);
					}
				}
			}
			ipl.commitTransaction("repair");
		}
		IPLContext iplc = pl.startNewTransaction("WriteCache");
		writeAll(iplc); // Cache durchschreiben erzwingen
		iplc.commitTransaction("WriteCache");
	}
	
	public void repair(ArrayList<String> itemnames) {
		
	}
	
	static Set<String> getObjectItems(String content) {
		//ArrayList<String> al = new ArrayList<String>();
		Set<String> al = new LinkedHashSet<String>();
		if (content == null) {
			return al;
		}
		content = content.toLowerCase();
		StringTokenizer toks = new StringTokenizer(content, ObjectItemServiceIF.DEFAULT_DELIM);
		while(toks.hasMoreTokens()) {
			String itemname = toks.nextToken();
			boolean b = al.add(itemname);
			if (!b) {
				// duplicate!
			}
		}
		return al;
	}

	/**
	 * Die Liste mit den Tokens wird mit den Items ergänzt;
	 * entweder aus dem Cache oder aus der Datenbank frisch eingelesen.
	 * Achtung! Wird ein Item angegeben, welches nicht existiert, bleibt slot null!
	 * @param al
	 * @throws Exception
	 */
	public void findItems(ArrayList<OperToken> al) throws Exception {
		/* Prüfen, ob Item im Cache bereits vorhanden; 
		 * ansonsten Item aus der Datenbank einlesen und im Cache versenken. */
		ArrayList<String> alToks = new ArrayList<String>(al.size());
		for(OperToken ot:al) {
			boolean cached = false;		
			if (iCache != null) {
				Item cs = iCache.get(ot.token);
				if (cs != null) {
					ot.item = cs.clone();
					cached = true;
				}
			}
			if (!cached) {
				alToks.add(ot.token);
			}
		}
		if (alToks.size() > 0) { // Alles aus dem Cache?
			/*
			 * TODO: wenn itemname mit Wildcard, dann alle Items dazu einlesen und mit AND verknüpfen
			 */
			String sql = "SELECT Itemname, Bits FROM ITEM WHERE Itemname IN (?) ORDER BY Itemname";
			ParameterList list = new ParameterList();
			list.addParameter("Itemname", alToks);
			try {
		      JDataSet ds = pl.getDatasetSql("Items", sql, list);
		      if (ds == null || ds.getRowCount() == 0)
		      	return;
		      for(OperToken ot:al) {
			      Iterator<JDataRow> it = ds.getChildRows();	      
			      while(it.hasNext()) {
			      	JDataRow row = it.next();
			      	String itemname = row.getValue("itemname");
			      	if (itemname.equalsIgnoreCase(ot.token)) {
			      		Item item = new Item(itemname.toLowerCase(), row);
			      		ot.item = item.clone();
			      		if (iCache != null) {
			      			iCache.put(item);
			      		}
			      	}
			      }
		      }
	      } catch (PLException e) {
	      	logger.error(e.getMessage(), e);
	      	throw e;
	      }
		}
	}
	public String getObjekts(int[] oids) throws Exception {
		long[] l = new long[oids.length];
		for(int i = 0; i < oids.length; i++) {
			l[i] = oids[i];
		}
		return getObjekts(l);
	}	
	public String getObjekts(long[] oids) throws Exception {
		try {
	      JDataSet ds = pl.getDataset("objekt", oids);
	      if (ds == null || ds.getRowCount() == 0) {
	      	return null;
	      }
	      Iterator<JDataRow> it = ds.getChildRows();
	      StringBuilder sb = new StringBuilder();
	      while (it.hasNext()) {
	      	JDataRow row = it.next();
	      	sb.append(row.getValue("content"));
	      	sb.append('\n'); 
	      }
	      return sb.toString();
      } catch (PLException e) {
      	logger.error(e.getMessage(), e);
      	throw e;
      }		
	}
	
	public JDataSet getObjectPage(int[] oids) throws Exception {
		if (oids == null) {
			return null;
		}
		long[] loids = new long[oids.length];
		for (int i = 0; i < oids.length; i++) {
			loids[i] = oids[i];
		}
		JDataSet ds = pl.getDataset("objekt", loids);		
		
//		ArrayList<Integer> al = new ArrayList<Integer>();
//		for (int i = 0; i < oids.length; i++) {
//			al.add(oids[i]);
//		}
//		String sql = "SELECT * FROM OBJEKT WHERE OID IN(?)";
//		ParameterList list = new ParameterList();
//		list.addParameter("oids", al);
//		JDataSet ds = pl.getDatasetSql("objekt", sql, list);		
//		JDataTable tbl = ds.getDataTable(); 
//		JDataColumn colPK = tbl.getDataColumn("oid");
//		colPK.setPrimaryKey(true); // PK fürs zurückschreiben
		return ds;
	}
	/**
	 * Schreibt veränderte (gelöscht, eingefügt, geänderter Content) Objekte in die Datenbasis zurück.
	 * Der Bitzaun wird dabei aktualisiert.
	 * @param ds Ein Dataset mit dem Namen "Objekt"; zuvor {@link JDataSet#getChanges()} ausführen
	 * @return
	 * @throws Exception
	 */
	public int updateObjects(JDataSet ds) throws Exception {
		if (ds == null || ds.getRowCount() == 0) 
			return 0;
		int cnt;
		IPLContext ipl = null;
		final String transname = "setObjectPage";
		try {
			 ipl = pl.startNewTransaction(transname);
      	 // 1. BitZaun aktualisieren
			 Iterator<JDataRow> it = ds.getChildRows();
			 while (it.hasNext()) {
				 JDataRow row = it.next();
				 this.updateRow(row, ipl);
			 }
			 // 2. Dataset schreiben
	      cnt = ipl.setDataset(ds);
	      ipl.commitTransaction(transname);
	      return cnt;
      } catch (PLException e) {
      	logger.error(e.getMessage(), e);
      	if (ipl != null) {
      		try {
	            ipl.rollbackTransaction(transname);
            } catch (PLException e1) {
	            e1.printStackTrace();
            }
      	}
      	throw e;
      }
	}
	
	private int updateRow(JDataRow row, IPLContext ipl) throws Exception {
		int cnt = 0; 
		long oid = row.getValueLong("oid");
		 String oldContent = row.getDataValue("content").getOldValue();
		 String content = row.getValue("content");
		 if (row.isDeleted() && oldContent == null) { // gelöscht, aber nicht geändert
			 oldContent = content;
		 }
		 Set<String> olditems = getObjectItems(oldContent);
		 Set<String> items = getObjectItems(content);
		 // 1.1 Wenn deleted, dann alte Werte löschen
		 if (row.isDeleted()) {
			 for(String itemname:olditems) {
				 this.removeBit(oid, itemname, ipl);
				 cnt++;
			 }
		 }
		 // 1.2 Alte Attribute austragen
		 else if (row.isInserted() == false) { // keine neuen austragen
			 for(String itemname:olditems) {
				 if (items.contains(itemname) == false) {
					 this.removeBit(oid, itemname, ipl);
					 cnt++;
				 }
			 }
		 }
		 // 1.3 Neue Werte schreiben
		 if (row.isDeleted() == false) { // keine gelöschten neu schreiben
			 for(String itemname:items) {
				 if (olditems.contains(itemname) == false) {
					 this.setBit(oid, itemname, ipl);
					 cnt++;
				 }
			 }
		 }
		return cnt;
	}
	// INIT ################################
	private void init(String configFilename) {
		Document configDoc = null;
		Element root = null;
		try {
			configDoc = this.loadFile(configFilename);
		} catch (Exception ex) {
			System.err.println(
				"Unable to read " + configFilename + "\n" + ex.getMessage());
			System.exit(1);
		}
		try {
			root = configDoc.getRoot();
			// Options
			Element optEle = root.getElement("Options");
			// MaxOid
//			Element maxOidEle = optEle.getElement("MaxOid");
//			if (maxOidEle != null) {
//				String sMaxOid = maxOidEle.getAttribute("value");
//				maxOid = Long.parseLong(sMaxOid);
////				maxSlot = (int)maxOid / Const.SLOT_BITS;
//			}
			Element maxRsEle = optEle.getElement("MaxResultSet");
			if (maxRsEle != null) {
				String sMaxRs = maxRsEle.getAttribute("value");
				maxResultSet = Integer.parseInt(sMaxRs);
			}
			Element rsPageEle = optEle.getElement("ResultSetPage");
			if (rsPageEle != null) {
				String sRsPage = rsPageEle.getAttribute("value");
				resultSetPage = Integer.parseInt(sRsPage);
			}
			// Caches
			Element scEle = optEle.getElement("SlotCache");
			if (scEle != null) {
				iCache = new ItemCache(scEle);
			}
			Element ocEle = optEle.getElement("ObjektCache");
			// TODO: ObjektCache
			// GLUE
			webServiceConfig = root.getElement("WebService");
			// Spider
			spiderConfig = root.getElement("Spider");
			// PL
			pl = new PL(configDoc); // neu

		} catch (Exception ex) {
			System.err.println(
				"Error reading " + configFilename +"\n" + ex.getMessage());
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private Document loadFile(String fileName) throws Exception {
		Document doc = null;
		try {
		// ClassLoader cl = this.getClass().getClassLoader();
		InputStream inp = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
		if (inp != null) {
			doc = new Document(inp);
			logger.debug("PLConfig File Loaded: " + fileName);
			return doc;
		} else {
			File f = new File(fileName);
			if (f.canRead() == false) {
				f = new File("../" + fileName);
			}
			doc = new Document(f);
			return doc;
		}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw ex;
		}
	}

	public Element getWebServiceConfig() {
		return webServiceConfig;
	}
	public Element getSpiderConfig() {
		return spiderConfig;
	}
//	public long getMaxOid() {
//		return maxOid;
//	}
	public int getMaxResultSet() {
		return maxResultSet;
	}
	public int getResultSetPage() {
		return resultSetPage;
	}
	void setResultSetPage(int p) {
		resultSetPage = p;
	}
	
	/**
	 * Wird nur benutzt, wenn kein Cache definiert!
	 * @param s
	 * @param ipl
	 * @throws Exception
	 */
	private void saveItem(Item s, IPLContext ipl) throws Exception {
		if (iCache != null) {
			return;
		}
		if (s.isInserted() == true) {
			this.insertItem(s, ipl);
		} else {
			this.updateItem(s, ipl);
		}
	}
	/**
	 * Verknüpft ein Objekt mit einem Item
	 * @param oid
	 * @param itemname
	 * @param ipl
	 * @throws Exception
	 */
	private void setBit(long oid, String itemname, IPLContext ipl) throws Exception {
		Item item = loadItem(itemname, true, ipl);
		item.setBit(oid);
		if (iCache == null) {
			saveItem(item, ipl);
		} else {
			// Cached
		}
	}
	/**
	 * Testet, ob das angegebene Objekt mit dem Item verknüpft ist
	 * @param oid
	 * @param itemname
	 * @param ipl
	 * @return
	 * @throws Exception
	 */
	private boolean testBit(long oid, String itemname, IPLContext ipl) throws Exception {
		Item item = loadItem(itemname, false, ipl);
		if (item == null) {
			return false;		
		} else {
			return item.testBit(oid);
		}
	}
	/**
	 * Löscht die Verknüpfung zwischen Objekt und Item
	 * @param oid
	 * @param itemname
	 * @param ipl
	 * @throws Exception
	 */
	private void removeBit(long oid, String itemname, IPLContext ipl) throws Exception {
		Item item = loadItem(itemname, false, ipl);
		if (item == null) {
			return;		
		} else {
			item.removeBit(oid);
			if (iCache == null) {
				saveItem(item, ipl);
			} else {
				// Cached
			}
		}		
	}
	
	void writeAll(IPLContext ipl) throws Exception {
		//iCache.removeAll();
		boolean transStarted = false;
		if (ipl == null) {
			ipl = pl.startNewTransaction("writeAll");
			transStarted = true;
		}
	   List<Item> list = iCache.getAll();
	   for (Item item:list) {
	   	this.insertOrUpdateItem(item, ipl);
	   }
	   if (transStarted) {
	   	ipl.commitTransaction("writeAll");
	   }
	}
	
	private Exception handleEx(Exception ex, IPLContext ipl, String transName) throws Exception {
		if (ex == null) {
			logger.warn("Missing Exception");
			return null;
		}
		logger.error(ex.getMessage(), ex);
		if (ipl != null && transName != null) {
			try {
				ipl.rollbackTransaction(transName);
			} catch (PLException pex) {
				logger.warn("Unable to rollback Transaction: "+ transName, pex);
			}
		}		
		return ex;
	}
}
