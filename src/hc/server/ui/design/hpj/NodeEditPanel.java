package hc.server.ui.design.hpj;

import hc.App;
import hc.server.ui.design.Designer;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class NodeEditPanel extends JPanel{
	public JComponent getMainPanel(){
		return this;
	}
	
	final Runnable updateTreeRunnable = new Runnable() {
		@Override
		public void run() {
			tree.updateUI();
		}
	};
	
	final ThreadGroup threadPoolToken = App.getThreadPoolToken();
	DefaultMutableTreeNode currNode;
	JTree tree;
	Designer designer;
	public DefaultMutableTreeNode getCurrNode(){
		return currNode;
	}
	
	public void init(final MutableTreeNode data, final JTree tree){
		currNode = (DefaultMutableTreeNode)data;
		this.tree = tree;
	}

	public void extInit(){
	}

	public void notifyLostEditPanelFocus(){
	}
}
