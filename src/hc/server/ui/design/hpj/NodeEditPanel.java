package hc.server.ui.design.hpj;

import hc.App;
import hc.server.ui.design.Designer;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class NodeEditPanel extends JPanel{
	HPNode currItem;
	boolean isInited = false;

	public JComponent getMainPanel(){
		return this;
	}
	
	public final void notifyModified(final boolean isModi){
		if(isInited){
			currItem.getContext().modified.setModified(isModi);
		}
	}
	
	public final long getSaveToken(){
		if(isInited){
			return currItem.getContext().modified.getSaveToken();
		}else{
			return 0;
		}
	}
	
	public final boolean isModified(){
		if(isInited){
			return currItem.getContext().modified.isModified();
		}else{
			return false;
		}
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
	public Designer designer;
	public DefaultMutableTreeNode getCurrNode(){
		return currNode;
	}
	
	public void loadAfterShow(final Runnable run){
		if(run != null){
			run.run();
		}
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
