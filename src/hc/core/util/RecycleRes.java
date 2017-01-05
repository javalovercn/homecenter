package hc.core.util;

import hc.core.HCConditionWatcher;

public class RecycleRes {
	public final ThreadPool threadPool;
	public final HCConditionWatcher sequenceWatcher;
	
	public RecycleRes(final String name, final ThreadPool pool) {
		this(name, pool, null);
	}
	
	public RecycleRes(final String name, final ThreadPool pool, final HCConditionWatcher sequenceWatcher) {
		this.threadPool = pool;
		if(sequenceWatcher == null){
			this.sequenceWatcher = new HCConditionWatcher(name + "SequenceConditionWatcher", ThreadPriorityManager.SEQUENCE_SCRIPT_PRIORITY);
		}else{
			this.sequenceWatcher = sequenceWatcher;
		}
	}

	private static HCConditionWatcher sequenceTempWatcher;
	
	public static HCConditionWatcher getSequenceTempWatcher(){
		if(sequenceTempWatcher == null){
			sequenceTempWatcher = new HCConditionWatcher("Temp" + "SequenceConditionWatcher", ThreadPriorityManager.SEQUENCE_SCRIPT_PRIORITY);
		}
		return sequenceTempWatcher;
	}
}
