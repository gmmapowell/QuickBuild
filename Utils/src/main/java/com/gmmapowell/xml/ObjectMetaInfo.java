package com.gmmapowell.xml;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.gmmapowell.exceptions.UtilException;

// I would expect this to be capable of handling sensible defaults
class ObjectMetaInfo {

	private final Object callbacks;
	private Map<String, MethodMetaInfo> callbackTable = new HashMap<String, MethodMetaInfo>();
	public final boolean wantsText;

	public ObjectMetaInfo(Object callbacks) {
		this.callbacks = callbacks;
		Class<?> clz = callbacks.getClass();
		XMLWants annotation = clz.getAnnotation(XMLWants.class);
		if (annotation != null)
		{
			if (annotation.value() == XMLWant.ELEMENTS)
				wantsText = false;
			else
			{
				if (!(callbacks instanceof XMLTextReceiver))
					throw new UtilException("The class " + clz + " cannot receive text because it does not implement XMLTextReceiver");
				wantsText = true;
			}
		}
		else
			wantsText = false;
		for (Method m : clz.getDeclaredMethods())
		{
			// System.out.println(m);
			Class<?>[] ptypes = m.getParameterTypes();
			if (m.getReturnType().equals(Object.class) && ptypes.length == 1 && ptypes[0].equals(XMLElement.class))
				callbackTable.put(m.getName().toLowerCase(), new MethodMetaInfo(m));
		}
				
	}

	public Object dispatch(XMLElement xe) {
		// There should be ordering checks
		// There should be an indirection table
		// There should be checks that the method exists
		
		if (callbacks instanceof XMLElementReceiver)
		{
			return ((XMLElementReceiver)callbacks).receiveElement(xe);
		}
		String tag = xe.tag().toLowerCase();
		if (!callbackTable.containsKey(tag))
			throw new UtilException("The tag " + tag + " is not valid for the handler " + callbacks);
		try
		{
			return callbackTable.get(tag).method.invoke(callbacks, xe);
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}
}

class MethodMetaInfo {
	public final Method method;
	
	public MethodMetaInfo(Method m)
	{
		method = m;
		method.setAccessible(true);
	}
}
