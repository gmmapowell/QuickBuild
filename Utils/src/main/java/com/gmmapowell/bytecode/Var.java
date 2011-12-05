package com.gmmapowell.bytecode;

import com.gmmapowell.exceptions.UtilException;

public abstract class Var extends Expr{
	protected final int id;
	protected final String clz;
	private int argpos = -1;

	protected Var(MethodCreator meth, String clz, String name) {
		super(meth);
		id = meth.nextLocal();
		this.clz = clz;
	}

	protected Var(MethodCreator meth, int id, String name) {
		super(meth);
		this.clz = meth.getClassName();
		this.id = id;
	}
	
	@Override
	public String getType() {
		return clz;
	}

	public int argPos()
	{
		if (argpos == -1)
			throw new UtilException("It was not an argument");
		return argpos;
	}
	public abstract void store();
	
	public static class AVar extends Var {

		public AVar(MethodCreator meth, String clz, String name) {
			super(meth, clz, name);
		}
		
		private AVar(MethodCreator meth) {
			super(meth, 0, "this");
		}

		public static AVar myThis(MethodCreator meth)
		{
			return new AVar(meth); 
		}
		
		@Override
		public void spitOutByteCode(MethodCreator meth) {
			meth.aload(id);
		}
		
		public void store() {
			meth.astore(id);
		}
	}

	public Var setArgument(int i) {
		argpos = i;
		return this;
	}

	/*
	 * I don't think I actually want this, but I may want something similar ...
	public static class StorePop extends Expr {
		private final Var ret;

		public StorePop(MethodCreator methodCreator, Var ret) {
			super(methodCreator);
			this.ret = ret;
		}

		@Override
		public void spitOutByteCode(MethodCreator meth) {
			ret.store(meth);
		}
	}
	*/

}
