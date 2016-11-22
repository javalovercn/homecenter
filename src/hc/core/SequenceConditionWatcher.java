package hc.core;

/**
 * 序列式执行任务单元<BR>
 * 不占用主线程
 */
public class SequenceConditionWatcher extends HCConditionWatcher {
	public SequenceConditionWatcher(final String watcherName, final int priority) {
		super(watcherName, true, priority);
	}
}
