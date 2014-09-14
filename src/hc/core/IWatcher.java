package hc.core;

public interface IWatcher {
	/**
	 * 返回true，表示观察结束，由容器负责移出，并不再执行
	 * @return
	 */
	public boolean watch();
	
	public void setPara(Object p);
	
	public void cancel();
	
	public boolean isNotCancelable();
}
