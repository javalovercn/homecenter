package hc.server.relay;

import hc.core.util.Stack;

import java.util.Vector;

public class ThreeDirNode {
	public Vector<SessionConnector> data = null;
	
	public final void appendSessionConnector(final SessionConnector sc){
		synchronized (this) {
			if(data == null){
				data = new Vector<SessionConnector>(4);
			}
			data.add(sc);
		}
	}
	
	public final SessionConnector popupSessionConnector(){
		synchronized (this) {
			if(data == null || data.size() == 0){
				return null;
			}else{
				return data.remove(0);
			}
		}
	}

	protected ThreeDirNode lowNode;
	protected ThreeDirNode eqNode;
	protected ThreeDirNode highNode;

	protected byte singleByte;

	public static final byte MID_BYTE = (byte)((126-48)/2);
	private static final Stack nodeCache = new Stack();
	
	private static ThreeDirNode getFree(final byte sByte){
		synchronized (nodeCache) {
			if(nodeCache.isEmpty()){
				return new ThreeDirNode(sByte);
			}else{
				final ThreeDirNode n = (ThreeDirNode)nodeCache.pop();
				n.singleByte = sByte;
				return n;
			}
		}
	}
	
	private static void cycle(final ThreeDirNode node){
		synchronized (nodeCache) {
			nodeCache.push(node);
		}
	}
	
	public ThreeDirNode(final byte singleByte) {
		this.singleByte = singleByte;
	}
	
	/**
	 * 将所存数据对象全部收集到集合stack中
	 * @param stack
	 */
	public final void getDataSet(final Stack stack){
		synchronized (data) {
			if(data != null){
				final int size = data.size();
				for (int i = 0; i < size; i++) {
					stack.push(data.elementAt(i));
				}
			}
		}
		
		if(lowNode != null){
			lowNode.getDataSet(stack);
		}
		if(eqNode != null){
			eqNode.getDataSet(stack);
		}
		if(highNode != null){
			highNode.getDataSet(stack);
		}
	}
	
	public final int countDataNodes(){
		int count = ((data == null)?0:data.size());
		if(lowNode != null){
			count += lowNode.countDataNodes();
		}
		if(eqNode != null){
			count += eqNode.countDataNodes();
		}
		if(highNode != null){
			count += highNode.countDataNodes();
		}
		return count;
	}
	
	public final int countNodes(){
		int count = 1;
		if(lowNode != null){
			count += lowNode.countNodes();
		}
		if(highNode != null){
			count += highNode.countNodes();
		}
		if(eqNode != null){
			count += eqNode.countNodes();
		}
		
		return count;
	}
	
	/**
	 * 返回true，表明末端没有分支，供前端点移去该分支之用
	 * @param bs
	 * @param idx
	 * @param endIdx = idx + bs.length
	 * @return
	 */
	public final boolean delNode(final SessionConnector sc, final byte[] bs, int idx, final int endIdx){
		final int cpmResult = bs[idx] - singleByte;
		if(cpmResult == 0){
			if((++idx) == endIdx){
				synchronized (this) {
					if(data != null){
						data.remove(sc);
						if(data.size() > 0){
							return false;
						}
					}
				}
				if(highNode == null && lowNode == null && eqNode == null){
					return true;
				}else{
					return false;
				}
			}
			if(eqNode == null){
				return false;
			}
			if(eqNode.delNode(sc, bs, idx, endIdx)){
				cycle(eqNode);
				eqNode = null;
				if(highNode == null && lowNode == null && (data == null || data.size() == 0)){
					return true;
				}
			}
			return false;
		}else if(cpmResult < 0){
			if(lowNode == null){
				return false;
			}
			if(lowNode.delNode(sc, bs, idx, endIdx)){
				cycle(lowNode);
				lowNode = null;
				if(eqNode == null && highNode == null && (data == null || data.size() == 0)){
					return true;
				}
			}
			return false;
		}else{
			if(highNode == null){
				return false;
			}
			if(highNode.delNode(sc, bs, idx, endIdx)){
				cycle(highNode);
				highNode = null;
				if(eqNode == null && lowNode == null && (data == null || data.size() == 0)){
					return true;
				}
			}
			return false;
		}
	}

//	public boolean delOverTime(RelayOverTimeCond cond){
//		if(highNode != null){
//			if(highNode.delOverTime(cond)){
//				highNode = null;
//			}
//		}
//		
//		if(eqNode != null){
//			if(eqNode.delOverTime(cond)){
//				eqNode = null;
//			}
//		}
//		
//		if(lowNode != null){
//			if(lowNode.delOverTime(cond)){
//				lowNode = null;
//			}
//		}
//		
//		if(data != null){
//			if(cond.timeBound > data.lastKeepaliveMS){
//				cond.delCount++;
//				data = null;
//			}
//		}
//		
//		if(highNode == null && lowNode == null && eqNode == null && data == null){
//			return true;
//		}else{		
//			return false;
//		}
//	}

	/**
	 * 没有找到，返回null。
	 * @param bs
	 * @param idx
	 * @param endIdx = idx + bs.length
	 * @return
	 */
	public final SessionConnector getNodeData(final byte[] bs, int idx, final int endIdx) {
		ThreeDirNode current = this;
		final byte currByte = bs[idx];
		while (true) {
			final int cpmResult = currByte - current.singleByte;
			if(cpmResult == 0){
				if((++idx) == endIdx){
					return current.popupSessionConnector();
				}
				if(current.eqNode == null){
					return null;
				}
				return current.eqNode.getNodeData(bs, idx, endIdx);
			}else if(cpmResult < 0){
				if(current.lowNode == null){
					return null;
				}
				current = current.lowNode;
			}else{
				if(current.highNode == null){
					return null;
				}
				current = current.highNode;
			}
		}
	}

	/**
	 * 
	 * @param bs
	 * @param idx
	 * @param endIdx = idx + bs.length
	 * @return
	 */
	public final void addNodeData(final byte[] bs, int idx, final int endIdx, final SessionConnector data) {
		ThreeDirNode current = this;
		while (true) {
			final byte currByte = bs[idx];
			final int cpmResult = currByte - current.singleByte;
			if(cpmResult == 0){
				if((++idx) == endIdx){
					current.appendSessionConnector(data);
					return;
				}
				if(current.eqNode == null) {
					current.eqNode = getFree(bs[idx]);//不能使用currByte
				}
				current = current.eqNode;
			}else if(cpmResult < 0){
				if(current.lowNode == null){
					current.lowNode = getFree(currByte);
				}
				current = current.lowNode;
			}else{
				if(current.highNode == null){
					current.highNode = getFree(currByte);
				}
				current = current.highNode;
			}
		}
	}
}
