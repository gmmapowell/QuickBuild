package com.gmmapowell.reflection;

import java.lang.reflect.Field;

import com.gmmapowell.exceptions.UtilException;

public class Reflection {

	public static void setField(Object target, String fieldName, Object value) {
		try
		{
			if (target == null)
				throw new UtilException("Cannot use reflection on null object");
			if (fieldName == null)
				throw new UtilException("Must specify a valid field name");
			Class<?> clz = target.getClass();
			Field f = clz.getDeclaredField(fieldName);
			if (f == null)
				throw new UtilException("Class " + clz + " does not have a field " + fieldName);
			f.setAccessible(true);
			if (value == null || f.getType().isAssignableFrom(value.getClass()))
				f.set(target, value);
			else
				throw new UtilException("The field " + fieldName + " is not assignable from " + value.getClass());
		}
		catch (NoSuchFieldException ex)
		{
			throw new UtilException("The field '" + ex.getMessage() +"' was not defined in " + target.getClass(), ex);
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

}
