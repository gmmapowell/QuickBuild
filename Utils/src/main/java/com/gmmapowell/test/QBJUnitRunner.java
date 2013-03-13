package com.gmmapowell.test;

import java.util.HashSet;
import java.util.Set;

import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
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
		for (String arg : args)
			try {
				Class<?> clz = Class.forName(arg);
				RunWith runWith = clz.getAnnotation(RunWith.class);
				if (runWith != null)
				{
					lsnr.testRunStarted(Description.createSuiteDescription(clz));
					Reflection.create(runWith.value(), clz, new AllDefaultPossibilitiesBuilder(true)).run(nfy);
					lsnr.testRunFinished(null);
				}
				else
				{
					lsnr.testRunStarted(Description.createSuiteDescription(clz));
					new BlockJUnit4ClassRunner(clz).run(nfy);
					lsnr.testRunFinished(null);
				}
			} catch (ClassNotFoundException e) {
				System.err.println("There was no class " + arg + " found");
				lsnr.failed++;
			} catch (InitializationError e) {
				System.err.println("Class " + arg + " failed to start: " + e.getMessage());
				lsnr.failed++;
			} catch (Throwable e) {
				System.err.println("Class " + arg + " encountered run exception: " + e.getMessage());
				lsnr.failed++;
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
