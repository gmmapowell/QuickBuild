package com.gmmapowell.test;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.gmmapowell.reflection.Reflection;

public class QBJUnitRunner {
	public static void main(String... args)
	{
		JUnitListener lsnr = new JUnitListener();
		RunNotifier nfy = new RunNotifier();
		nfy.addFirstListener(lsnr);
		boolean ignoreQIs = false;
		Class<? extends Annotation> qiclz = null;
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> tmp = (Class<? extends Annotation>) Class.forName("org.zinutils.test.QuickIgnore");
			qiclz = tmp;
		} catch (Exception ex) { }
		for (String arg : args) { 
			if (arg.equals("--quick")) {
				System.out.println("Enabling @QuickIgnore");
				ignoreQIs = true;
				continue;
			}
			try {
				Class<?> clz = Class.forName(arg);
				Description desc = Description.createSuiteDescription(clz);
				lsnr.testRunStarted(desc);
				Annotation qi = clz.getAnnotation(QuickIgnore.class);
				if (qi == null && qiclz != null)
					qi = clz.getAnnotation(qiclz);
				RunWith runWith = clz.getAnnotation(RunWith.class);
				Ignore ign = clz.getAnnotation(Ignore.class);
				if (ign != null || (ignoreQIs && qi != null)) {
					nfy.fireTestIgnored(desc);
					continue;
				} else if (runWith != null) {
					Runner suite = Reflection.create(runWith.value(), clz, new AllDefaultPossibilitiesBuilder(true));
					suite.run(nfy);
				} else
					new BlockJUnit4ClassRunner(clz).run(nfy);
			} catch (ClassNotFoundException e) {
				System.out.println("There was no class " + arg + " found");
				e.printStackTrace(System.err);
				lsnr.failed++;
			} catch (InitializationError e) {
				System.out.println("Class " + arg + " failed to start: " + e.getMessage());
				e.printStackTrace(System.err);
				lsnr.failed++;
			} catch (Throwable e) {
				System.out.println("Class " + arg + " encountered run exception: " + e.getMessage());
				e.printStackTrace(System.err);
				lsnr.failed++;
			}
			finally {
				try { lsnr.testRunFinished(null); } catch (Exception ex) { ex.printStackTrace(); }
			}
		}
		
		System.out.println("Active Threads:");
		Set<Thread> threads = new HashSet<Thread>(Thread.getAllStackTraces().keySet());
		int counter = 1;
		for (Thread t : threads)
			if (t.isAlive() && !t.isDaemon())
				System.out.println(counter++ +". " + t.getName());
		System.out.println("Daemon Threads:");
		for (Thread t : threads)
			if (t.isAlive() && t.isDaemon())
				System.out.println(counter++ +". " + t.getName());
		
		System.out.println("Summary: ran " + lsnr.runCount + ": " + lsnr.failed + " failed, " + lsnr.ignored + " ignored");
		// Restrict the exit code to be in a range 0-100 to avoid conflict with 128+
		int exitStatus = lsnr.failed;
		if (exitStatus > 100)
			exitStatus = 100;
		System.exit(exitStatus);
	}
}
