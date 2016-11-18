package hc.server.ui.design;

import hc.core.CoreSession;
import hc.core.util.BinTree;
import hc.core.util.CCoreUtil;

public class SessionThread {
	static final BinTree binTree = new BinTree();
	
	public static final BinTree getInstance(){
		CCoreUtil.checkAccess();
		
		return binTree;
	}
	
	public static void setWithCheckSecurityX(final CoreSession ss){
		CCoreUtil.checkAccess();
		
		binTree.set(Thread.currentThread().getId(), ss);
	}
	
	public static CoreSession getWithCheckSecurityX(){
		CCoreUtil.checkAccess();
		
		return binTree.get(Thread.currentThread().getId());
	}
}