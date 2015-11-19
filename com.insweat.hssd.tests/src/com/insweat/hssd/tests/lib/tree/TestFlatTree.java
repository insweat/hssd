package com.insweat.hssd.tests.lib.tree;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.insweat.hssd.lib.tree.TreeLike;
import com.insweat.hssd.lib.tree.flat.Tree;

public class TestFlatTree extends TestTreeBase{

	@Override
	protected TreeLike newTree() {
		return new Tree();
	}
	
	@Before
	public void setUp() throws Exception {
		setupInvariantTree();
	}

	@After
	public void tearDown() throws Exception {
		releaseInvariantTree();
	}

	@Test
	public final void testConstruction() {
		doTestConstruction();
	}

	@Test
	public final void testInsertion() {
		doTestInsertion();
	}
	
	@Test
	public final void testRemoval() {
		doTestRemoval();
	}

	@Ignore("Renaming is unsupported for flat trees.")
	@Test
	public final void testRenaming() {
		doTestRenaming();
	}

	@Ignore("Moving is unsupported for flat trees.")
	@Test
	public final void testMoving() {
		doTestMoving();
	}

	@Test
	public final void testSearching() {
		doTestSearching();
	}
}
