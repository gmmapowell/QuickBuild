package com.gmmapowell.adt.avm;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.gmmapowell.adt.ADTActivity;
import com.gmmapowell.adt.ADTIntent;
import com.gmmapowell.adt.ADTLayout;
import com.gmmapowell.adt.swt.SWTADTLayout;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.StringUtil;

public class Repository {
	private static Repository repository = null;
	private Map<String, Class<ADTActivity>> activities = new HashMap<String, Class<ADTActivity>>();
	private Map<Integer, ADTLayout> layouts = new HashMap<Integer, ADTLayout>();
	
	public Repository()
	{
		synchronized(this.getClass())
		{
			if (repository != null)
				throw new UtilException("You cannot create more than one repository");
			repository = this;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void addActivity(String name) {
		System.out.println(name);
		try {
			activities.put(name, (Class<ADTActivity>) Class.forName(name));
		} catch (ClassNotFoundException e) {
			throw UtilException.wrap(e);
		}
	}

	public void startActivity(ADTIntent intent) {
		try {
			if (intent.isAbsolute())
			{
				ADTActivity act = intent.absoluteClass().newInstance();
				act.onCreate(intent.getContext());
			}
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
	}

	public void setRClass(String string) {
		try
		{
			Class<?> clz = Class.forName(string);
			System.out.println("Class = " + clz);
			for (Class<?> subclz : clz.getClasses())
			{
				String type = subclz.getSimpleName();
				for (Field f : subclz.getDeclaredFields())
				{
					String name = f.getName();
					int id = f.getInt(subclz);
					System.out.println(type + "." + name + " = " + StringUtil.hex(id, 8));
					if (type.equals("layout"))
						addLayout(id, name);
				}
			}
		}
		catch (Exception e)
		{
			throw UtilException.wrap(e);
		}
	}

	private void addLayout(int id, String name) {
		layouts.put(id, new SWTADTLayout(name));
	}

	public ADTLayout getLayout(int resId) {
		if (!layouts.containsKey(resId))
			throw new UtilException("There is no layout with id " + resId);
		return layouts.get(resId);
	}
}
