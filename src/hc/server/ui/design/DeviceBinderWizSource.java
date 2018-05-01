package hc.server.ui.design;

public interface DeviceBinderWizSource {
	public DevTree buildDevTree() throws Exception ;
	
	public ConverterTree buildConverterTree() throws Exception ;
	
	public BDNTree buildTree() throws Exception ;
	
	public void save();
	
	public void cancel();
	
}
