package hc.core.util;

import java.util.Enumeration;
import java.util.Vector;

/**
 * 先入先出
 *
 */
public class LinkedSet {
	LinkedNode top, tail;// 分别指向首结点和最后一个可用结点。

	static final LinkedNodeCacher cacher = new LinkedNodeCacher(64);

	public final boolean isEmpty() {
		return top == null;
	}

	public final boolean isTailNull() {
		return tail == null;
	}

	public final Vector toVector() {
		final Vector v = new Vector(10);

		if (top == null) {
		} else {
			LinkedNode temp = top;
			do {
				v.addElement(temp.data);
				temp = temp.next;
			} while (temp != null);
		}

		return v;
	}

	public final int size() {
		if (top == null) {
			return 0;
		} else {
			int count = 0;
			LinkedNode temp = top;
			do {
				count++;
				temp = temp.next;
			} while (temp != null);
			return count;
		}
	}

	public final void addToFirst(final Object obj) {
		final LinkedNode addNode = cacher.getFree();
		addNode.data = obj;

		addNode.next = top;
		top = addNode;
	}

	/**
	 * 装入到最后端，即最后出
	 * 
	 * @param obj
	 * @return true : is empty before addTail
	 */
	public final boolean addTail(final Object obj) {
		final LinkedNode addNode = cacher.getFree();
		addNode.data = obj;

		final boolean isEmpty = (top == null);
		if (isEmpty) {
			top = addNode;
		} else {
			tail.next = addNode;
		}
		tail = addNode;
		return isEmpty;
	}

	/**
	 * 如果没有对象，则返回null
	 * 
	 * @return
	 */
	public final Object getFirst() {
		if (top == null) {
			return null;
		} else {
			final Object out = top.data;
			final LinkedNode cyc = top;
			top = top.next;
			if (top == null) {
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
	 * 
	 * @param obj
	 */
	public final void removeData(final Object obj) {
		if (top == null) {
			return;
		}

		// 首结点或首结点的下一个结点均可能为obj
		LinkedNode temp;
		if (top.data == obj) {
			temp = top.next;

			top.data = null;
			top.next = null;
			cacher.cycle(top);
			if (top == tail) {
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
		while (move != null) {
			nextOfMove = move.next;

			if (move == tail) {
				tail = pre;
			}
			if (move.data == obj) {
				move.data = null;
				move.next = null;
				cacher.cycle(move);

				pre.next = nextOfMove;

				pre = nextOfMove;
			} else {
				pre = move;
			}
			if (nextOfMove == null) {
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
	final private Stack free;

	private int freeSize = 0;

	public LinkedNodeCacher() {
		this(10);
	}

	public LinkedNodeCacher(final int size) {
		free = new Stack(size);
	}

	public final LinkedNode getFree() {
		synchronized (free) {
			if (freeSize == 0) {
				// LogManager.log("------MEM ALLOCATE [EventBack]------");
				return new LinkedNode();
			} else {
				freeSize--;
				return (LinkedNode) free.pop();
			}
		}

	}

	public final void cycle(final LinkedNode dp) {
		synchronized (free) {
			free.push(dp);
			freeSize++;
		}
	}
}