package de.pk86.bf.pl;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.pk86.bf.TestImport;
import de.pk86.bf.TestOIS;

@RunWith(Suite.class)
@SuiteClasses({
	TestImport.class,
	TestObjekt.class,
	TestPL.class,
	TestOIS.class

})

public class AllTest2 {
	private static BfPL pl;

	
	public static BfPL getPL() {
		if (pl == null) {
		  pl = BfPL.getInstance();
		}
		return pl;
	}
	

}
