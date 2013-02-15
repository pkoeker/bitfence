package de.pk86.bf;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;


/**
 * @author peter
 */
public class Test {
	public static void main(String[] args) {
		ObjectItemService sv = new ObjectItemService();
		
		long start = System.currentTimeMillis();
		//System.out.println(start);
		int sessionId = sv.startSession().getSessionId();
		int cntl = sv.performOper(sessionId, "Beispiel", Selection.Oper.OR);
		while (sv.hasNext(sessionId)) {
			long[] erg = sv.getNext(sessionId);
			//System.out.println(erg.length);
		}
		sv.endSession(sessionId);
		System.out.println(System.currentTimeMillis() - start);
		
		
		/*		
		GuiFactory fact = GuiFactory.getInstance();
		try {
			//GuiWindow objWin = fact.createWindow("Object.xml");
			GuiWindow objWin = fact.createWindow("Selection.xml");
			objWin.show();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		*/
		{
			final Charset ENCODING = StandardCharsets.ISO_8859_1;
			Hashtable<String, String> hash = new Hashtable<String, String>(6000);
		try {
			{		
				sv.deleteItem("%");
				//File f = new File();
				Path path = Paths.get("/home/peter/peter_hd/Projekte/bmv/LEXD06S.TXT");
				BufferedReader buff = Files.newBufferedReader(path,ENCODING);
				String s = buff.readLine();
				long start1 = System.currentTimeMillis();
				System.out.println(start);
				int cnt = 0;
				while ( s != null) {
					String slgwID = s.substring(1,5);
					String slgw = s.substring(5);
					slgw = slgw.substring(0, slgw.length()-1).trim();
					hash.put(slgwID, slgw);
					try {
						sv.createItem(slgw);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					s = buff.readLine();
					cnt++;
				}
				System.out.println(System.currentTimeMillis() - start);
				System.out.println(cnt);
			}
			{
				Path path = Paths.get("/home/peter/peter_hd/Projekte/bmv/LEXD01.TXT");
				BufferedReader buff = Files.newBufferedReader(path,ENCODING);
				String s = buff.readLine();
				long start1 = System.currentTimeMillis();
				System.out.println(start);
				int cnt = 0;
				while ( s != null) {
					String leitID = s.substring(1,7);
					long oid = Long.parseLong(leitID);
					sv.createObject(oid);
					String slgws = s.substring(69);
					slgws = slgws.substring(0, slgws.length()-1).trim();
					for (int i = 0; i < slgws.length(); i = i+4) {
						String slgwID = slgws.substring(i,i+4);
						String item = hash.get(slgwID);
						if (item != null) {
							sv.addObjectItem(oid, item);
							cnt++;
						}
					}
					s = buff.readLine();
					cnt++;
					System.out.println(oid + "/" + cnt);
				}
				System.out.println(System.currentTimeMillis() - start);
				System.out.println(cnt);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		}
		/*
		sv.startTrans("test");
		int anz = 0;
		anz = sv.performOper("test", "Kunde", Selection.NONE);
		System.out.println("Anzahl: "+ anz);
		anz = sv.performOper("test", "bla", Selection.OR);
		System.out.println("Anzahl: "+ anz);
		long[] erg = sv.endTrans("test");
		for (int i = 0; i < erg.length; i++) {
			System.out.println(erg[i]);
		}
		//sv.addObjectItem(1,"Interessent");
		*/
		
		/*
		String[] items = sv.getObjectItems(1);
		for (int i = 0; i < items.length; i++) {
			System.out.println(items[i]);
		}
		*/
		
		/*
		sv.createObject(1);
		sv.createObject(2);
		sv.createObject(3);
		sv.createObject(4);
		sv.createItem("Kunde");
		sv.createItem("Interessent");
	
		sv.addObjectItem(1,"Kunde");
		//sv.addObjectItem(2,"Kunde");
		//sv.addObjectItem(3,"Kunde");
		sv.addObjectItem(4, "Kunde");
		sv.removeObjectItem(4, "Kunde");
		*/
		//sv.indexObject(10, "bli bla blu ble blo", true);
	}
}
