package de.pk86.bf.client;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.context.ApplicationContext;

import de.pk86.bf.ObjectItemServiceImpl;
import de.pk86.bf.ObjectItemServiceIF;
import de.pk86.bf.soap.Bitset;
import de.pk86.bf.ObjectItemSOAPService;

/**
 * Erzeugt einen ObjectItemService auf Basis von SOAP oder Spring-RMI
 * @author peter
 */
public class ServiceFactory {	
	private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ServiceFactory.class);
	private static ObjectItemServiceImpl srv;
	public static void setService(ObjectItemServiceImpl s) {
		srv = s;
	}
	public static ObjectItemServiceIF getService() {
		return srv;
	}

	public static ObjectItemServiceIF getLocalService() {
		if (srv == null) {
			synchronized (ServiceFactory.class) {
				if (srv == null) {
					srv = new ObjectItemServiceImpl(false);
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
	
	public static ObjectItemSOAPService getSOAPService(String host) {
		if (host == null) {
			host = "https://pk86.de/bitdemo/soap?wsdl";
		}
		Bitset bs;
      try {
      	URL url = new URL(host);
	      bs = new Bitset(url);
	      de.pk86.bf.ObjectItemSOAPService srv = bs.getBitset();
	      return srv;
      } catch (MalformedURLException e) {
	      e.printStackTrace();
	      logger.error(e.getMessage(), e);
	      throw new IllegalStateException("Unable to create service: " + e.getMessage(), e);
      }
	}
	
}
