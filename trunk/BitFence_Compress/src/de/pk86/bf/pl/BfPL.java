package de.pk86.bf.pl;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import net.sf.ehcache.CacheManager;

import org.apache.log4j.xml.DOMConfigurator;

import de.jdataset.JDataRow;
import de.jdataset.JDataSet;
import de.jdataset.JDataValue;
import de.jdataset.ParameterList;
import de.pk86.bf.Const;
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
//	private static String username;
//	private static String password;
	//private Connection con;
	// Config
	public static int maxSlot = 26; // 13 = 106.496; 26 = 212 992 
	private static long maxOid = maxSlot * Const.SLOT_BITS -1;
	private static int maxResultSet = 20000;
	private static int resultSetPage = 20;
	// Statements
	// Object
	private String insertObject = "INSERT INTO OBJEKT Values(?,?)";
	private String deleteObject = "DELETE FROM OBJEKT WHERE oid = ?";
	//private String getMaxOid = "SELECT MAX(oid)+1 FROM OBJEKT";	
	// ObjectItem
	private String findOIs = "SELECT Itemname FROM OBJEKTITEM WHERE oid = ? ORDER BY Itemname";	
	private String insertOI = "INSERT INTO OBJEKTITEM Values(?, ?)";
	private String deleteOI = "Delete FROM OBJEKTITEM WHERE oid = ? AND Itemname = ?";
	private String deleteItemOIs = "Delete FROM OBJEKTITEM WHERE Itemname = ?";
	private String deleteItemOIsLike = "Delete FROM OBJEKTITEM WHERE Itemname LIKE ?";
	private String deleteObjectOIs = "Delete FROM OBJEKTITEM WHERE oid = ?";
	// Item
	private String findItems = "SELECT Itemname FROM ITEM WHERE Itemname LIKE ? ORDER BY Itemname";
	private String selectItem = "SELECT Itemname FROM ITEM WHERE Itemname = ?";
	private String insertItem = "INSERT INTO ITEM Values(?)";
	private String updateItem = "UPDATE ITEM SET Itemname = ? WHERE Itemname = ?";
	private String deleteItem = "Delete FROM ITEM WHERE Itemname = ?";	
	private String deleteItemLike = "Delete FROM ITEM WHERE Itemname LIKE ?";	
	private String countItemBits = "SELECT SUM(bitCount) as ANZ FROM SLOT WHERE Itemname = ?";	
	// Slot
	private String selectSlot = "SELECT Bits, BitCount FROM SLOT WHERE Itemname = ?";
	private String getAllSlots = "SELECT Bits, BitCount FROM SLOT WHERE Itemname = ?";
	private String insertSlot = "INSERT INTO SLOT Values(?, ?, ?)";
	private String updateSlot = "UPDATE SLOT SET Bits = ?, BitCount = ? WHERE Itemname = ?";
	private String deleteSlot = "Delete FROM SLOT WHERE Itemname = ?";
	private String deleteSlots = "Delete FROM SLOT WHERE Itemname = ?";
	private String deleteSlotsLike = "Delete FROM SLOT WHERE Itemname LIKE ?";
	//
	private Element webServiceConfig;
	private Element spiderConfig;
	
	private PL pl;
	
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
					try {
						DOMConfigurator.configure("bf_log4j.xml");
					} catch (Exception ex) {}
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
//			conn = this.getConnection("deleteObject", false);
//			ps1 = conn.getConnection().prepareStatement(deleteObject);
//			ps1.setLong(1, oid);
//			int cnt = ps1.executeUpdate();
			if (cnt1 != 1) {				
				throw new IllegalArgumentException("PL#deleteObject: DELETE Failed");
			}
			// ObjectItem
			int cnt2 = ipl.executeSql(deleteObjectOIs, list);
