package hc.server.ui.design.hpj;

import javax.swing.tree.DefaultMutableTreeNode;

public class NodeInvalidException extends Exception {
	public DefaultMutableTreeNode node;
	private String desc;
	
	public NodeInvalidException(final DefaultMutableTreeNode node, final String desc) {
		this.node = node;
		this.desc = desc;
	}
	
	public final void setDesc(final String desc){
		this.desc = desc;
	}
	
	public final String getDesc(){
		return desc;
	}
}
