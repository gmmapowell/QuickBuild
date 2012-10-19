package com.gmmapowell.jsgen;


public class ForEachStmt extends AbstractForStmt {
	private final JSExpr over;

	public ForEachStmt(JSScope scope, String var, JSExpr over) {
		super(scope, var);
		this.over = over;
		// TODO: "idx" should be randomly/uniquely generated
		nestedBlock().add(new Assign(getLoopVar(), new ArrayIndex(over, scope.getVarLike("idx")), true));
	}

	@Override
	public void constructFor(JSBuilder sb) {
		sb.append("for (idx=0;idx<");
		over.toScript(sb);
		sb.append(".length;idx++)");
	}

}