package hc.core.util;

public interface HarHelper {
	public String getExceptionReportURL();

	public HarInfoForJSON getHarInfoForJSON();

	static final String NO_REPORT_URL_IN_HAR = "no_report_url_in_har";
}
