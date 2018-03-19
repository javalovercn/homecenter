package hc.core.cache;

public class PendStore {
	public String projID, softUID, urlID;
	public byte[] projIDbs, softUidBS, urlIDBS;
	public byte[] codeBS;
	public byte[] scriptBS;

	public final long createMS = System.currentTimeMillis();

	public PendStore(final String projID, final String softUID,
			final String urlID, final byte[] projIDbs, final byte[] softUidBS,
			final byte[] urlIDbs, final byte[] code, final byte[] scriptBS) {
		this.projID = projID;
		this.softUID = softUID;
		this.urlID = urlID;

		this.projIDbs = projIDbs;
		this.softUidBS = softUidBS;
		this.urlIDBS = urlIDbs;

		this.codeBS = code;
		this.scriptBS = scriptBS;
	}
}
