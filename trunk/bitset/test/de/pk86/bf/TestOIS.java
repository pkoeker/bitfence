package de.pk86.bf;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestOIS /*extends TestCase*/ {
	private ObjectItemServiceIF sv = new ObjectItemServiceWrapper("test");
	
	@Test public void testItem1() {
		String itemname = "xxx.zzz";
		if (sv.hasItem(itemname)) {
			sv.deleteItem(itemname);
		}
		sv.createItem(itemname);
		boolean b = sv.hasItem(itemname);
		assertEquals(true, b);
		int cnt = sv.getItemCount(itemname);
		assertEquals(0, cnt);
		sv.deleteItem(itemname);
		b = sv.hasItem(itemname);
		assertEquals(false, b);		
	}
	
	
	@Test public void testObjectItem2() {
		sv.createItem("123.xxx");
		sv.createItem("123.yyy");
		sv.createItem("123.zzz");
		String[] items = sv.findItems("123.%");
		assertEquals(3, items.length);
		sv.deleteItem("123.%");
		items = sv.findItems("123.%");
		assertEquals(0, items.length);
	}
	public void testValid() {
		//sv.validate();
	}
}
