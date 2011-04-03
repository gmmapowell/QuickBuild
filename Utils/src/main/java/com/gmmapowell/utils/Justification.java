package com.gmmapowell.utils;

public enum Justification {
	LEFT, RIGHT, PADLEFT_TRUNCRIGHT, PADRIGHT_TRUNCLEFT;

	public String format(String text, int len) {
		if (text == null)
			text = "";
		if (len < 0)
			return text;
		int tlen = text.length();
		if (tlen < len)
		{
			switch (this)
			{
			case LEFT:
			case PADLEFT_TRUNCRIGHT:
				return text + pad(len-tlen);
			case RIGHT:
			case PADRIGHT_TRUNCLEFT:
				return pad(len-tlen) + text;
			}
		}
		else
		{
			switch (this)
			{
			case LEFT:
			case PADRIGHT_TRUNCLEFT:
				return text.substring(0, len);
			case RIGHT:
			case PADLEFT_TRUNCRIGHT:
				return text.substring(tlen-len);
			}
		}
		return null;
	}

	static String pad(int len) {
		char[] cs = new char[len];
		for (int i=0;i<len;i++)
			cs[i] = ' ';
		return new String(cs);
	}
}