//			ps2 = conn.getConnection().prepareStatement(deleteObjectOIs);
//			ps2.setLong(1, oid);
//			int cnt2 = ps2.executeUpdate();
			logger.debug("deleteObjectOIs: " + cnt2);
			// Bits
			String[] items = this.getObjectItems(oid, ipl);
			for (int i = 0; i < items.length; i++) {
				String itemname = items[i];
				Item.removeBit(oid, itemname, ipl);
			}
			ipl.commitTransaction(transname);
			return cnt1 + cnt2;
		} catch (PLException ex) {
//			if (conn != null) {
//				conn.rollbackTransaction("deleteObject");
//			}
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
//		} finally {
//			if (ps1 != null) {
//				ps1.close();
//			}
//			if (ps2 != null) {
//				ps2.close();
//			}
//			if (conn != null) {
//				conn.close("ok");
//			}
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
//		long ret = -1;
//		//BfConnection conn = null;
//		//PreparedStatement ps = null;
//		try {
//			//conn = this.getConnection("getMaxOid", true);
//			//ps = conn.getConnection().prepareStatement(getMaxOid);
//			//ResultSet rs = ps.executeQuery();
//			
//			if (rs.next()) {
//				ret = rs.getLong(1);
//				ret++;
//				if (ret > getMaxOid()) {
//					throw new IllegalStateException("OID out of range.");
//				}
//			} else {
//				throw new IllegalStateException("Unable to get OID");
//			}
//		} catch (SQLException ex) {
//			throw ex;
//		} finally {
//			if (ps != null) {
//				ps.close();
//			}
//			if (conn != null) {
//				conn.close("ok");
//			}
//		}
//		return ret;
	}
	// ObjectItems ##########################################
	public void addObjectItem(long oid, String itemname) throws Exception {
//		BfConnection conn = null;
//		PreparedStatement ps = null;
		String transname = "insertOI";
		IPLContext ipl = pl.startNewTransaction(transname);
		try {
			//conn = this.getConnection("insertOI", false);
			boolean set = Item.setBit(oid, itemname, ipl);
			if (set) {
				ParameterList list = new ParameterList();
				list.addParameter("oid", oid);
				list.addParameter("itemname", itemname);
				int cnt = ipl.executeSql(insertOI, list);
//				ps = conn.getConnection().prepareStatement(insertOI);
//				ps.setLong(1, oid);
//				ps.setString(2, itemname);
//				int cnt = ps.executeUpdate();
				if (cnt != 1) {
					throw new IllegalArgumentException("PL#addObjectItem: INSERT failed");
				}
			} else {
				 // TODO: was, wenn nicht? Bit schon gesetzt?
				logger.warn("Bit schon gesetzt? " + oid + "/" + itemname);
			}
			ipl.commitTransaction(transname);
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
//		} finally {
//			if (ps != null) {
//				ps.close();
//			}
//			if (conn != null) {
//				conn.close("ok");
//			}
		}
	}
	public void removeObjectItem(long oid, String itemname) throws Exception {
//		BfConnection conn = null;
//		PreparedStatement ps = null;
		String transname = "deleteOI";
		IPLContext ipl = pl.startNewTransaction(transname);
		try {
			ParameterList list = new ParameterList();
			list.addParameter("oid", oid);
			list.addParameter("itemname", itemname);
			int cnt = ipl.executeSql(deleteOI, list);
//			conn = this.getConnection("deleteOI", false);
//			ps = conn.getConnection().prepareStatement(deleteOI);
//			ps.setLong(1, oid);
//			ps.setString(2, itemname);
//			int cnt = ps.executeUpdate();
			if (cnt != 1) {
				throw new IllegalArgumentException("PL#removeObjectItem: DELETE failed");
			}
			// Bits
			Item.removeBit(oid, itemname, ipl);
			ipl.commitTransaction(transname);
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
//		} finally {
//			if (ps != null) {
//				ps.close();
//			}
//			if (conn != null) {
//				conn.close("ok");
//			}
		}
	}
	public String[] getObjectItems(long oid, IPLContext ipl) throws Exception {
		String[] ret = null;
		//ArrayList<String> al = new ArrayList<String>();
		//PreparedStatement ps = null;
		try {
			ParameterList list = new ParameterList();
			list.addParameter("oid", oid);
			JDataSet ds;
			if (ipl != null) {
				ds = ipl.getDatasetSql("OI", findOIs, list);
			} else {
				ds = pl.getDatasetSql("OI", findOIs, list);
			}
//			ps = conn.getConnection().prepareStatement(findOIs);
//			ps.setLong(1, oid);
//			ResultSet rs = ps.executeQuery();
//			while (rs.next()) {
//				String s = rs.getString(1);
//				al.add(s);
//			}
			ret = new String[ds.getRowCount()];
			Iterator<JDataRow> it = ds.getChildRows();
			int i = 0;
			if (it != null) {
				while(it.hasNext()) {
					JDataRow row = it.next();			
					ret[i] = row.getValue("itemname");
					i++;
				}
			}
		} catch (PLException ex) {
			throw ex;
//		} finally {
//			if (ps != null) {
//				ps.close();
//			}
		}
		return ret;	
	}
	
	public String[] getObjectItems(long oid) throws Exception {
		String[] ret = null;
//		BfConnection conn = null;
//		conn = this.getConnection("findOIs", true);
		ret = this.getObjectItems(oid, null);
		return ret;
	}
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
//		PreparedStatement stmt = null;
		try {
			String sql = "SELECT DISTINCT itemname, COUNT(itemname) AS anz FROM ObjektItem WHERE oid IN(" + bo.toString()+")";
			if (sbi != null) {
				sql = sql + " AND itemname NOT IN("+sbi+")";
			}
			sql = sql + " group by itemname order by anz desc";
			//BfConnection conn = this.getConnection("getOtherItems", true);
			JDataSet ds = pl.getDatasetSql("otherItems", sql);
			Iterator<JDataRow> it = ds.getChildRows();
			if (it != null) {
				while (it.hasNext()) {
					JDataRow row = it.next();
					ret.add(row.getValue("itemname"));
				}
			}
//			stmt = conn.getConnection().prepareStatement(sql);
//			ResultSet rs  = stmt.executeQuery();
//			while (rs.next()) {
//				String item = rs.getString(1);
//				ret.add(item);
//			}
//			stmt.close();
		} catch (PLException ex) {
			throw ex;	
//		} finally {
//			if (stmt != null) {
//				stmt.close();
//			}
		}
		return ret;
	}
	// Items #############################################
	public String[] findItems(String pattern) throws Exception {
		String[] ret = null;
		//ArrayList<String> al = new ArrayList<String>();
//		BfConnection conn = null;
//		PreparedStatement ps = null;
		try {
			ParameterList list = new ParameterList();
			list.addParameter("pattern", pattern);
			JDataSet ds = pl.getDatasetSql("findItems", findItems, list);
//			conn = this.getConnection("findItems", true);
//			ps = conn.getConnection().prepareStatement(findItems);			
//			ps.setString(1, pattern);
//			ResultSet rs = ps.executeQuery();
//			while (rs.next()) {
//				String s = rs.getString(1);
//				al.add(s);
//			}
//			rs.close();
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
//			for (int i = 0; i < al.size(); i++) {
//				ret[i] = (String)al.get(i);
//			}
		} catch (PLException ex) {
			throw ex;
//		} finally {
//			if (ps != null) {
//				ps.close();
//			}
//			if (conn != null) {
//				conn.close("ok");
//			}
		}
		
		return ret;
	}
	public int createItem(String name) throws Exception{
//		BfConnection conn = null;
//		PreparedStatement ps = null;
		try {
			ParameterList list = new ParameterList();
			list.addParameter("name", name);
			int cnt = pl.executeSql(insertItem, list);
//			conn = this.getConnection("createItem", true);
//			ps = conn.getConnection().prepareStatement(insertItem);
//			ps.setString(1, name);
//			int cnt = ps.executeUpdate();
			if (cnt != 1) {
				throw new IllegalArgumentException("PL#createItem: INSERT Failed");
			}
			return cnt;
		} catch (PLException ex) {
			throw ex;
//		} finally {
//			if (ps != null) {
//				ps.close();
//			}
//			if (conn != null) {
//				conn.close("ok");
//			}
		}
	}
	public int deleteItem(String name) throws Exception {
//		BfConnection conn = null;
//		PreparedStatement ps1 = null;
//		PreparedStatement ps2 = null;
//		PreparedStatement ps3 = null;
		IPLContext ipl = null;
		String transname = "deleteItem";
		try {
			// Item
			ParameterList list = new ParameterList();
			list.addParameter("name", name);
			ipl = pl.startNewTransaction(transname);
//			conn = this.getConnection("deleteItem", false);
			// Delete all Child Slots (CASCADE?)
			int cnt1;
			int cnt2;
			int cnt3;
			if (name.indexOf("%") != -1) {
				cnt2 = ipl.executeSql(deleteSlotsLike, list);
				cnt3 = ipl.executeSql(deleteItemOIsLike, list);
				cnt1 = ipl.executeSql(deleteItemLike, list);
//				ps1 = conn.getConnection().prepareStatement(deleteItemLike);
//				ps2 = conn.getConnection().prepareStatement(deleteSlotsLike);
//				ps3 = conn.getConnection().prepareStatement(deleteItemOIsLike);
			} else {
				cnt2 = ipl.executeSql(deleteSlots, list);
				cnt3 = ipl.executeSql(deleteItemOIs, list);
				cnt1 = ipl.executeSql(deleteItem, list);
//				ps1 = conn.getConnection().prepareStatement(deleteItem);
//				ps2 = conn.getConnection().prepareStatement(deleteSlots);				
//				ps3 = conn.getConnection().prepareStatement(deleteItemOIs);
			}
//			ps2.setString(1, name);
//			int cnt2 = ps2.executeUpdate();
//			// ObjectItem (CASCADE?)
//			ps3.setString(1, name);
//			int cnt3 = ps3.executeUpdate();
//			// Item erst zuletzt löschen
//			ps1.setString(1, name);
//			int cnt = ps1.executeUpdate();
//			if (cnt != 1) {
//				throw new IllegalArgumentException("PL#deleteItem: DELETE failed");
//			}

			ipl.commitTransaction(transname);	
			return cnt1 + cnt2 + cnt3;
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
//		} finally {
//			if (ps1 != null) {
//				ps1.close();
//			}
//			if (ps2 != null) {
//				ps2.close();
//			}
//			if (ps3 != null) {
//				ps3.close();
//			}
//			if (conn != null) {
//				conn.close("ok");
//			}
		}
	}
	/**
	 * @param oldItemname
	 * @param newItemname
	 * @return Anzahl der geänderten Datensätze
	 */
	public int renameItem(String oldItemname, String newItemname)  throws Exception {
		// Erst Constraint löschen?
		// Drop Constraint ist bei InnoDB leider nicht implementiert!!!
		// Workaround: Alles nach neu kopieren, dann alt löschen
		String drop = "ALTER TABLE ObjektItem DROP Constraint Item_ObjektItem";
		String upd1 = "UPDATE ObjektItem set Itemname = ? WHERE Itemname = ?";
		String upd2 = "UPDATE Item set Itemname = ? WHERE Itemname = ?";
		String crea = "ALTER TABLE ObjektItem ADD Constraint Item_ObjektItem foreign key (ITEMNAME) references ITEM (ITEMNAME) ON DELETE CASCADE";
//		BfConnection conn = null;
//		PreparedStatement st1 = null;
//		PreparedStatement ps1 = null;
//		PreparedStatement ps2 = null;
//		PreparedStatement st2 = null;
		String transname = "renameItem";
		IPLContext ipl = pl.startNewTransaction(transname);
		try {
			ipl.executeSql(drop);
			ParameterList list = new ParameterList();
			list.addParameter("newItemname", newItemname);
			list.addParameter("oldItemname", oldItemname);
			int anz1 = ipl.executeSql(upd1, list);
			int anz2 = ipl.executeSql(upd2, list);
			ipl.executeSql(crea);
//			conn = this.getConnection("renameItem", false);
//			st1 = conn.getConnection().prepareStatement(drop);
//			st1.executeUpdate();
//			
//			ps1 = conn.getConnection().prepareStatement(upd1);
//			ps1.setString(1, newItemname);
//			ps1.setString(2, oldItemname);
//			int anz1 = ps1.executeUpdate();
//			
//			ps2 = conn.getConnection().prepareStatement(upd2);
//			ps2.setString(1, newItemname);
//			ps2.setString(2, oldItemname);
//			int anz2 = ps2.executeUpdate();
//
//			st2 = conn.getConnection().prepareStatement(crea);
//			st2.executeUpdate();

			ipl.commitTransaction(transname);
			return anz1 + anz2;
		} catch (PLException ex) {
			if (ipl != null) {
				ipl.rollbackTransaction(transname);
			}
			throw ex;
//		} finally {
//			if (ps1 != null) {
//				ps1.close();
//			}
//			if (ps2 != null) {
//				ps2.close();
//			}
//			if (st1 != null) {
//				st1.close();
//			}
//			if (st2 != null) {
//				st2.close();
//			}
//			if (conn != null) {
//				conn.close("ok");
//			}
		}
	}
	public boolean hasItem(String itemname) throws Exception {
		boolean ret = false;
//		BfConnection conn = null;
//		PreparedStatement ps = null;
		try {
			ParameterList list = new ParameterList();
			list.addParameter("itmname", itemname);
			JDataSet ds = pl.getDatasetSql("hasItem", selectItem, list);
			if (ds.getRowCount() == 1) 
				return true;
//			conn = this.getConnection("selectItem", true);
//			ps = conn.getConnection().prepareStatement(selectItem);
//			ps.setString(1, itemname);
//			ResultSet rs = ps.executeQuery();
//			if (rs.next()) {
//				ret = true;
//			}
		} catch (PLException ex) {
			throw ex;
//		} finally {
//			if (ps != null) {
//				ps.close();
//			}
//			if (conn != null) {
//				conn.close("ok");
//			}
		}
		return ret;
	}
	public boolean hasItem(long oid, String itemname) throws Exception {
//		BfConnection conn = null;
//		try {
//			conn = this.getConnection("hasItem", true);
			boolean b = Item.testBit(oid, itemname, pl);
			return b;
//		} finally {
//			if (conn != null) {
//				conn.close("ok");
//			}
//		}
	}
	public int getItemCount(String itemname) throws Exception {
		int ret = -1;
//		BfConnection conn = null;
//		PreparedStatement ps = null;
		try {
			ParameterList list = new ParameterList();
			list.addParameter("itemname", itemname);
			JDataSet ds = pl.getDatasetSql("countItemBits", countItemBits, list);
			JDataRow row = ds.getChildRow(0);
			ret = row.getValueInt("anz");
//			conn = this.getConnection("countItemBits", true);
//			ps = conn.getConnection().prepareStatement(countItemBits);
//			ps.setString(1, itemname);
//			ResultSet rs =  ps.executeQuery();
//			if (rs.next()) {
//				ret = rs.getInt(1);
//			}
		} catch (PLException ex) {
			throw ex;
//		} finally {
//			if (ps != null) {
//				ps.close();
//			}
//			if (conn != null) {
//				conn.close("ok");
//			}
		}
		return ret;
	}
	// Slots ###########################################
	public Slot selectSlot(String itemname, IPLContext ipl) throws Exception {
		Slot ret = null;
		try {
			ParameterList list = new ParameterList();
			list.addParameter("itemname", itemname);
			JDataSet ds = ipl.getDatasetSql("slot", selectSlot, list);
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
				int bitCount = row.getValueInt("bitCount");
				JDataValue val = row.getDataValue("bits");
				Object oval = val.getObjectValue();
				int ints[] = byteToInt((byte[])oval);
				ret = new Slot(itemname, ints, bitCount);
			}
		} catch (PLException ex) {
			System.out.println("SQL-Exception in PL#selectSlot: " +ex.getMessage());
			throw ex;
		}
		return ret;
	}
	public void insertSlot(Slot s, IPLContext ipl) throws Exception {
		try {
			ParameterList list = new ParameterList();
			list.addParameter("itemname", s.itemname);
			byte[] bts = intToByte(s.getBits());
			list.addParameter("bts", bts);
			list.addParameter("bitcount", s.countBits());
			int cnt = ipl.executeSql(insertSlot, list);
		} catch (PLException ex) {
			throw ex;
		}
	}
	public void updateSlot(Slot s, IPLContext ipl) throws Exception {
		try {
			ParameterList list = new ParameterList();
			byte[] bts = intToByte(s.getBits());
			list.addParameter("bts", bts);
			list.addParameter("bitcount", s.countBits());
			list.addParameter("itemname", s.itemname);
			int cnt = ipl.executeSql(updateSlot, list);
		} catch (PLException ex) {
			throw ex;
		}
	}
	public void removeSlot(Slot s, IPLContext ipl) throws Exception {
		//PreparedStatement ps = null;
		try {
			ParameterList list = new ParameterList();
			list.addParameter("itemname", s.itemname);
			int cnt = ipl.executeSql(deleteSlot, list);
		} catch (PLException ex) {
			throw ex;
		}
	}
	public int validate() throws Exception {
		int err = 0;
		System.out.println("*** start verify ***");
		{
			// 1. Check Items / Number of Bits -- BitCount
			JDataSet ds = pl.getDatasetSql("slot", "SELECT itemname, bits, bitcount FROM SLOT");
			Iterator<JDataRow> it = ds.getChildRows();
			if (it == null) return 0;
			while(it.hasNext()) {
				JDataRow row = it.next();
				String itemname = row.getValue("itemname");
				JDataValue val = row.getDataValue("bits");
				Object oval = val.getObjectValue();
				int ints[] = byteToInt((byte[])oval, false);
				int bitCount = row.getValueInt("bitcount");
				Slot slot = new Slot(itemname, ints, bitCount);
				try {
					slot.validate();
				} catch (Exception ex) {
					System.err.println("PL#validate: "+ex.getMessage());
					err++;
				}
			}	
		}
		{
			// 2. Object-Item <--> Bitcount
			//BfConnection conn = this.getConnection("validate2", true);
//			PreparedStatement psOI = conn.getConnection().prepareStatement("SELECT itemname, count(oid) AS anz from OBJEKTITEM group by itemname");
//			PreparedStatement psSlot = conn.getConnection().prepareStatement("Select sum(bitCount) AS anz from SLOT where Itemname = ?");
//			ResultSet rs = psOI.executeQuery();
			JDataSet ds1 = pl.getDatasetSql("ois", "SELECT itemname, count(oid) AS anz from OBJEKTITEM group by itemname");
			Iterator<JDataRow> it1 = ds1.getChildRows();
			if (it1 != null) {
				while(it1.hasNext()) {
					JDataRow row1 = it1.next();
					String itemname = row1.getValue("itemname");
					int anz = row1.getValueInt("anz");
					ParameterList list2 = new ParameterList();
					list2.addParameter("itemname", itemname);
					JDataSet ds2 = pl.getDatasetSql("slot","Select sum(bitCount) AS anz from SLOT where Itemname = ?",  list2);
					Iterator<JDataRow> it2 = ds2.getChildRows();
					if (it2 != null) {
						while (it2.hasNext()) {
							JDataRow row2 = it2.next();
							int cnt = row2.getValueInt("anz");
							if (cnt != anz) {
								System.err.println("BitCount != SUM(item): " + itemname);
								err++;
							}
						}
					} else {
						System.err.println("Missing Slots for: "+ itemname);
						err++;
					}
				}
			}
//			while (rs.next()) {
//				String itemname = rs.getString(1);
//				int anz = rs.getInt(2);
//				psSlot.setString(1, itemname);
//				ResultSet rss = psSlot.executeQuery();
//				if (rss.next()) {
//					int cnt = rss.getInt(1);
//					if (anz != cnt) {
//						System.err.println("BitCount != SUM(item): " + itemname);
//						err++;
//					}
//				} else {
//					System.err.println("Missing Slots for: "+ itemname);
//					err++;
//				}
//				rss.close();
//			}
//			psSlot.close();
//			psOI.close();
//			conn.close("ok");
		}
		System.out.println("*** verified ***");
		return err;
	}
	public void repair() throws Exception {
		// Slots aus den ObjectItems aufbauen.
		// 1. Alle Slots wegwerfen
		int cnt = pl.executeSql("DELETE FROM Slot");
		System.out.println("Slots delete: " + cnt);
		// Slots aus ObjectItems neu aufbauen
		JDataSet ds = pl.getDatasetSql("ois", "SELECT oid, itemname FROM ObjektItem ORDER BY oid,itemname");
		System.out.println("ObjektItems rowCount: " + ds.getRowCount());
		Iterator<JDataRow> it = ds.getChildRows();
		if (it == null) return;
		IPLContext ipl = pl.startNewTransaction("repair");
		int anz = 0;
		while(it.hasNext()) {
			JDataRow row = it.next();
			long oid = row.getValueLong("oid");
			String itemname = row.getValue("itemname");
			Item.setBit(oid, itemname, ipl);
			anz++;
			if (anz % 100 == 0) {
				System.out.println(anz);
				ipl.commitTransaction("repair");
				ipl = pl.startNewTransaction("repair");
			}
		}
		ipl.commitTransaction("repair");
	}
	
	/**
	 * Die Liste mit den Tokes wird mit den Slots ergänzt;
	 * entweder aus dem Cache oder aus der Datenbank frisch eingelesen.
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
			String sql = "SELECT Itemname, Bits, BitCount FROM SLOT WHERE Itemname IN (?) ORDER BY Itemname";
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
		ArrayList<Long> al = new ArrayList<Long>();
		for (int i = 0; i < oids.length; i++) {
			al.add(oids[i]);
		}
		String sql = "SELECT * FROM OBJEKT WHERE OID IN(?)";
		ParameterList list = new ParameterList();
		list.addParameter("oids", al);
		JDataSet ds = pl.getDatasetSql("objekt", sql, list);		
		return ds;
	}

	// STATIC ############################
	static int[] toInts(String s) {
		if (s == null) return null;
		return byteToInt(s.getBytes());
	}
	static int[] byteToInt(byte[] b) {
		return byteToInt(b, false);
	}
	static int[] byteToInt(byte[] b, boolean compressed) {
		if (b == null) return null;
		if (compressed) { // uncompress
			try {
				Inflater decomp = new Inflater();
				decomp.setInput(b);
				byte[] result = new byte[1024 * maxSlot];
				int resLength = decomp.inflate(result);
		      System.out.println("Decomp: " + resLength);
				b = Arrays.copyOfRange(result, 0, resLength);
				decomp.end();
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new IllegalStateException(ex);
			}
			
		}
		int[] ret = new int[b.length/4];
		for (int i = 0; i < ret.length; i++) {
			int value = 0;
			for (int j = 0; j < 4; j++) {
				value = (value << 8) | (b[i*4 + j] & 0xFF);
			}
			ret[i] = value;
		}
		return ret;
	}
	static byte[] intToByte(int[] in) {
		return intToByte(in, false);
	}	
	static byte[] intToByte(int[] in, boolean compressed) {
		byte[] ret = new byte[in.length * 4];
		for (int i = 0; i < in.length; i++) {
			int value = in[i];
			ret[i * 4 + 0] = (byte) (value >>> 24);
			ret[i * 4 + 1] = (byte) (value >>> 16);
			ret[i * 4 + 2] = (byte) (value >>> 8);
			ret[i * 4 + 3] = (byte) (value >>> 0);
		}
		if (compressed) { // compress
			try {											
		      Deflater compresser = new Deflater(Deflater.BEST_SPEED);
		      //Deflater compresser = new Deflater(Deflater.BEST_COMPRESSION);
		      compresser.setInput(ret);
		      compresser.finish();
		      byte[] output = new byte[1024 * maxSlot];
		      int compLenght = compresser.deflate(output);
		      ret = Arrays.copyOfRange(output, 0, compLenght);
		      System.out.println("Comp: " + compLenght);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new IllegalStateException(ex);
			}
		}
		return ret;
	}
	// INIT ################################
	private void init(String configFilename) {
		Document configDoc = null;
		Element root = null;
		try {
			configDoc = new Document(new File(configFilename));
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
			Element maxOidEle = optEle.getElement("MaxOid");
			if (maxOidEle != null) {
				String sMaxOid = maxOidEle.getAttribute("value");
				maxOid = Long.parseLong(sMaxOid);
				maxSlot = (int)maxOid / Const.SLOT_BITS;
			}
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

	public Element getWebServiceConfig() {
		return webServiceConfig;
	}
	public Element getSpiderConfig() {
		return spiderConfig;
	}
	public int getMaxSlot() {
		return maxSlot;
	}
	public long getMaxOid() {
		return maxOid;
	}
	public int getMaxResultSet() {
		return maxResultSet;
	}
	public int getResultSetPage() {
		return resultSetPage;
	}
	void setResultSetPage(int p) {
		resultSetPage = p;
	}
	
//	private BfConnection getConnection(String label, boolean autocommit) throws BfException {
//      logger.debug("get Connection: " + label);
//      BfConnection dconn = this.pool.getConnection(label, autocommit);
//      return dconn;
//	}

}
