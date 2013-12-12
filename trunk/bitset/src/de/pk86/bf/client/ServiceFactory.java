package de.pk86.bf.client;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.context.ApplicationContext;

import de.pk86.bf.ObjectItemService;
import de.pk86.bf.ObjectItemServiceIF;
import de.pk86.bf.soap.Bitset;
import de.pk86.bf.ObjectItemSOAPService;

/**
 * Erzeugt einen ObjectItemService auf Basis von SOAP oder Spring-RMI
 * @author peter
 */
public class ServiceFactory {	
	private static ObjectItemService srv;
	private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ServiceFactory.class);

	public static ObjectItemServiceIF getDirectService() {
		if (srv == null) {
			synchronized (ServiceFactory.class) {
				if (srv == null) {
					srv = new ObjectItemService();
				}
			}
		}
		return srv;	      
	}
	/**
	 * localhost:1098
	 * @return
	 */
	public static ObjectItemServiceIF getSpringService() {
		return getRmiService("localhost:1098");
	}
	public static ObjectItemServiceIF getSpringService(String host) {
		return getRmiService(host);
	}
	private static ObjectItemServiceIF getRmiService(String host) {
		ApplicationContext context = BfClientConfig.getContext(host); // Erzwingt init
		ObjectItemServiceIF srv = context.getBean(ObjectItemServiceIF.class);		
		return srv;
	}
	// http://pk86.de/bitdemo/soap?wsdl
	public static ObjectItemSOAPService getSOAPService(String host) {
		if (host == null) {
			host = "http://pk86.de/bitdemo/soap?wsdl";
		}
		Bitset bs;
      try {
	      bs = new Bitset(new URL(host));
	      de.pk86.bf.ObjectItemSOAPService srv = bs.getBitset();
	      return srv;
      } catch (MalformedURLException e) {
	      e.printStackTrace();
	      logger.error(e.getMessage(), e);
	      throw new IllegalStateException("Unable to create service: " + e.getMessage(), e);
      }
	}
	
}
