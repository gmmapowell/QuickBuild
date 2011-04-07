package com.gmmapowell.quickbuild.config;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.CommandObjectFactory;
import com.gmmapowell.parser.Parent;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.exceptions.QBConfigurationException;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class ConfigFactory implements CommandObjectFactory {
	private Map<String, Constructor<Parent<?>>> handlers = new HashMap<String, Constructor<Parent<?>>>();

	@Override
	public Parent<?> create(String cmd, TokenizedLine toks) {
		if (cmd.equals("proxy"))
		{
			return new ProxyCommand(toks);
		}
		// these should all be in a proxyOptionsProcessor ...
		else if (cmd.equals("host"))
		{
			return new ProxyHostCommand(toks);
		}
		else if (cmd.equals("port"))
		{
			return new ProxyPortCommand(toks);
		}
		else if (cmd.equals("user"))
		{
			return new ProxyUserCommand(toks);
		}
		else if (cmd.equals("password"))
		{
			return new ProxyPasswordCommand(toks);
		}
		else if (cmd.equals("root"))
		{
			return new RootCommand(toks);
		}
		else if (cmd.equals("output"))
		{
			return new OutputCommand(toks);
		}
		else if (cmd.equals("path"))
		{
			return new SetPathCommand(toks);
		}
		else if (cmd.equals("var"))
		{
			return new SetVarCommand(toks);
		}
		else if (cmd.equals("libs"))
		{
			return new LibsCommand(toks);
		}
		else if (cmd.equals("repo"))
		{
			return new RepoCommand(toks);
		}
		else if (cmd.equals("maven"))
		{
			return new MavenLibraryCommand(toks);
		}
		else if (cmd.equals("jar"))
		{
			return new JarCommand(toks);
		}
		else if (cmd.equals("android"))
		{
			return new AndroidCommand(toks);
		}
		// this should be in some android options processor
		else if (cmd.equals("use"))
		{
			return new AndroidUseLibraryCommand(toks);
		}
		else if (cmd.equals("android-jar"))
		{
			return new AndroidJarCommand(toks);
		}
		else if (cmd.equals("adbinstall"))
		{
			return new AdbInstallCommand(toks);
		}
		else if (cmd.equals("extension"))
		{
			addCommandExtension(toks);
			return new DoNothingCommand();
		}
		else if (handlers.containsKey(cmd))
		{
			try {
				return handlers.get(cmd).newInstance(toks);
			} catch (Exception e) {
				throw UtilException.wrap(e);
			}
		}
		else
			throw new QuickBuildException("Cannot understand command " + cmd);
	}

	static class Args
	{
		String cmd;
		String clz;
	}
	
	@SuppressWarnings("unchecked")
	private void addCommandExtension(TokenizedLine toks) {
		Args args = new Args();
		toks.process(args,
			new ArgumentDefinition("*", Cardinality.REQUIRED, "cmd", "command"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "clz", "class name")
		);

		try {
			addCommandHandler(args.cmd, (Class<Parent<?>>) Class.forName(args.clz));
		} catch (ClassNotFoundException e) {
			throw new QBConfigurationException("Cannot add the extension command " + args.cmd + " because the class " + args.clz + " could not be found");
		}
	}

	public void addCommandHandler(String cmd, Class<Parent<?>> handler)
	{
		try {
			if (handlers.containsKey(cmd))
				throw new QBConfigurationException("Duplicate command handler: " + cmd);
			Constructor<Parent<?>> ctor;
			ctor = handler.getConstructor(TokenizedLine.class);
			handlers.put(cmd, ctor);
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
	}
	
}
