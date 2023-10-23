package test.quickbuild.dependencies.javabuild;

import java.io.File;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;

//import static org.junit.Assert.*;

import org.junit.Test;

import com.gmmapowell.quickbuild.app.QuickBuild;
import com.gmmapowell.utils.OrderedFileList;
import com.gmmapowell.vc.VCHelper;

import test.quickbuild.scaffolding.GitHandler;

public class SingleJavaBuild {
	public @Rule JUnitRuleMockery context = new JUnitRuleMockery();
	
	@Test
	public void testASingleJavaBuild() {
		// TODO: we need to copy the files from samples, otherwise they get messed up
		File dir = new File(System.getProperty("user.dir"), "samples");
		File qbfile = new File(dir, "qb/java1.qb");
		File cqbfile = new File(dir, "qb/java1/cache/java1.qb");
		File jarmainfile = new File(dir, "qb/java1/cache/Jar.Test1.jar_main");
		File jarjarfile = new File(dir, "qb/java1/cache/Jar.Test1.jar_jar");
		GitHandler handleGit = new GitHandler();
		VCHelper mock = context.mock(VCHelper.class);
		context.checking(new Expectations() {{
			oneOf(mock).checkFiles(with(true), with(any(OrderedFileList.class)), with(cqbfile)); will(handleGit);
			oneOf(mock).checkFiles(with(false), with(any(OrderedFileList.class)), with(jarmainfile)); will(handleGit);
			oneOf(mock).checkFiles(with(false), with(aNull(OrderedFileList.class)), with(jarjarfile)); will(handleGit);
		}});
		QuickBuild.setVCHelper(mock);
		QuickBuild.main(new String[] { "--debugInternals", qbfile.toString() });
	}
}
