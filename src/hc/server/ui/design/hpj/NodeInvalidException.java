package hc.server.ui.design.hpj;

import javax.swing.tree.DefaultMutableTreeNode;

public class NodeInvalidException extends Exception {
	public DefaultMutableTreeNode node;
	private String desc;
	
	public NodeInvalidException(DefaultMutableTreeNode node) {
		this.node = node;
	}
	
	public void setDesc(String desc){
		this.desc = desc;
	}
	
	public String getDesc(){
		return desc;
	}
}
