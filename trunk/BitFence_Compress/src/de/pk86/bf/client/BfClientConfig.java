package de.pk86.bf.client;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.rmi.RmiClientInterceptor;

import de.pk86.bf.ObjectItemServiceIF;


@Configuration
public class BfClientConfig {
  private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BfClientConfig.class);
  
  private static ApplicationContext context;
  private static String host = "localhost:1098";
  
  public static ApplicationContext getContext(String h) {
	 host = h;
    if (context == null) {
      synchronized (BfClientConfig.class) {
        if (context == null)
          context = new AnnotationConfigApplicationContext(BfClientConfig.class);        
      }
    }
    return context;
  }
  // RMI
  @Bean
  public ObjectItemServiceIF getObjectItemServiceIF() {
	 ObjectItemServiceIF srv = null;
    RmiClientInterceptor rmi = new RmiClientInterceptor();
    rmi.setServiceInterface(ObjectItemServiceIF.class);
    rmi.setServiceUrl("rmi://" + host + "/" + ObjectItemServiceIF.class.getName());
    rmi.setRefreshStubOnConnectFailure(true);
    rmi.afterPropertiesSet();
    srv = ProxyFactory.getProxy( ObjectItemServiceIF.class, rmi );
    return srv;    
  }  
}
