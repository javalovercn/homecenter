package hc.util;

public class CheckSum {
	public final String md5;
	public final String sha512;

	public CheckSum(final String md5, final String sha512) {
		this.md5 = md5.toLowerCase();
		this.sha512 = sha512 != null ? sha512.toLowerCase() : null;
	}

	public final boolean isEquals(final String filemd5, final String filesha512) {
		if (sha512 == null) {
			return filemd5.toLowerCase().equals(md5);
		} else {
			return filemd5.toLowerCase().equals(md5) && filesha512.toLowerCase().equals(sha512);
		}
	}
}
