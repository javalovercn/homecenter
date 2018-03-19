package hc.core.util;

import java.util.Hashtable;

/**
 * 线程不安全
 */
public class LRUCache {
	final int capacity;
	final Hashtable map = new Hashtable();
	Node head = null;
	Node end = null;

	public LRUCache(int capacity) {
		this.capacity = capacity;
	}

	/**
	 * 如果不存在，则返回null
	 * 
	 * @param key
	 * @return
	 */
	public final Object get(Object key) {
		Node n = (Node) map.get(key);
		if (n != null) {
			removeNode(n);
			putHead(n);
			return n.value;
		}

		return null;
	}

	public final Object remove(Object key) {
		Node n = (Node) map.get(key);
		Object result = null;
		if (n != null) {
			result = n.value;
			map.remove(key);
			removeNode(n);
		}
		return result;
	}

	public final int size() {
		return map.size();
	}

	private final void removeNode(Node n) {
		if (n.pre != null) {
			n.pre.next = n.next;
		} else {
			head = n.next;
		}

		if (n.next != null) {
			n.next.pre = n.pre;
		} else {
			end = n.pre;
		}

	}

	private final void putHead(Node n) {
		n.next = head;
		n.pre = null;

		if (head != null)
			head.pre = n;

		head = n;

		if (end == null)
			end = head;
	}

	public final void put(final Object key, final Object value) {
		if (map.containsKey(key)) {
			Node old = (Node) map.get(key);
			old.value = value;
			removeNode(old);
			putHead(old);
		} else {
			Node created = new Node(key, value);
			if (map.size() >= capacity) {
				map.remove(end.key);
				removeNode(end);
			}
			putHead(created);

			map.put(key, created);
		}
	}
}

class Node {
	Object key;
	Object value;
	Node pre;
	Node next;

	public Node(final Object key, final Object value) {
		this.key = key;
		this.value = value;
	}
}