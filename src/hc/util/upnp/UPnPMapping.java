package hc.util.upnp;

public class UPnPMapping {
    private String enable;
    private int externalPort;
    private String host;
    private String localHost;
	private int port;
    private String portMappingDescription;
    private String protocol;

    public int getInternalPort() {
		return port;
	}
	public void setInternalPort(int internalPort) {
		this.port = internalPort;
	}
	public int getExternalPort() {
		return externalPort;
	}
	public void setExternalPort(int externalPort) {
		this.externalPort = externalPort;
	}
	public String getRemoteHost() {
		return host;
	}
	public void setRemoteHost(String remoteHost) {
		this.host = remoteHost;
	}
	public String getInternalClient() {
		return localHost;
	}
	public void setInternalClient(String internalClient) {
		this.localHost = internalClient;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getEnabled() {
		return enable;
	}
	public void setEnabled(String enabled) {
		this.enable = enabled;
	}
	public String getPortMappingDescription() {
		return portMappingDescription;
	}
	public void setPortMappingDescription(String portMappingDescription) {
		this.portMappingDescription = portMappingDescription;
	}
}
