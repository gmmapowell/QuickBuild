package com.gmmapowell.utils;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.collections.StateMap;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.lambda.Func1R;
import com.gmmapowell.reflection.Reflection;

public class ProcessArgs {

	public static void process(Object config, ArgumentDefinition[] argumentDefinitions, String[] args) {
		if (config == null)
			throw new UtilException("Cannot store to a null config");
		StateMap<ArgumentDefinition, Integer> argcount = new StateMap<ArgumentDefinition, Integer>();
		List<String> errors = new ArrayList<String>();
		
		int i = 0;
		while (i < args.length)
		{
			if (args[i].startsWith("--"))
			{
				
			}
			else if (args[i].startsWith("-"))
			{
				
			}
			else
			{
				for (ArgumentDefinition ad : argumentDefinitions)
					if (ad.text.startsWith("-"))
						continue;
					else if (StringUtil.globMatch(ad.text, args[i]))
					{
						if (ad.cardinality.maxOfOne() && argcount.containsKey(ad))
							error(errors, ad, "cannot be duplicated");
						else
						{
							Reflection.setField(config, ad.toVar, args[i]);
							// need to save it
							argcount.op(ad, 1, new Func1R<Integer, Integer>() {
								@Override
								public Integer apply(Integer arg) {
									return arg+1;
								}});
						}
					}
			}
			i++;
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
