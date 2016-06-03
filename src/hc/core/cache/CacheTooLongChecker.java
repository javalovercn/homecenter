package hc.core.cache;

public interface CacheTooLongChecker {
	public boolean isTooLongForSleep(final String projectID, final String uuid);
}
