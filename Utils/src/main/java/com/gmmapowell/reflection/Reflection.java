package com.gmmapowell.reflection;

import java.lang.reflect.Field;
import java.util.Collection;

import com.gmmapowell.exceptions.UtilException;

public class Reflection {

	@SuppressWarnings("unchecked")
	public static <T> T getField(Object target, String fieldName) {
		try
		{
			if (target == null)
				throw new UtilException("Cannot use reflection on null object");
			if (fieldName == null)
				throw new UtilException("Must specify a valid field name");
			Class<?> clz = target.getClass();
			Field f = findField(clz, fieldName);
			if (f == null)
				throw new UtilException("The field '" + fieldName +"' was not defined in " + target.getClass());
			f.setAccessible(true);

			return (T) f.get(target);
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	@SuppressWarnings("unchecked")
	public static void setField(Object target, String fieldName, Object value) {
		try
		{
			if (target == null)
				throw new UtilException("Cannot use reflection on null object");
			if (fieldName == null)
				throw new UtilException("Must specify a valid field name");
			Class<?> clz = target.getClass();
			Field f = findField(clz, fieldName);
			if (f == null)
				throw new UtilException("The field '" + fieldName +"' was not defined in " + target.getClass());
			f.setAccessible(true);
			if (value instanceof Boolean)
				f.setBoolean(target, (Boolean)value);
			else if (value == null || f.getType().isAssignableFrom(value.getClass()))
				f.set(target, value);
			else if (value != null && Collection.class.isAssignableFrom(f.getType()))
				((Collection<Object>)f.get(target)).add(value);
			else
				throw new UtilException("The field " + fieldName + " is not assignable from " + value.getClass());
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	private static Field findField(Class<?> clz, String fieldName) {
		try
		{
			return clz.getDeclaredField(fieldName);
		}
		catch (NoSuchFieldException fex)
		{
			if (clz.getSuperclass() != null)
				return findField(clz.getSuperclass(), fieldName);
			return null;
		}
	}
}
