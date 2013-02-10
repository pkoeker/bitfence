package de.pk86.bf.pl;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.ehcache.CacheManager;
import de.jdataset.JDataColumn;
import de.jdataset.JDataRow;
import de.jdataset.JDataSet;
import de.jdataset.JDataTable;
import de.jdataset.JDataValue;
import de.jdataset.ParameterList;
import de.pk86.bf.Const;
import de.pk86.bf.OperToken;
import de.pk86.bf.util.ImportObjects;
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
	//public static int maxSlot = 26; // 13 = 106.496; 26 = 212 992 
	//private static long maxOid = maxSlot * Const.SLOT_BITS -1;
	private static int maxResultSet = 20000;
	private static int resultSetPage = 20;
	// Statements
	// Object
	private String insertObject = "INSERT INTO OBJEKT Values(?,?)";
	private String deleteObject = "DELETE FROM OBJEKT WHERE oid = ?";
	//private String getMaxOid = "SELECT MAX(oid)+1 FROM OBJEKT";	
	// Item
	private String findItems = "SELECT Itemname FROM ITEM WHERE Itemname LIKE ? ORDER BY Itemname";
	private String selectItem = "SELECT Itemname FROM ITEM WHERE Itemname = ?";
	private String insertItem = "INSERT INTO ITEM Values(?,?)";
	// Slot
	private String selectSlot = "SELECT Bits FROM ITEM WHERE Itemname = ?";
	private String getAllSlots = "SELECT Bits FROM ITEM WHERE Itemname = ?";
	private String insertSlot = "INSERT INTO ITEM Values(?, ?)";
	private String updateSlot = "UPDATE ITEM SET Bits = ? WHERE Itemname = ?";
	private String deleteSlot = "Delete FROM ITEM WHERE Itemname = ?";
	//private String deleteSlots = "Delete FROM ITEM WHERE Itemname = ?";
	private String deleteSlotsLike = "Delete FROM ITEM WHERE Itemname LIKE ?";
	//
	private Element webServiceConfig;
	private Element spiderConfig;
	
	private PL pl;
	private int missing;
	//private static LinkedHashMap<String, Slot> hash = new LinkedHashMap<String, Slot>();

	
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
//					try {
//						DOMConfigurator.configure("bf_log4j.xml");
//					} catch (Exception ex) {}
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
					URL url = PL.class.getResource("/ehcache.xml");
					cacheManager = CacheManager.create(url);
				} catch (Throwable ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
		}
		return cacheManager;
	}
	
	private SlotCache sc;

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
			sc.removeAll();
			Iterator<JDataRow> itc = ds.getChildRows();
			while(itc.hasNext()) {
				JDataRow row = itc.next();
				String content = row.getValue("content");
				long oid = row.getValueLong("oid");
				this.addObjectItems(oid, content, ipl);
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
	
	public void createObject(long oid) throws Exception {
		try {
			ParameterList list = new ParameterList();
			list.addParameter("oid", oid);
			list.addParameter("content", (String)null);
			int cnt = pl.executeSql(insertObject, list);
			if (cnt != 1) {
				throw new IllegalArgumentException("PL#createObject: INSERT Failed");
			}
		} catch (PLException ex) {
			throw ex;
		}
	}
	public int deleteObject(long oid) throws Exception {
		// Start Trans
		IPLContext ipl = null;
		String transname = "deleteObject";
		try {
			// Object
			ipl = pl.startNewTransaction(transname);
			ParameterList list = new ParameterList();
			list.addParameter("oid", oid);
			int cnt1 = ipl.executeSql(deleteObject, list);
			if (cnt1 != 1) {				
				throw new IllegalArgumentException("PL#deleteObject: DELETE Failed");
			}
			// Bits
			String[] items = this.getObjectItems(oid, ipl);
			for (int i = 0; i < items.length; i++) {
				String itemname = items[i];
				removeBit(oid, itemname, ipl);
			}
			ipl.commitTransaction(transname);
			return cnt1;
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
		}
	}
	/**
	 * Siehe createObject()
	 * @return long
	 * @throws Exception
	 */
	public long getOid() throws Exception {
		// @TODO : Lücken füllen wie?
		long oid = pl.getOID();
		return oid;
	}
	// ObjectItems ##########################################
	public void addObjectItems(long oid, String content, IPLContext ipl) throws Exception {
		ArrayList<String> al = getObjectItems(content);
		for (String itemname:al) {
			setBit(oid, itemname, ipl);
		}
	}
	public void addObjectItem(long oid, String itemname) throws Exception {
		String transname = "insertOI";
		IPLContext ipl = pl.startNewTransaction(transname);
		try {
			setBit(oid, itemname, ipl);
			ipl.commitTransaction(transname);
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
		}
	}
	public void removeObjectItem(long oid, String itemname) throws Exception {
		String transname = "deleteOI";
		IPLContext ipl = pl.startNewTransaction(transname);
		try {
			// Bits
			removeBit(oid, itemname, ipl);
			ipl.commitTransaction(transname);
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
		}
	}
	
	/**
	 * @deprecated macht ganz und gar nix :-(
	 * @param oid
	 * @param ipl
	 * @return
	 * @throws Exception
	 */
	public String[] getObjectItems(long oid, IPLContext ipl) throws Exception {
		String[] ret = null;
		// TODO!
//		try {
//			ParameterList list = new ParameterList();
//			list.addParameter("oid", oid);
//			JDataSet ds;
//			if (ipl != null) {
//				ds = ipl.getDatasetSql("OI", findOIs, list);
//			} else {
//				ds = pl.getDatasetSql("OI", findOIs, list);
//			}
//			ret = new String[ds.getRowCount()];
//			Iterator<JDataRow> it = ds.getChildRows();
//			int i = 0;
//			if (it != null) {
//				while(it.hasNext()) {
//					JDataRow row = it.next();			
//					ret[i] = row.getValue("itemname");
//					i++;
//				}
//			}
//		} catch (PLException ex) {
//			throw ex;
//		}
		return ret;	
	}
	
	public String[] getObjectItems(long oid) throws Exception {
		String[] ret = null;
		ret = this.getObjectItems(oid, null);
		return ret;
	}
	/**
	 * @deprecated Das geht jetzt so nicht mehr
	 * @param oids
	 * @param items
	 * @return
	 * @throws Exception
	 */
	public ArrayList<String> getOtherItems(long[] oids, ArrayList<String> items) throws Exception {
		ArrayList<String> ret = new ArrayList<String>();
		// in oid
		StringBuilder bo = new StringBuilder();
		for (int i = 0; i < oids.length; i++) {
			bo.append(Long.toString(oids[i]));
			bo.append(",");
		}
		bo.deleteCharAt(bo.length()-1);
		// not in items
		String sbi = null;
		if (items != null && items.size() > 0) {
			StringBuilder bi = new StringBuilder();	
			for (int i = 0; i < items.size(); i++) {
				String s = (String)items.get(i);
				bi.append("'");
				bi.append(s);
				bi.append("',");
			}
			bi.deleteCharAt(bi.length()-1);
			sbi = bi.toString();
		}
		try {
			String sql = "SELECT DISTINCT itemname, COUNT(itemname) AS anz FROM ObjektItem WHERE oid IN(" + bo.toString()+")";
			if (sbi != null) {
				sql = sql + " AND itemname NOT IN("+sbi+")";
			}
			sql = sql + " group by itemname order by anz desc";
			JDataSet ds = pl.getDatasetSql("otherItems", sql);
			Iterator<JDataRow> it = ds.getChildRows();
			if (it != null) {
				while (it.hasNext()) {
					JDataRow row = it.next();
					ret.add(row.getValue("itemname"));
				}
			}
		} catch (PLException ex) {
			throw ex;	
		}
		return ret;
	}
	// Items #############################################
	public String[] findItems(String pattern) throws Exception {
		String[] ret = null;
		try {
			ParameterList list = new ParameterList();
			list.addParameter("pattern", pattern);
			JDataSet ds = pl.getDatasetSql("findItems", findItems, list);
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
	public int createItem(String name) throws Exception{
		try {
			ParameterList list = new ParameterList();
			list.addParameter("name", name);
			int cnt = pl.executeSql(insertItem, list);
			if (cnt != 1) {
				throw new IllegalArgumentException("PL#createItem: INSERT Failed");
			}
			return cnt;
		} catch (PLException ex) {
			throw ex;
		}
	}
	public int deleteItem(String name) throws Exception {
		IPLContext ipl = null;
		String transname = "deleteItem";
		try {
			// Item
			ParameterList list = new ParameterList();
			list.addParameter("name", name);
			ipl = pl.startNewTransaction(transname);
			int cnt2;
			if (name.indexOf("%") != -1) {
				cnt2 = ipl.executeSql(deleteSlotsLike, list);
			} else {
				cnt2 = ipl.executeSql(deleteSlot, list);
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
		String transname = "renameItem";
		IPLContext ipl = pl.startNewTransaction(transname);
		try {
			ParameterList list = new ParameterList();
			list.addParameter("newItemname", newItemname);
			list.addParameter("oldItemname", oldItemname);
			int anz2 = ipl.executeSql(upd2, list);

			ipl.commitTransaction(transname);
			return anz2;
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
		}
	}
	public boolean hasItem(String itemname) throws Exception {
		boolean ret = false;
		try {
			ParameterList list = new ParameterList();
			list.addParameter("itmname", itemname);
			JDataSet ds = pl.getDatasetSql("hasItem", selectItem, list);
			if (ds.getRowCount() == 1) 
				return true;
		} catch (PLException ex) {
			throw ex;
		}
		return ret;
	}
	public boolean hasItem(long oid, String itemname) throws Exception {
			boolean b = testBit(oid, itemname, pl);
			return b;
	}
	public int getItemCount(String itemname) throws Exception {
		int ret = -1;
		try {
			ParameterList list = new ParameterList();
			list.addParameter("itemname", itemname);
			JDataSet ds = pl.getDatasetSql("countItemBits", selectItem, list);
			JDataRow row = ds.getChildRow(0);
			Object oval = row.getDataValue("bits").getObjectValue();
			BitSet bs = BitSet.valueOf((byte[])oval);
			ret = bs.cardinality();
		} catch (PLException ex) {
			throw ex;
		}
		return ret;
	}
	// Slots ###########################################
	public Slot selectSlot(String itemname, IPLContext ipl) throws Exception {
		Slot ret = null;
		try {
			ParameterList list = new ParameterList();
			list.addParameter("itemname", itemname);
			JDataSet ds = ipl.getDatasetSql("item", selectSlot, list);
			if (ds.getRowCount() != 1) {
				return null;
			}
			JDataRow row = ds.getChildRow(0);
			ret = new Slot(itemname, row);
		} catch (PLException ex) {
			System.out.println("SQL-Exception in PL#selectSlot: " +ex.getMessage());
		}

		return ret;
	}
	public Slot getSlot(String itemname) throws Exception {
		Slot ret = new Slot(itemname);
		try {
			ParameterList list = new ParameterList();
			list.addParameter("itemname", itemname);
			JDataSet ds = pl.getDatasetSql("getAllSlots", getAllSlots, list);
			Iterator<JDataRow> it = ds.getChildRows();
			if (it != null && ds.getRowCount() == 1) {
				JDataRow row = it.next();
				JDataValue val = row.getDataValue("bits");
				Object oval = val.getObjectValue();
				ret = new Slot(itemname, (byte[])oval);
			}
		} catch (PLException ex) {
			System.out.println("SQL-Exception in PL#selectSlot: " +ex.getMessage());
			throw ex;
		}
		return ret;
	}
	void insertOrUpdateSlot(Slot s) throws Exception {
		String transname = "insertUpdateSlot";
		IPLContext ipl = pl.startNewTransaction(transname);
		try {
			this.insertOrUpdateSlot(s, ipl);
			ipl.commitTransaction(transname);
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
		}
	}
	void insertOrUpdateSlot(Slot s, IPLContext ipl) throws Exception {
		if (s.isInserted()) {
			this.insertSlot(s, ipl);
		} else if (s.isModified()) {
			this.updateSlot(s, ipl);
		}
	}
	
	public void insertSlot(Slot s, IPLContext ipl) throws Exception {
		try {
			ParameterList list = new ParameterList();
			list.addParameter("itemname", s.itemname);
			byte[] bts = s.getBytes();
			list.addParameter("bts", bts);
			int cnt = ipl.executeSql(insertSlot, list);
			s.setUpdated(); // reset			
		} catch (PLException ex) {
			throw ex;
		}
	}
	public void updateSlot(Slot s, IPLContext ipl) throws Exception {
		try {
			ParameterList list = new ParameterList();
			byte[] bts = s.getBytes();
			list.addParameter("bts", bts);
			list.addParameter("itemname", s.itemname);
			int cnt = ipl.executeSql(updateSlot, list);
			s.setUpdated(); // reset
		} catch (PLException ex) {
			throw ex;
		}
	}
	public void removeSlot(Slot s, IPLContext ipl) throws Exception {
		try {
			ParameterList list = new ParameterList();
			list.addParameter("itemname", s.itemname);
			int cnt = ipl.executeSql(deleteSlot, list);
			// TODO: was tun, wenn wenn cached? Eigenschaft "removed" beim Slot setzen?
		} catch (PLException ex) {
			throw ex;
		}
	}
	/**
	 * @deprecated macht nix sinnvolles
	 * @return
	 * @throws Exception
	 */
	public int validate() throws Exception {
		int err = 0;
		System.out.println("*** start verify ***");
		{
			// 1. Check Items / Number of Bits -- BitCount
			JDataSet ds = pl.getDatasetSql("slot", "SELECT itemname, bits FROM SLOT");
			Iterator<JDataRow> it = ds.getChildRows();
			if (it == null) return 0;
			while(it.hasNext()) {
				JDataRow row = it.next();
				String itemname = row.getValue("itemname");
				JDataValue val = row.getDataValue("bits");
				Object oval = val.getObjectValue();
				Slot slot = new Slot(itemname, (byte[])oval);
				try {
					//slot.validate();
				} catch (Exception ex) {
					System.err.println("PL#validate: "+ex.getMessage());
					err++;
				}
			}	
		}
		System.out.println("*** verified ***");
		return err;
	}
	/**
	 * TODO: Benötigt 3GB bei 200.000 Objekten!
	 * @throws Exception
	 */
	public void repair() throws Exception {
		// Slots aus den ObjectItems aufbauen.
		// 1. Alle Slots wegwerfen
		int cnt = pl.executeSql("DELETE FROM ITEM");
		System.out.println("Items delete: " + cnt);
		// Slots aus ObjectItems neu aufbauen
		JDataSet ds = pl.getDatasetSql("objekt", "SELECT OID, Content FROM Objekt"); // je 100.000 Objekte paketieren? Mehrere Threads?
		System.out.println("Objekt rowCount: " + ds.getRowCount());
		Iterator<JDataRow> it = ds.getChildRows();
		if (it == null) return;
		IPLContext ipl = pl.startNewTransaction("repair");
		int anzo = 0;
		int anzoi = 0;
		while(it.hasNext()) {
			JDataRow row = it.next();
			long oid = row.getValueLong("oid");
			String content = row.getValue("content");
			ArrayList<String> al = getObjectItems(content);
			anzo++;
			for(String itemname:al) {
				setBit(oid, itemname, ipl); 
				anzoi++;
				if (anzoi % 1000 == 0) {
					System.out.println(anzo + "/" +anzoi);
				}
			}
//			if (anzo > 10000) {
//				writeAll(ipl); // Cache durchschreiben erzwingen
//				ipl.commitTransaction("repair");
//				return;
//			}
		}
		writeAll(ipl); // Cache durchschreiben erzwingen
		ipl.commitTransaction("repair");
	}
	
	public static ArrayList<String> getObjectItems(String content) {
		content = content.toLowerCase();
		StringTokenizer toks = new StringTokenizer(content, Const.DEFAULT_DELIM);
		ArrayList<String> al = new ArrayList<String>();
		while(toks.hasMoreTokens()) {
			String itemname = toks.nextToken();
			al.add(itemname);
		}
		return al;
	}

	/**
	 * Die Liste mit den Tokes wird mit den Slots ergänzt;
	 * entweder aus dem Cache oder aus der Datenbank frisch eingelesen.
	 * Achtung! Wird ein Item angegeben, welches nicht existiert, bleibt slot null!
	 * @param al
	 * @throws Exception
	 */
	public void findSlots(ArrayList<OperToken> al) throws Exception {
		/* Prüfen, ob Slot im Cache bereits vorhanden; 
		 * ansonsten Slot aus der Datenbank einlesen und im Cache versenken. */
		ArrayList<String> alToks = new ArrayList<String>(al.size());
		for(OperToken ot:al) {
			boolean cached = false;		
			if (sc != null) {
				Slot cs = sc.get(ot.token);
				if (cs != null) {
					ot.slot = cs;
					cached = true;
				}
			}
			if (!cached) {
				alToks.add(ot.token);
			}
		}
		if (alToks.size() > 0) { // Alles aus dem Cache?
			/*
			 * TODO: wenn itemname mit Wildcard, dann alle Slots dazu einlesen und mit AND verknüpfen
			 */
			String sql = "SELECT Itemname, Bits FROM ITEM WHERE Itemname IN (?) ORDER BY Itemname";
			ParameterList list = new ParameterList();
			list.addParameter("Itemname", alToks);
			try {
		      JDataSet ds = pl.getDatasetSql("Slots", sql, list);
		      if (ds == null || ds.getRowCount() == 0)
		      	return;
		      for(OperToken ot:al) {
			      Iterator<JDataRow> it = ds.getChildRows();	      
			      while(it.hasNext()) {
			      	JDataRow row = it.next();
			      	String itemname = row.getValue("itemname");
			      	if (itemname.equals(ot.token)) {
			      		Slot slot = new Slot(itemname, row);
			      		ot.slot = slot;
			      		if (sc != null) {
			      			sc.put(slot);
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
	
	public String getObjekts(long[] oids) throws Exception {
		ArrayList<Long> al = new ArrayList<Long>(oids.length);
		for (int i = 0; i < oids.length; i++) {
			al.add(oids[i]);
		}
		String sql = "SELECT OID, CONTENT FROM OBJEKT WHERE OID IN(?)";
		ParameterList list = new ParameterList();
		list.addParameter("oids", al);
		try {
	      JDataSet ds = pl.getDatasetSql("objekts", sql, list);
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
	
	public JDataSet getObjectPage(long[] oids) throws Exception {
		if (oids == null) {
			return null;
		}
		ArrayList<Long> al = new ArrayList<Long>();
		for (int i = 0; i < oids.length; i++) {
			al.add(oids[i]);
		}
		String sql = "SELECT * FROM OBJEKT WHERE OID IN(?)";
		ParameterList list = new ParameterList();
		list.addParameter("oids", al);
		JDataSet ds = pl.getDatasetSql("objekt", sql, list);		
		JDataTable tbl = ds.getDataTable(); 
		JDataColumn colPK = tbl.getDataColumn("oid");
		colPK.setPrimaryKey(true); // PK fürs zurückschreiben
		return ds;
	}
	
	public int setObjectPage(JDataSet ds) throws Exception {
		if (ds == null || ds.getRowCount() == 0) return 0;
		int cnt;
		IPLContext ipl = null;
		try {
			 ipl = pl.startNewTransaction("setObjectPage");
      	 // BitZaun aktualisieren
			 Iterator<JDataRow> it = ds.getChildRows();
			 while (it.hasNext()) {
				 JDataRow row = it.next();
				 long oid = row.getValueLong("oid");
				 String oldContent = row.getDataValue("content").getOldValue();
				 String content = row.getValue("content");
				 ArrayList<String> olditems = getObjectItems(oldContent);
				 ArrayList<String> items = getObjectItems(content);
				 // TODO: Abgleichen neu/alt
				 if (row.isInserted() == false) { // keine neuen austragen
					 for(String itemname:olditems) {
						 this.removeBit(oid, itemname, ipl);					 
					 }
				 }
				 if (row.isDeleted() == false) { // keine gelöschten neu schreiben
					 for(String itemname:items) {
						this.setBit(oid, itemname, ipl);
					 }
				 }
			 }
	      cnt = ipl.setDataset(ds);
	      ipl.commitTransaction("setObjectPage");
	      return cnt;
      } catch (PLException e) {
      	logger.error(e.getMessage(), e);
      	if (ipl != null) {
      		try {
	            ipl.rollbackTransaction("setObjectPage");
            } catch (PLException e1) {
	            e1.printStackTrace();
            }
      	}
      	throw e;
      }
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
				sc = new SlotCache(scEle);
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
//	public int getMaxSlot() {
//		return maxSlot;
//	}
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
	
	
	// Methods
	private Slot findSlot(String itemname, boolean force, IPLContext ipl) throws Exception {
		Slot s = null;
		s = sc.get(itemname); // Cache
		if (s != null) 
			return s;
		missing++;
		if (missing % 100 == 0) {
			System.out.println("findSlot/Missing: " + missing + " " + itemname);
		}
		//s = hash.get(itemname); // HashMap
		s = me.selectSlot(itemname,  ipl); // DB
		if (force == true && s == null) {
			s = new Slot(itemname);
			//hash.put(itemname, s);			
		}
		if (s != null) {
			sc.put(s);
		}
		return s;		
	}
	private void writeSlot(Slot s, IPLContext ipl) throws Exception {
		if (sc != null) {
			return;
		}
		if (s.isInserted() == true) {
			me.insertSlot(s, ipl);
		} else {
			me.updateSlot(s, ipl);
		}
	}
	void setBit(long l, String itemname, IPLContext ipl) throws Exception {
		Slot s = findSlot(itemname, true, ipl);
		s.setBit(l);
		if (sc == null) {
			writeSlot(s, ipl);
		} else {
			// Cached
		}
	}
	boolean testBit(long l, String itemname, IPLContext ipl) throws Exception {
		Slot s = findSlot(itemname, false, ipl);
		if (s == null) {
			return false;		
		} else {
			return s.testBit(l);
		}
	}
	void removeBit(long l, String itemname, IPLContext ipl) throws Exception {
		Slot s = findSlot(itemname, false, ipl);
		if (s == null) {
			return;		
		} else {
			s.removeBit(l);
			me.updateSlot(s, ipl);
		}		
	}
	
	void writeAll(IPLContext ipl) throws Exception {
		//sc.removeAll();
		boolean transStarted = false;
		if (ipl == null) {
			ipl = pl.startNewTransaction("writeAll");
			transStarted = true;
		}
	   List<Slot> list = sc.getAll();
	   for (Slot slot:list) {
	   	this.insertOrUpdateSlot(slot, ipl);
	   }
	   if (transStarted) {
	   	ipl.commitTransaction("writeAll");
	   }

//		Iterator<Map.Entry<String, Slot>> it = hash.entrySet().iterator();
//		while(it.hasNext()) {
//			Map.Entry<String, Slot> entry = it.next();
//			writeSlot(entry.getValue(), ipl);
//		}
	}

}
