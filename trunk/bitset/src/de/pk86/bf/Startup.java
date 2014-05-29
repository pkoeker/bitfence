package de.pk86.bf;

import org.springframework.context.ApplicationContext;

/**
 * @author peter
 */
public class Startup {

	public static void main(String[] args) {
		ObjectItemServiceImpl sv = new ObjectItemServiceImpl();
		try {
			ApplicationContext actx = BfServerConfig.getContext();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
