package hc.core.util;

public interface SecurityChecker {
	public void check(Object token);
	
	public void resetFastCheckThreads();
}
