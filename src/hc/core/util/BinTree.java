package hc.core.util;

import hc.core.CoreSession;

public class BinTree {
	private BinTreeNode root;
	
	/**
	 * 
	 * @param id
	 * @param coreSS 可能为null
	 */
	public final void set(final long id, final CoreSession coreSS){
		if(root == null){
			root = new BinTreeNode(id);
			root.session = coreSS;
			return;
		}
		
		BinTreeNode parentStep = null;
		boolean isLeft = false;
		BinTreeNode curr = root;
		
		while(true){
			final long currID = curr.id;
			if(currID == id){
				curr.session = coreSS;
				return;
			}else if(currID < id){
				parentStep = curr;
				curr = parentStep.leftChild;
				isLeft = true;
			}else{
				parentStep = curr;
				curr = parentStep.rightChild;
			}
			
			if(curr == null){
				curr = new BinTreeNode(id);
				curr.session = coreSS;
				if(isLeft){
					parentStep.leftChild = curr;
				}else{
					parentStep.rightChild = curr;
				}
				return;
			}
		}
	}
	
	/**
	 * 
	 * @param id
	 * @return 可能为null
	 */
	public final CoreSession get(final long id){
		BinTreeNode curr = root;
		
		while(true){
			if(curr == null){
				return null;
			}
			
			final long currID = curr.id;
			if(currID == id){
				return curr.session;
			}else if(currID < id){
				curr = curr.leftChild;
			}else{
				curr = curr.rightChild;
			}
		}
		
	}
}
