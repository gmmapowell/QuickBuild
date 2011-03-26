package com.gmmapowell.parser;

public interface CommandObjectFactory {
	Parent<?> create(String cmd, TokenizedLine toks);
}
