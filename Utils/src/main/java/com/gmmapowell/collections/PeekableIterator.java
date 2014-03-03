package com.gmmapowell.collections;

import java.util.Iterator;

import com.gmmapowell.exceptions.UtilException;

public class PeekableIterator<T> implements Iterator<T> {
	private Iterator<T> it;
	private T peeked;

	public PeekableIterator(Iterable<T> items) {
		this.it = items.iterator();
	}

	@Override
	public boolean hasNext() {
		return peeked != null || it.hasNext();
	}

	@Override
	public T next() {
		if (peeked != null) {
			T tmp = peeked;
			peeked = null;
			return tmp;
		}
		return it.next();
	}
	
	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

	public T peek() {
		if (peeked != null)
			return peeked;
		if (!hasNext())
			return null;
		return (peeked = it.next());
	}
	
	public void accept() {
		if (peeked == null)
			throw new UtilException("Cannot accept unpeeked argument");
		peeked = null;
	}
}
