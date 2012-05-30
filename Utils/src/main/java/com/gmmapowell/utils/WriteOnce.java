package com.gmmapowell.utils;

import com.gmmapowell.exceptions.UtilException;

public class WriteOnce<T> {
	private boolean isSet;
	private T value;
	
	/** If the value has not already been set, this call will set it.  Once set, it cannot
	 * be set to another value
	 * @param setTo the value to store
	 */
	public void set(T setTo) {
		if (isSet)
			throw new UtilException("Cannot set a writeOnce variable more than once");
		value = setTo;
		isSet = true;
	}

	/** After all other initialization logic is complete, this can be used to set an
	 * arbitrary default value.
	 * @param value the value to set 
	 */
	public void complete(T value) {
		if (!isSet)
			set(value);
	}

	/** Once the value has been set, recover it
	 * 
	 * @return the value to recover
	 */
	public T get() {
		if (!isSet)
			throw new UtilException("Cannot read a writeOnce variable before setting it");
		return value;
	}

	/** Generic test if it has already been set
	 * 
	 * @return true if it has been set, false otherwise.
	 */
	public boolean alreadySet()  
	{
		return isSet;
	}
	
	/** After all other initialization logic is complete, calling this will ensure
	 * that if no other value has been set, the member will be set to null.
	 * 
	 * This is the same as complete(null)
	 */
	public void nullIfUnwritten() {
		if (!isSet)
		{
			value = null;
			isSet = true;
		}
	}
}
