package hc.core.util;

/**
 * 先入先出
 *
 */
public class LinkedSet {
	LinkedNode top, tail;//分别指向首结点和最后一个可用结点。
	
	static final LinkedNodeCacher cacher = new LinkedNodeCacher();
	
	public boolean isEmpty(){
		return top == null;
	}
	
	public boolean isTailNull(){
		return tail == null;
	}
	
	public void addToFirst(Object obj){
		final LinkedNode addNode = cacher.getFree();
		addNode.data = obj;
		
		addNode.next = top;
		top = addNode;
	}
	
	/**
	 * 装入到最后端，即最后出
	 * @param obj
	 */
	public void addTail(Object obj){
		final LinkedNode addNode = cacher.getFree();
		addNode.data = obj;
		
		if(top == null){
			top = addNode;
		}else{
			tail.next = addNode;
		}
		tail = addNode;
	}
	
	/**
	 * 如果没有对象，则返回null
	 * @return
	 */
	public Object getFirst(){
		if(top == null){
			return null;
		}else{
			Object out = top.data;
			LinkedNode cyc = top;
			top = top.next;
			if(top == null){
				tail = null;
			}
			
			cyc.data = null;
			cyc.next = null;
			
			cacher.cycle(cyc);
			
			return out;
		}
	}
	
	/**
	 * 如果集合中存在多个相同实例，一并删除
	 * @param obj
	 */
	public void removeData(Object obj){
		if(top == null){
			return;
		}
		
		//首结点或首结点的下一个结点均可能为obj
		LinkedNode temp;
		if(top.data == obj){
			temp = top.next;
			
			top.data = null;
			top.next = null;
			cacher.cycle(top);
			if(top == tail){
				tail = null;
				top = null;
				return;
			}
			
			top = temp;
			this.removeData(obj);
			return;
		}
		
		LinkedNode pre = top;
		LinkedNode move = top.next;
		LinkedNode nextOfMove;
		while(move != null){
			nextOfMove = move.next;
			
			if(move == tail){
				tail = pre;
			}
			if(move.data == obj){
				move.data = null;
				move.next = null;
				cacher.cycle(move);
				
				pre.next = nextOfMove;
				
				pre = nextOfMove;
			}else{
				pre = move;
			}
			if(nextOfMove == null){
				return;
			}
			move = nextOfMove.next;
		}
	}
}

class LinkedNode {
	Object data;
	LinkedNode next;
}

class LinkedNodeCacher {
	final private Stack free = new Stack();
	
	private int freeSize = 0;
	
	public LinkedNodeCacher(){
	}
	
	public final LinkedNode getFree(){
		synchronized (free) {
			if(freeSize == 0){
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("------MEM ALLOCATE [EventBack]------");
				return new LinkedNode();
			}else{
				freeSize--;
				return (LinkedNode)free.pop();
			}
        }
		
	}
	
	public final void cycle(LinkedNode dp){
		synchronized (free) {
			free.push(dp);
			freeSize++;
        }		
	}
}