package hc.core.sip;

import hc.core.EnumNAT;

public abstract class StunDesc {
	protected int errorResponseCode = 0;
	protected String errorReason;
	protected int publicPort;
	protected boolean error = false;
	protected int natType = 0;
    private Object connector;
	private String stunServer;
	private int stunPort;
	private String ip;
	public String getPublicIP(){
		return ip;
	}
	public void setPublicIP(String ip){
		this.ip = ip;
	}
	
//	public abstract void keepalive();
	
	public void setError(int responseCode, String reason) {
		this.error = true;
		this.errorResponseCode = responseCode;
		this.errorReason = reason;
	}
	
	public void setPublicPort(int port){
		this.publicPort = port;
	}
	
	public int getPublicPort(){
		return publicPort;
	}

	public int getNatType(){
		return natType;
	}
	public boolean isOpenAccess() {
		if (error) return false;
		return natType == EnumNAT.OPEN_INTERNET;
	}

	public void setOpenAccess() {
		natType = EnumNAT.OPEN_INTERNET;
	}

	public boolean isBlockedUDP() {
		if (error) return false;
		return natType == EnumNAT.UDP_BLOCKED;
	}

	public void setBlockedUDP() {
		natType = EnumNAT.UDP_BLOCKED;
	}
	
	public boolean isFullCone() {
		if (error) return false;
		return natType == EnumNAT.FULL_CONE_NAT;
	}

	public void setFullCone() {
		natType = EnumNAT.FULL_CONE_NAT;
	}

	public boolean isPortRestrictedCone() {
		if (error) return false;
		return natType == EnumNAT.PORT_RESTRICTED_CONE_NAT;
	}

	public void setPortRestrictedCone() {
		natType = EnumNAT.PORT_RESTRICTED_CONE_NAT;
	}

	public boolean isRestrictedCone() {
		if (error) return false;
		return natType == EnumNAT.RESTRICTED_CONE_NAT;
	}

	public void setRestrictedCone() {
		natType = EnumNAT.RESTRICTED_CONE_NAT;
	}

	public boolean isSymmetric() {
		if (error) return false;
		return natType == EnumNAT.SYMMETRICT_NAT;
	}

	public void setSymmetric() {
		natType = EnumNAT.SYMMETRICT_NAT;
	}

	public boolean isSymmetricUDPFirewall() {
		if (error) return false;
		return natType == EnumNAT.SYMMETRIC_FIREWALL;
	}

	public void setSymmetricUDPFirewall() {
		natType = EnumNAT.SYMMETRIC_FIREWALL;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (error) {
			sb.append(errorReason + " - Responsecode: " + errorResponseCode);
			return sb.toString();
		}
		sb.append("Result: ");
		if (natType == EnumNAT.OPEN_INTERNET) sb.append("Open access to the Internet.\n");
		else if (natType == EnumNAT.UDP_BLOCKED) sb.append("Firewall blocks UDP.\n");
		else if (natType == EnumNAT.FULL_CONE_NAT) sb.append("Full Cone NAT handles connections.\n");
		else if (natType == EnumNAT.RESTRICTED_CONE_NAT) sb.append("Restricted Cone NAT handles connections.\n");
		else if (natType == EnumNAT.PORT_RESTRICTED_CONE_NAT) sb.append("Port restricted Cone NAT handles connections.\n");
		else if (natType == EnumNAT.SYMMETRICT_NAT) sb.append("Symmetric Cone NAT handles connections.\n");
		else if (natType == EnumNAT.SYMMETRIC_FIREWALL) sb.append ("Symmetric UDP Firewall handles connections.\n");
		else sb.append("unkown\n");
		return sb.toString();
	}	


	public Object getSocket() {
		return connector;
	}

	public void setSocket(Object socket) {
		this.connector = socket;
	}
	public String getStunServer() {
		return stunServer;
	}
	public void setStunServer(String stunServer) {
		this.stunServer = stunServer;
	}
	public int getStunPort() {
		return stunPort;
	}
	public void setStunPort(int stunPort) {
		this.stunPort = stunPort;
	}		
}
