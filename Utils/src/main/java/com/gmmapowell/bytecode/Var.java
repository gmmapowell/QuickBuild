package com.gmmapowell.bytecode;

import com.gmmapowell.exceptions.UtilException;

public abstract class Var extends Expr {
	protected final int id;
	protected final String clz;
	private int argpos = -1;

	protected Var(MethodCreator meth, String clz, String name) {
		super(meth);
		id = meth.nextLocal();
		this.clz = clz;
	}

	public Var(MethodCreator meth, JavaType clz, String name) {
		super(meth);
		id = meth.nextLocal();
		this.clz = clz.getActual();
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

	public Var setArgument(int i) {
		argpos = i;
		return this;
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
		
		public AVar(MethodCreator meth, JavaType clz, String name) {
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

	public static class IVar extends Var {

		public IVar(MethodCreator meth, String clz, String name) {
			super(meth, clz, name);
		}
		
		private IVar(MethodCreator meth) {
			super(meth, 0, "this");
		}

		@Override
		public void spitOutByteCode(MethodCreator meth) {
			meth.iload(id);
		}
		
		public void store() {
			meth.istore(id);
		}
	}

	public static class DVar extends Var {

		public DVar(MethodCreator meth, String clz, String name) {
			super(meth, clz, name);
			meth.nextLocal(); // phantom thing
		}
		
		private DVar(MethodCreator meth) {
			super(meth, 0, "this");
			meth.nextLocal(); // phantom thing
		}

		@Override
		public void spitOutByteCode(MethodCreator meth) {
			meth.dload(id);
		}
		
		public void store() {
			meth.dstore(id);
		}
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
