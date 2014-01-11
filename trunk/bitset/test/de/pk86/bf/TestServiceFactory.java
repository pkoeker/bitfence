package de.pk86.bf;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.context.ApplicationContext;

import de.pk86.bf.client.ServiceFactory;

public class TestServiceFactory {
	
	
	@Test public void testSOAP() {
		ObjectItemSOAPService srv = ServiceFactory.getSOAPService(null);
		srv.echo("äöüÄÖÜß");
	}
	@Test public void testRMI() {
		try {
			ApplicationContext actx = BfServerConfig.getContext(); // startet den Server lokal
			ObjectItemServiceIF srv = ServiceFactory.getSpringService("localhost:1098");
			srv.echo("äöüÄÖÜß");
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}
	@Test public void testAPI() {
		ObjectItemServiceIF srv = ServiceFactory.getDirectService();
		srv.echo("äöüÄÖÜß");		
	}
}
