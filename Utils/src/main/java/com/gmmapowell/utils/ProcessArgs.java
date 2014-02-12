package com.gmmapowell.utils;

import java.util.ArrayList;
import java.util.Collection;
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
		ArgumentDefinition pendingField = null;
		loop:
		while (++i < args.length)
		{
			if (pendingField != null) {
				applyArgument(config, argcount, pendingField, args[i]);
				pendingField = null;
				continue loop;
			}
			if (args[i].startsWith("--"))
			{
				for (ArgumentDefinition ad : argumentDefinitions)
					if (ad.text.equals(args[i]))
					{
						Class<?> type = Reflection.getFieldVar(config.getClass(), ad.toVar).getType();
						if (!type.equals(Boolean.class) && !type.equals(boolean.class))
						{
							pendingField = ad;
							continue loop;
						}
						// if option starts with "--no-", set the value to "false", otherwise "true"
						// it's legal for the option list to include both, pointing to the same var
						boolean val = !(ad.text.startsWith("--no-"));
						Reflection.setField(config, ad.toVar, val);
						argcount.op(ad, 1, new FuncR1<Integer, Integer>() {
							@Override
							public Integer apply(Integer arg) {
								return arg+1;
							}});
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
							applyArgument(config, argcount, ad, val);
							continue loop;
						}
				}
				for (ArgumentDefinition ad : argumentDefinitions)
					if (ad.cardinality == Cardinality.REQUIRED_ALLOW_FLAGS)
					{
						Reflection.setField(config, ad.toVar, args[i]);
						argcount.op(ad, 1, new FuncR1<Integer, Integer>() {
							@Override
							public Integer apply(Integer arg) {
								return arg+1;
							}});
						continue loop;
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
						Class<?> type = Reflection.getFieldVar(config.getClass(), ad.toVar).getType();
						if (!type.equals(Boolean.class) && !type.equals(boolean.class))
						{
							pendingField = ad;
							continue loop;
						}
						Reflection.setField(config, ad.toVar, true);
						argcount.op(ad, 1, new FuncR1<Integer, Integer>() {
							@Override
							public Integer apply(Integer arg) {
								return arg+1;
							}});
						continue loop;
					}
				}
				for (ArgumentDefinition ad : argumentDefinitions)
					if (ad.cardinality == Cardinality.REQUIRED_ALLOW_FLAGS)
					{
						Reflection.setField(config, ad.toVar, args[i]);
						argcount.op(ad, 1, new FuncR1<Integer, Integer>() {
							@Override
							public Integer apply(Integer arg) {
								return arg+1;
							}});
						continue loop;
					}
				throw new UtilException("There is no option definition for " + args[i]);
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

		if (pendingField != null)
		{
			error(errors, pendingField, "did not have a value provided");
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

	private static void applyArgument(Object config,
			StateMap<ArgumentDefinition, Integer> argcount, ArgumentDefinition ad,
			String val) {
		Class<?> clz = Reflection.fieldType(config, ad.toVar);
		if (Collection.class.isAssignableFrom(clz)) {
			for (String s : val.split(ad.splitChar))
				if (s != null && s.length() > 0)
					Reflection.setField(config, ad.toVar, s); // append to collection
		} else
			Reflection.setField(config, ad.toVar, val);
		argcount.op(ad, 1, new FuncR1<Integer, Integer>() {
			@Override
			public Integer apply(Integer arg) {
				return arg+1;
			}});
	}

	private static void error(List<String> errors, ArgumentDefinition ad, String msg) {
		errors.add("The argument " + ad.message + " (" + ad.text + ") " + msg);
	}

}
