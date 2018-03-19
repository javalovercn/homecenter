package hc.core.util;

public abstract class ExceptionJSONBuilder {
	/**
	 * 
	 * @param hellper
	 * @param checker
	 * @param throwable
	 * @param reportURL
	 * @param script
	 * @param scriptErrMsg
	 * @return null if it is posted
	 */
	public abstract ExceptionJSON buildJSON(final HarHelper hellper,
			final ExceptionChecker checker, final Throwable throwable,
			final String reportURL, final String script, String scriptErrMsg);
}
