package hc.core.util;

import hc.core.SequenceConditionWatcher;

public class RecycleRes {
	public final ThreadPool threadPool;
	public final SequenceConditionWatcher sequenceWatcher;
	
	public RecycleRes(final String name, final ThreadPool pool) {
		this(name, pool, null);
	}
	
	public RecycleRes(final String name, final ThreadPool pool, final SequenceConditionWatcher sequenceWatcher) {
		this.threadPool = pool;
		if(sequenceWatcher == null){
			this.sequenceWatcher = new SequenceConditionWatcher(name + "SequenceConditionWatcher", ThreadPriorityManager.SEQUENCE_SCRIPT_PRIORITY);
		}else{
			this.sequenceWatcher = sequenceWatcher;
		}
	}

	private static SequenceConditionWatcher sequenceTempWatcher;
	
	public static SequenceConditionWatcher getSequenceTempWatcher(){
		if(sequenceTempWatcher == null){
			sequenceTempWatcher = new SequenceConditionWatcher("temp" + "SequenceConditionWatcher", ThreadPriorityManager.SEQUENCE_SCRIPT_PRIORITY);
		}
		return sequenceTempWatcher;
	}
}
