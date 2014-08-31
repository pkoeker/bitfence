package de.pk86.bf;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
/**
 * @deprecated
 */
public class BfServletContextListener implements ServletContextListener {
	
	int xx = 0;
	public BfServletContextListener() {
		xx++;
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		xx++;
	}
	@Override
   public void contextDestroyed(ServletContextEvent arg0) {
		xx++;
   }


}
