package de.pk86.bf.soap;

import static org.junit.Assert.*;

import java.rmi.RemoteException;

import org.junit.Test;

import de.pk86.bf.client.ServiceFactory;

public class TestSOAP {
	@Test public void getService() {
		//de.pk86.bf.ObjectItemSOAPService srv = ServiceFactory.getSOAPService("http://pk86.de/bitdemo/soap?wsdl");
		de.pk86.bf.ObjectItemSOAPService srv = ServiceFactory.getSOAPService("http://localhost:8080/bitset/soap?wsdl");
		long start = System.currentTimeMillis();
		int sid;
      try {
	      sid = srv.createSession("berlin | münchen");
	      System.out.println(sid);
	      for (int i = 0; i < 10; i++) {
	      	String p = srv.getNextPageString(sid);
	      	//System.out.println(p);
	      	System.out.print('.');
	      }
	      srv.endSession(sid);
	      long end = System.currentTimeMillis();
	      System.out.println("\n" + (end -start));
      } catch (RemoteException e) {
	      e.printStackTrace();
	      fail(e.getMessage());
      }
	}
}
