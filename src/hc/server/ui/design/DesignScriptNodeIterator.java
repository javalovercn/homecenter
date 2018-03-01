package hc.server.ui.design;

import hc.server.ui.design.hpj.HPNode;

import javax.swing.tree.DefaultMutableTreeNode;

public interface DesignScriptNodeIterator {
	public void next(final DefaultMutableTreeNode treeNode, final HPNode node, final int type,
			final String name, final String script);
}
