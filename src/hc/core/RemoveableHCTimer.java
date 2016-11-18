package hc.core;


public abstract class RemoveableHCTimer extends HCTimer {
	boolean isAddedIn = true;
	
	public RemoveableHCTimer(String name, boolean enable) {
		super(name, enable);
	}
	
	public RemoveableHCTimer(String name, int ms, boolean enable) {
		super(name, ms, enable);
	}

	public void setEnable(final boolean enable){
		if(enable && (isAddedIn == false)){
			isAddedIn = true;
			HCTimer.notifyToGenerailManager(this);//再次移进
		}else if((enable == false) && isAddedIn){
			isAddedIn = false;
			HCTimer.remove(this);//再次移出
		}
		//注意：需要先加载notifyToGenerailManager，然后再setEnable，因为因时间流逝，而需要重新计算下点
		super.setEnable(enable);
	}
}
