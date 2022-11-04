package test.quickbuild.dependencies.javabuild;

import java.io.File;

//import static org.junit.Assert.*;

import org.junit.Test;

import com.gmmapowell.quickbuild.app.QuickBuild;

public class SingleJavaBuild {
	@Test
	public void testASingleJavaBuild() {
		File dir = new File(System.getProperty("user.dir"), "samples");
		QuickBuild.main(new String[] { "--debugInternals", new File(dir, "qb/java1.qb").toString() });
	}
}
