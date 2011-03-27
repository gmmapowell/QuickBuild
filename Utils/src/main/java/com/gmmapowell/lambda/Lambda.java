package com.gmmapowell.lambda;

import java.util.HashSet;
import java.util.Set;

public class Lambda {
	public static <TR, T1> Set<TR> map(FuncR1<TR, T1> func, Set<T1> input)
	{
		Set<TR> ret = new HashSet<TR>();
		for (T1 in : input)
			ret.add(func.apply(in));
		return ret;
	}
}
