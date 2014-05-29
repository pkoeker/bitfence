package de.pk86.bf;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.remoting.rmi.RmiServiceExporter;

@Configurable
public class BfServerConfig {
	private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BfServerConfig.class);
	private static int registryPort = 1098;
	private static int servicePort = 1099;
	private static String registryHost = "localhost";
	private static Registry registry;

	public static void setRegistryPort(int registryPort) {
		BfServerConfig.registryPort = registryPort;
	}

	public static void setServicePort(int servicePort) {
		BfServerConfig.servicePort = servicePort;
	}

	public static void setRegistryHost(String registryHost) {
		BfServerConfig.registryHost = registryHost;
	}

	private static ApplicationContext context;

	public static ApplicationContext getContext() {
		if (context == null) {
			synchronized (BfServerConfig.class) {
				if (context == null)
					context = new AnnotationConfigApplicationContext(BfServerConfig.class);
			}
		}
		return context;
	}

	public BfServerConfig() {
		if (!"localhost".equalsIgnoreCase(registryHost)) {
			System.setProperty("java.rmi.server.hostname", registryHost); // Für abweichende IP-Adresse
		}
	}

	private Registry getRegistry() {
		if (registry == null) {
			synchronized (BfServerConfig.class) {
				if (registry == null) {
					try {
						registry = LocateRegistry.createRegistry(registryPort);
					} catch (RemoteException ex) {
						ex.printStackTrace();
						logger.error(ex.getMessage(), ex);
						return null;
					}
				}
			}
		}
		return registry;
	}
	
	// RMI
	@Bean
	private ObjectItemServiceIF srv_bf() {
		ObjectItemServiceIF srv = new ObjectItemSpringService();
		// srv.setSessionManager(getSessionManager());
		RmiServiceExporter exp = new RmiServiceExporter();
		exp.setService(srv);
		exp.setServiceInterface(ObjectItemServiceIF.class);
		exp.setServiceName(ObjectItemServiceIF.class.getName());
		Registry r = getRegistry();
		exp.setRegistry(r);
		if (servicePort > 0)
			exp.setServicePort(servicePort);
		try {
			exp.afterPropertiesSet();
			return srv;
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error(ex.getMessage(), ex);
			return null;
		}
	}
}
