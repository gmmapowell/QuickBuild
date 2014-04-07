package com.gmmapowell.quickbuild.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.CommandObjectFactory;
import com.gmmapowell.parser.Parent;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.CopyDirectoryCommand;
import com.gmmapowell.quickbuild.build.CopyResourceCommand;
import com.gmmapowell.quickbuild.build.ImageMagickConvertCommand;
import com.gmmapowell.quickbuild.build.ImageMagickLauncherIcon;
import com.gmmapowell.quickbuild.build.ImageMagickNotificationIcon;
import com.gmmapowell.quickbuild.build.ftp.DistributeCommand;
import com.gmmapowell.quickbuild.build.ftp.DistributeSeparatelyCommand;
import com.gmmapowell.quickbuild.build.ftp.DistributeWrapCommand;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.exceptions.QBConfigurationException;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class ConfigFactory implements CommandObjectFactory {
	private Map<String, Constructor<? extends Parent<?>>> handlers = new HashMap<String, Constructor<? extends Parent<?>>>();
	private Map<String, Class<? extends Nature>> natureClasses = new HashMap<String, Class<? extends Nature>>();
	private Map<Class<? extends Nature>, Nature> natures = new HashMap<Class<? extends Nature>, Nature>();

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
		addCommandExtension("buildif", BuildIfCommand.class);

		addCommandExtension("context", LibraryContextCommand.class);
		addCommandExtension("directory", DirectoryResourceCommand.class);
		addCommandExtension("resource", ResourceCommand.class);
		addCommandExtension("produces", ProducesCommand.class);
		addCommandExtension("readsFile", ReadsFileCommand.class);
		addCommandExtension("readsDir", ReadsFileCommand.class);

		// standard build commands
		addCommandExtension("copydir", CopyDirectoryCommand.class);
		addCommandExtension("copy", CopyResourceCommand.class);
		
		// this should be in some other nature, but save me now!
		addCommandExtension("distribute", DistributeCommand.class);
		addCommandExtension("wrap", DistributeWrapCommand.class);
		addCommandExtension("separately", DistributeSeparatelyCommand.class);
		addCommandExtension("images", ImageMagickConvertCommand.class);
		addCommandExtension("launcher", ImageMagickLauncherIcon.class);
		addCommandExtension("notification", ImageMagickNotificationIcon.class);
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
			throw new QuickBuildException("Cannot understand command '" + cmd + "'");
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
		try {
			addCommandHandler(cmd, clz);
		} catch (Throwable t) {
			// could not install the extension
			System.out.println("Could not install extension " + clz);
		}
	}

	public void addCommandHandler(String cmd, Class<? extends Parent<?>> handler)
	{
		try {
			if (handlers.containsKey(cmd))
			{
				if (handler == handlers.get(cmd).getDeclaringClass())
					return;
				throw new QBConfigurationException("Duplicate command handler: " + cmd);
			}
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
		if (natureClasses.containsKey(cmd))
			throw new QuickBuildException("Cannot add duplicate nature name " + cmd);
		natureClasses.put(cmd, clz);
		try {
			Method method = clz.getMethod("init", ConfigFactory.class);
			if (method != null)
				method.invoke(clz, this);
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends Nature> T getNature(Config config, Class<T> cls)
	{
		if (natures.containsKey(cls))
			return (T)natures.get(cls);
		try
		{
			T n = cls.getConstructor(Config.class).newInstance(config);
			natures.put(cls, n);
			return n;
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public Collection<Nature> installedNatures() {
		return natures.values();
	}

	public void done() {
		for (Nature n : natures.values())
			n.done();
	}

	public Set<String> availableNatures() {
		return natureClasses.keySet();
	}

	public Class<? extends Nature> natureClass(String s) {
		return natureClasses.get(s);
	}

	public boolean usesNature(Class<? extends Nature> cls) {
		return natures.containsKey(cls);
	}

	@Override
	public void handleError(TokenizedLine toks, Exception error) {
		// TODO Auto-generated method stub
		
	}
}

