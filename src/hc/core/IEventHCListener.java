package hc.core;


public abstract class IEventHCListener {
	public boolean enableSameEventTag = false;//false : 如果添加一个同名，则覆盖旧的。
	
	public final boolean isEnableSameEventTag(){
		return enableSameEventTag;
	}
	
	public IEventHCListener() {
	}
	
	/**
	 * 允许使用相同的EventTag，且不进行覆盖
	 * @param enableSameEventTag
	 */
	public IEventHCListener(boolean enableSameEventTag) {
		this.enableSameEventTag = enableSameEventTag;
	}
	
	public abstract byte getEventTag();

	/**
	 * 返回true，表示停止后继其它的侦听的响应操作。
	 * @param bs
	 * @param coreSS
	 * @return
	 */
	public abstract boolean action(final byte[] bs, final CoreSession coreSS);

}
