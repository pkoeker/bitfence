package de.pk86.bf.client;

import org.springframework.context.ApplicationContext;

import de.pk86.bf.ObjectItemService;
import de.pk86.bf.ObjectItemServiceIF;
import electric.registry.Registry;

/**
 * Erzeugt einen ObjectItemService auf Basis von SOAP oder Spring-RMI
 * @author peter
 *
 */
public class ServiceFactory {

	public static ObjectItemServiceIF getDirectService() {
		ObjectItemService srv = new ObjectItemService();
		return srv;
	}
	public static ObjectItemServiceIF getSOAP_Service(String url) {
		try {
			ObjectItemServiceIF sv = (ObjectItemServiceIF) Registry.bind(url,
			      ObjectItemServiceIF.class);
			return sv;
		} catch (Exception ex) {
			System.err.println("Error Binding Service: " + ex.getMessage());
			return null;
		}
	}
	public static ObjectItemServiceIF getSOAP_Service() {
		return getSOAP_Service("http://localhost:8004/bitdemo.wsdl");
	}
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
	
}
