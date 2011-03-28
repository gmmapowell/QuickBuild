package com.gmmapowell.utils;

import java.util.Date;

import com.gmmapowell.exceptions.UtilException;

public class DateUtils {
	public enum Format {
		hhmmss3;

		public String format(long elapsed) {
			final int millis = (int)elapsed%1000;
			final int seconds = (int) ((elapsed/1000)%60);
			final int minutes = (int) ((elapsed/60000)%60);
			final int hours = (int) ((elapsed/3600000));
			
			switch (this) {
			case hhmmss3:
			{
				return StringUtil.concat(StringUtil.digits(hours,2), ":", StringUtil.digits(minutes,2), ":", StringUtil.digits(seconds,2), ".", StringUtil.digits(millis, 3));
			}
			default:
				throw new UtilException("The format " + this + " is not handled");
			}
		}
	}

	public static String elapsedTime(Date from, Date to, Format fmt) {
		long fromMillis = from.getTime();
		long toMillis = to.getTime();
		long elapsed = toMillis-fromMillis;
		
		return fmt.format(elapsed);
	}
}
