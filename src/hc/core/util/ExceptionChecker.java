package hc.core.util;

import java.util.Vector;

public class ExceptionChecker {
	static final ExceptionChecker instance = new ExceptionChecker();

	public static final ExceptionChecker getInstance() {
		return instance;
	}

	// 一直保留到关机。
	final Vector table = new Vector(32);

	public boolean isPosted(final String projectID, final String errMsg,
			final String stackTrace) {
		final StringBuffer sb = StringBufferCacher.getFree();
		final String key = sb.append(projectID).append(errMsg)
				.append(stackTrace).toString();
		StringBufferCacher.cycle(sb);
		final boolean isPosted = table.contains(key);
		if (isPosted == false) {
			table.addElement(key);
		}
		return isPosted;
	}
}
