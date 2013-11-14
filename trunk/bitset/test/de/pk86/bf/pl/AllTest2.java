package de.pk86.bf.pl;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.pk86.bf.ObjectItemServiceIF;
import de.pk86.bf.TestExecute;
import de.pk86.bf.TestImport;
import de.pk86.bf.TestOIS;
import de.pk86.bf.TestSession;
import de.pk86.bf.client.ServiceFactory;

@RunWith(Suite.class)
@SuiteClasses({
	TestImport.class,
	TestObjekt.class,
	TestPL.class,
	TestOIS.class,
	TestExecute.class,
	TestSession.class,
	TestRepair.class

})

public class AllTest2 {
	private static BfPL pl;

	
	public static BfPL getPL() {
		if (pl == null) {
		  pl = BfPL.getInstance();
		}
		return pl;
	}
	public static ObjectItemServiceIF getService() {
		ObjectItemServiceIF srv = ServiceFactory.getDirectService();
		return srv;
	}

}
