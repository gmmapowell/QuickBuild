package com.gmmapowell.test;

import java.lang.reflect.Modifier;
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
import org.zinutils.reflection.Reflection;

public class QBJUnitRunner {
	public static void main(String... args)
	{
		JUnitListener lsnr = new JUnitListener();
		RunNotifier nfy = new RunNotifier();
		nfy.addFirstListener(lsnr);
		boolean ignoreQIs = false;
		boolean xmlFile = false;
		for (String arg : args) {
			if (xmlFile) {
				lsnr.toXML(arg);
				xmlFile = false;
				continue;
			} else if (arg.equals("--quick")) {
				System.out.println("Enabling @QuickIgnore");
				ignoreQIs = true;
				continue;
			} else if (arg.equals("--xml")) {
				xmlFile = true;
				continue;
			} else try {
				Class<?> clz = Class.forName(arg);
				if (Modifier.isAbstract(clz.getModifiers())) {
					System.out.println("Ignoring abstract test class " + clz);
					continue;
				}
				Description desc = Description.createSuiteDescription(clz);
				lsnr.testRunStarted(desc);
				QuickIgnore qi = clz.getAnnotation(QuickIgnore.class);
				RunWith runWith = clz.getAnnotation(RunWith.class);
				Ignore ign = clz.getAnnotation(Ignore.class);
				if (ign != null || (ignoreQIs && qi != null)) {
					nfy.fireTestIgnored(desc);
					continue;
				} else if (runWith != null) {
					Runner suite;
					try {
						suite = Reflection.create(runWith.value(), clz, new AllDefaultPossibilitiesBuilder());
					} catch (RuntimeException e) {
						suite = Reflection.create(runWith.value(), clz);
					}
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
				System.out.println("Summary of " + arg + ": ran " + (lsnr.runCount-lsnr.startRun) + "; " + (lsnr.failed-lsnr.startFailed) + " failed; " + (lsnr.ignored-lsnr.startIgnored) + " ignored");
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
		
		// Restrict the exit code to be in a range 0-100 to avoid conflict with 128+
		int exitStatus = lsnr.failed;
		if (exitStatus > 100)
			exitStatus = 100;
		System.exit(exitStatus);
	}
}
