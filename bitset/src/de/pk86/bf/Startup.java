package de.pk86.bf;

import org.springframework.context.ApplicationContext;

/**
 * @author peter
 */
public class Startup {

	public static void main(String[] args) {
		ObjectItemServiceIF sv = new ObjectItemServiceWrapper("main");
		try {
			ApplicationContext actx = BfServerConfig.getContext();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
