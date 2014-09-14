package hc.server.ui.design.hpj;


import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class NodeEditPanel extends JPanel{
	DefaultMutableTreeNode currNode;
	JTree tree;
	public DefaultMutableTreeNode getCurrNode(){
		return currNode;
	}
	
	public void init(final MutableTreeNode data, JTree tree){
		currNode = (DefaultMutableTreeNode)data;
		this.tree = tree;
	}


}
