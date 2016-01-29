package hc.core;

public interface IWatcher {
	/**
	 * 返回true，表示观察结束，由容器负责移出，并不再执行
	 * @return
	 */
	public boolean watch();
	
	public void setPara(Object p);
	
	/**
	 * 由系统根据需要进行cancel操作，而非应用调用
	 */
	public void cancel();
	
	public boolean isCancelable();
}
