package com.gmmapowell.jsgen;


public class ForEachStmt extends AbstractForStmt {
	private final JSExpr over;

	public ForEachStmt(Var takes, JSExpr over) {
		super(takes);
		this.over = over;
		// TODO: "idx" should be randomly/uniquely generated
		nestedBlock().add(new Assign(takes, new ArrayIndex(over, new Var("idx")), true));
	}

	@Override
	public void constructFor(JSBuilder sb) {
		sb.append("for (idx=0;idx<");
		over.toScript(sb);
		sb.append(".length;idx++)");
	}

}