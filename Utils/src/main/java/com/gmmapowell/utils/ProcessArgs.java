package com.gmmapowell.utils;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.collections.StateMap;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.lambda.FuncR1;
import com.gmmapowell.reflection.Reflection;

public class ProcessArgs {

	public static void process(Object config, ArgumentDefinition[] argumentDefinitions, String[] args) {
		if (config == null)
			throw new UtilException("Cannot store to a null config");
		StateMap<ArgumentDefinition, Integer> argcount = new StateMap<ArgumentDefinition, Integer>();
		List<String> errors = new ArrayList<String>();
		
		int i = -1;
		loop:
		while (++i < args.length)
		{
			if (args[i].startsWith("--"))
			{
				for (ArgumentDefinition ad : argumentDefinitions)
					if (ad.text.equals(args[i]))
					{
						Reflection.setField(config, ad.toVar, true);
						continue loop;
					}
				int idx = args[i].indexOf("="); 
				if (idx > -1)
				{
					String key = args[i].substring(0, idx);
					String val = args[i].substring(idx+1);
					for (ArgumentDefinition ad : argumentDefinitions)
						if (ad.text.equals(key))
						{
							for (String s : val.split(","))
								if (s != null && s.length() > 0)
									Reflection.setField(config, ad.toVar, s); // append to collection
							continue loop;
						}
				}
				throw new UtilException("There is no option definition for " + args[i]);
			}
			else if (args[i].startsWith("-"))
			{
				for (ArgumentDefinition ad : argumentDefinitions)
				{
					if (!ad.text.startsWith("-"))
						continue;
					if (StringUtil.globMatch(ad.text, args[i]))
					{
						Reflection.setField(config, ad.toVar, true);
						continue loop;
					}
				}
			}
			else
			{
				for (ArgumentDefinition ad : argumentDefinitions)
				{
					if (ad.text.startsWith("-"))
						continue;
					else if (StringUtil.globMatch(ad.text, args[i]))
					{
						if (ad.cardinality.maxOfOne() && argcount.containsKey(ad))
							;
						else
						{
							Reflection.setField(config, ad.toVar, args[i]);
							argcount.op(ad, 1, new FuncR1<Integer, Integer>() {
								@Override
								public Integer apply(Integer arg) {
									return arg+1;
								}});
							continue loop;
						}
					}
				}
				errors.add("There was no variable to handle " + args[i]);
			}
		}
		
		// Check all required args were specified
		for (ArgumentDefinition ad : argumentDefinitions)
		{
			if (ad.cardinality.isRequired())
				if (!argcount.containsKey(ad))
					error(errors, ad, "was required but not present");
		}
		
		if (errors.size() > 0)
			throw new UsageException(errors);
	}

	private static void error(List<String> errors, ArgumentDefinition ad, String msg) {
		errors.add("The argument " + ad.message + " (" + ad.text + ") " + msg);
	}

}
