package com.gmmapowell.bytecode;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;

public class ConcatExpr extends Expr {

	private final List<Expr> args = new ArrayList<Expr>();

	public ConcatExpr(MethodCreator meth, Object[] args) {
		super(meth);
		for (Object o : args)
		{
			if (o instanceof Expr)
				this.args.add((Expr)o);
			else if (o instanceof String)
				this.args.add(new StringConstExpr(meth, (String)o));
			else
				throw new UtilException("It is not currently possible to pass a " + o.getClass() + " directly to concat");
		}
	}

	@Override
	public void spitOutByteCode(MethodCreator meth) {
		String string = "java.lang.String";
		String stringBuilder = "java.lang.StringBuilder";
		meth.newObject(stringBuilder);
		meth.dup();
		boolean first = true;
		for (Expr o : args)
		{
			o.spitOutByteCode(meth);
			if (first)
			{
				if (!o.getType().equals(string))
					meth.invokeVirtualMethod(o.getType(), string, "toString");
				meth.invokeOtherConstructor(stringBuilder, string);
			}
			else
			{
				String atype = "java.lang.Object";
				if (o.getType().equals(string))
					atype = o.getType();
				meth.invokeVirtualMethod(stringBuilder, stringBuilder, "append", atype);
			}
			first = false;
		}
		meth.invokeVirtualMethod(stringBuilder, string, "toString");
	}

	@Override
	public String getType() {
		return "java.lang.String";
	}

}
