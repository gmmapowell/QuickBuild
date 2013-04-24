package com.gmmapowell.collections;

import java.util.Iterator;

import com.gmmapowell.exceptions.UtilException;

public class PushBackIterator<T> implements Iterator<T> {
	private T prev;
	private T pushed;
	private final Iterator<T> basedOn;

	public PushBackIterator(Iterator<T> iterator) {
		this.basedOn = iterator;
	}

	@Override
	public boolean hasNext() {
		return pushed != null || basedOn.hasNext();
	}

	@Override
	public T next() {
		if (pushed != null) {
			prev = pushed;
			pushed = null;
		} else
			prev = basedOn.next();
		return prev;
	}

	@Override
	public void remove() {
		basedOn.remove();
	}

	public void pushBack() {
		if (pushed != null)
			throw new UtilException("Only one item may be pushed back");
		pushed = prev;
	}
}
