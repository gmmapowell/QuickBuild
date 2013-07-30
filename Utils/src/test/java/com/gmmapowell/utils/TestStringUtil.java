package com.gmmapowell.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

public class TestStringUtil extends TestCase {

	public void testJoinCollectionOfQextendsObject() {
		List<String> items = Arrays.asList("Dog", "Cat", "Hamster", "Fish", "Bird");
		assertEquals("Dog Cat Hamster Fish Bird", StringUtil.join(items));
		
		Collection<Integer> set = new HashSet<Integer>();
		set.add(1);
		set.add(2);
		set.add(3);
		set.add(4);
		assertEquals("1 2 3 4", StringUtil.join(set));
	}

	public void testJoinCollectionOfQextendsObjectString() {
		List<String> items = Arrays.asList("Dog", "Cat", "Hamster", "Fish", "Bird");
		assertEquals("Dog, Cat, Hamster, Fish, Bird", StringUtil.join(items, ", "));

		Collection<Integer> set = new HashSet<Integer>();
		set.add(1);
		set.add(2);
		set.add(3);
		set.add(4);
		assertEquals("1-2-3-4", StringUtil.join(set, "-"));
	}

}
