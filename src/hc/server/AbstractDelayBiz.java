package hc.server;

public abstract class AbstractDelayBiz {
	private Object obj;

	public AbstractDelayBiz(Object object) {
		this.obj = object;
	}

	public Object getPara() {
		return obj;
	}

	public void setPara(Object obj) {
		this.obj = obj;
	}

	public abstract void doBiz();
}
