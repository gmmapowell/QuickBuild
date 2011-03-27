package com.gmmapowell.lambda;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Lambda {
	public static <TR, T1> Set<TR> map(FuncR1<TR, T1> func, Set<T1> input)
	{
		Set<TR> ret = new HashSet<TR>();
		for (T1 in : input)
			ret.add(func.apply(in));
		return ret;
	}

	public static <TR, T1> List<TR> map(FuncR1<TR, T1> func, List<T1> input)
	{
		List<TR> ret = new ArrayList<TR>();
		for (T1 in : input)
			ret.add(func.apply(in));
		return ret;
	}
}
