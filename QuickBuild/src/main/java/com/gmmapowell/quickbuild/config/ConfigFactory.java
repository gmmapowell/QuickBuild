package com.gmmapowell.quickbuild.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.CommandObjectFactory;
import com.gmmapowell.parser.Parent;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.CopyDirectoryCommand;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.exceptions.QBConfigurationException;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class ConfigFactory implements CommandObjectFactory {
	private Map<String, Constructor<? extends Parent<?>>> handlers = new HashMap<String, Constructor<? extends Parent<?>>>();
	private Map<String, Class<? extends Nature>> natures = new HashMap<String, Class<? extends Nature>>();

	public ConfigFactory() {
		// These are all the config ones
		addCommandExtension("proxy", ProxyCommand.class);
		addCommandExtension("host", ProxyHostCommand.class);
		addCommandExtension("port", ProxyPortCommand.class);
		addCommandExtension("user", ProxyUserCommand.class);
		addCommandExtension("password", ProxyPasswordCommand.class);
		addCommandExtension("root", RootCommand.class);
		addCommandExtension("output", OutputCommand.class);
		addCommandExtension("path", SetPathCommand.class);
		addCommandExtension("var", SetVarCommand.class);
		addCommandExtension("libs", LibsCommand.class);

		// standard build commands
		addCommandExtension("copy", CopyDirectoryCommand.class);
	}

	@Override
	public Parent<?> create(String cmd, TokenizedLine toks) {
		if (cmd.equals("extension"))
		{
			addCommandExtension(toks);
			return new DoNothingCommand();
		}
		else if (cmd.equals("nature"))
		{
			addNature(toks);
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
			addCommandExtension(args.cmd, (Class<Parent<?>>) Class.forName(args.clz));
		} catch (ClassNotFoundException e) {
			throw new QBConfigurationException("Cannot add the extension command " + args.cmd + " because the class " + args.clz + " could not be found");
		}
	}

	public void addCommandExtension(String cmd, Class<? extends Parent<?>> clz) {
		addCommandHandler(cmd, clz);
	}

	public void addCommandHandler(String cmd, Class<? extends Parent<?>> handler)
	{
		try {
			if (handlers.containsKey(cmd))
				throw new QBConfigurationException("Duplicate command handler: " + cmd);
			Constructor<? extends Parent<?>> ctor;
			ctor = handler.getConstructor(TokenizedLine.class);
			handlers.put(cmd, ctor);
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void addNature(TokenizedLine toks) {
		Args args = new Args();
		toks.process(args,
			new ArgumentDefinition("*", Cardinality.REQUIRED, "cmd", "nature"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "clz", "class name")
		);
		try {
			addNature(args.cmd, (Class<? extends Nature>) Class.forName(args.clz));
		} catch (ClassNotFoundException e) {
			throw new QBConfigurationException("Cannot add the nature " + args.cmd + " because the class " + args.clz + " could not be found");
		}
	}

	private void addNature(String cmd, Class<? extends Nature> clz) {
		natures.put(cmd, clz);
		try {
			Method method = clz.getMethod("init", ConfigFactory.class);
			if (method != null)
				method.invoke(clz, this);
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
	}

	public Collection<Class<? extends Nature>> registeredNatures() {
		return natures.values();
	}
}

