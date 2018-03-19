package hc.core.util;

import hc.core.CoreSession;

public class BinTreeNode {
	final long id;
	CoreSession session;
	BinTreeNode leftChild;
	BinTreeNode rightChild;

	BinTreeNode(final long value) {
		this.id = value;
	}
}
