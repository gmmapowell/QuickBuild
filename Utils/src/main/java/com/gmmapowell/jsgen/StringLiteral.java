package com.gmmapowell.jsgen;

public class StringLiteral extends JSExpr {
	private final String literal;

	public StringLiteral(String literal) {
		this.literal = literal;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append("\\\"");
		sb.append(literal);
		sb.append("\\\"");
	}

}