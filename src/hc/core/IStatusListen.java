package hc.core;

public interface IStatusListen {
	/**
	 * 先更新状态，后触发listener
	 * @param statusFrom
	 * @param statusTo
	 */
	public void notify(short statusFrom, short statusTo);
}
