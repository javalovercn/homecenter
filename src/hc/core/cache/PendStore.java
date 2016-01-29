package hc.core.cache;

public class PendStore {
	public String projID, uuid, urlID;
	public byte[] projIDbs, uuidBS, urlIDBS;
	public byte[] codeBS;
	public byte[] scriptBS;
	
	public PendStore(final String projID, final String uuid, final String urlID, 
			final byte[] projIDbs, final byte[] uuidBS, final byte[] urlIDbs, 
			final byte[] code, final byte[] scriptBS){
		this.projID = projID;
		this.uuid = uuid;
		this.urlID = urlID;
		
		this.projIDbs = projIDbs;
		this.uuidBS = uuidBS;
		this.urlIDBS = urlIDbs;
		
		this.codeBS = code;
		this.scriptBS = scriptBS;
	}
}
