package hc.server.msb;

public class ConverterInfo {
	public String proj_id;
	public String name;
	public Converter converter;
	public DeviceCompatibleDescription upDeviceCompatibleDescriptionCache;
	public DeviceCompatibleDescription downDeviceCompatibleDescriptionCache;
	
	@Override
	public String toString(){
		return (proj_id==null?"":proj_id) + "/" + (name==null?"":name);
	}
	
	@Override
	public boolean equals(final Object obj){
		if(obj instanceof ConverterInfo){
			final ConverterInfo target = (ConverterInfo)obj;
			try{
				return target.proj_id.equals(proj_id) && target.name.equals(name);
			}catch (final Throwable e) {
			}
		}
		return false;
	}
}
