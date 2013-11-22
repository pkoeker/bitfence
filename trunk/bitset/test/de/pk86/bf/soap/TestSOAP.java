package de.pk86.bf.soap;

import org.junit.Test;

public class TestSOAP {
	@Test public void getService() {
		Bitset bs = new Bitset();
		ObjectItemSOAPService srv = bs.getBitset();
		long start = System.currentTimeMillis();
		int sid = srv.createSession("berlin | münchen");
		System.out.println(sid);
		for (int i = 0; i < 10; i++) {
			String p = srv.getNextPageString(sid);
			//System.out.println(p);
			System.out.print('.');
		}
		srv.endSession(sid);
		long end = System.currentTimeMillis();
		System.out.println("\n" + (end -start));
	}
}
