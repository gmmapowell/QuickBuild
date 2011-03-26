package com.gmmapowell.quickbuild.config;

import com.gmmapowell.parser.CommandObjectFactory;
import com.gmmapowell.parser.Parent;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

public class ConfigFactory implements CommandObjectFactory {

	@Override
	public Parent<?> create(String cmd, TokenizedLine toks) {
		if (cmd.equals("proxy"))
		{
			return new ProxyCommand(toks);
		}
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
		else if (cmd.equals("output"))
		{
			return new OutputCommand(toks);
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
		else
			throw new QuickBuildException("Cannot understand command " + cmd);
	}

}
