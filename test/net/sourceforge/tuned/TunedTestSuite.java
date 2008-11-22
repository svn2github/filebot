
package net.sourceforge.tuned;


import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { ByteBufferOutputStreamTest.class, PreferencesMapTest.class, PreferencesListTest.class })
public class TunedTestSuite {
	
	public static Test suite() {
		return new JUnit4TestAdapter(TunedTestSuite.class);
	}
	
}
