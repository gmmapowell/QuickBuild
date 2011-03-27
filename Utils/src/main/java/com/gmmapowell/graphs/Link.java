package com.gmmapowell.graphs;

public class Link<N> {
	Node<N> from;
	Node<N> to;

	public Link(Node<N> f, Node<N> t) {
		from = f;
		to = t;
	}
	
	@Override
	public int hashCode() {
		return from.hashCode() ^ to.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Link<?>))
			return false;
		@SuppressWarnings("unchecked")
		Link<N> other = (Link<N>)obj;
		return other.from.node.equals(from.node) && other.to.node.equals(to.node);
	}
	@Override
	public String toString() {
		return "Link[" + from + " => " + to +"]";
	}
}
