package com.gmmapowell.graphs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;

public class DirectedAcyclicGraph<N> {
	private HashSet<Node<N>> nodes = new HashSet<Node<N>>();
	private HashSet<Link<N>> links = new HashSet<Link<N>>();
	private Comparator<N> spanSize = new Comparator<N>() {
		@Override
		public int compare(N arg0, N arg1) {
			Node<N> lhs = find(arg0);
			Node<N> rhs = find(arg1);
			System.out.println("Comparing " + lhs.span().size() + " to " + rhs.span().size());
			if (lhs.span().size() > rhs.span().size())
				return -1;
			else if (lhs.span().size() == rhs.span().size())
				return 0;
			else
				return 1;
		}
	};
	
	public void newNode(N node) {
//		System.out.println("Adding " + node);
		if (nodes.contains(node))
			throw new UtilException("Cannot add the same node " + node + " twice");
		Node<N> n = new Node<N>(node);
		nodes.add(n);
	}
	
	public void ensure(N node) {
		if (nodes.contains(node))
			return;
		newNode(node);
	}

	public void link(N from, N to) {
		Node<N> f = find(from);
		Node<N> t = find(to);
		addLink(new Link<N>(f,t));
	}

	public void ensureLink(N from, N to) {
		Node<N> f = find(from);
		Node<N> t = find(to);
		Link<N> l = new Link<N>(f, t);
		if (links.contains(l))
			return;
		addLink(l);
	}

	private void addLink(Link<N> link) {
		System.out.println("Adding link " + link);
		if (link.to.span().contains(link.from.node))
			throw new UtilException("Adding link from " + link.from.node + " to " + link.to.node + " creates a cycle");
		links.add(link);
		link.from.addLinkFrom(link);
		link.to.addLinkTo(link);
	}
	

	private Node<N> find(N n) {
		for (Node<N> ret : nodes)
			if (ret.node.equals(n))
				return ret;
		throw new UtilException("The node " + n + " was not in the graph");
	}

	public List<N> roots()
	{
		List<N> ret = new ArrayList<N>();
		for (Node<N> n : nodes)
		{
			if (n.linksTo().size() == 0)
				ret.add(n.node);
		}
		Collections.sort(ret, spanSize);
		return ret;
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		Set<Node<N>> nodesDone = new HashSet<Node<N>>();
		Set<Link<N>> linksDone = new HashSet<Link<N>>();
		for (N root : roots())
			recurseOver(ret, nodesDone, linksDone, find(root));
		return ret.toString();
	}

	private void recurseOver(StringBuilder ret, Set<Node<N>> nodesDone,	Set<Link<N>> linksDone, Node<N> n) {
		if (nodesDone.contains(n))
			return;
		ret.append("Node " + n + " depends on:\n");
		Set<Node<N>> ineed = new HashSet<Node<N>>();
		for (Link<N> l : n.linksFrom())
		{
			ret.append("  " + l.to + "\n");
			ineed.add(l.to);
		}
		nodesDone.add(n);
		for (Node<N> child : ineed)
			recurseOver(ret, nodesDone, linksDone, child);
	}
}
