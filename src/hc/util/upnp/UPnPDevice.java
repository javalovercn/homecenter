package hc.util.upnp;

import hc.core.IConstant;
import hc.server.KeepaliveManager;
import hc.util.PropertiesManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class UPnPDevice {
	boolean isGateWay = false;
	
	public boolean isGateWay(){
		return isGateWay;
	}
	
	String URLBASE;
	String DEVICE;
	String DEVICETYPE;
	String FRIENDLYNAME;
	String MODELNUMBER;
	String SERVICETYPE;
	String CONTROLURL;
	String SCPDURL;
	
	String LOCATION;
	
    private InetAddress localAddress;


    public void getURLs() throws SAXException, IOException {

        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setContentHandler(new UPnPDeviceHandler(this));
        reader.parse(new InputSource(new URL(LOCATION).openConnection().getInputStream()));

        String ctrlURL;
        if (URLBASE != null && URLBASE.length() > 0) {
            ctrlURL = URLBASE;
        } else {
            ctrlURL = LOCATION;
        }

        final int idx = ctrlURL.indexOf('/', 7);
        if (idx > 0) {
            ctrlURL = ctrlURL.substring(0, idx);
        }

        CONTROLURL = compUrl(ctrlURL, CONTROLURL);
        SCPDURL = compUrl(ctrlURL, SCPDURL);
    }

    public static Map<String, String> action(String action,
            String service, String url, Map<String, String> para) {
    	try{
	        StringBuffer req = new StringBuffer();
	
	        req.append("<?xml version=\"1.0\"?>\r\n" +
	            "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
	            "SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
	            "<SOAP-ENV:Body><m:" + action + " xmlns:m=\"" + service + "\">");
	
	        if (para != null && para.size() > 0) {
	            Set<Map.Entry<String, String>> entrySet = para.entrySet();
	
	            for (Map.Entry<String, String> entry : entrySet) {
	                String key = entry.getKey();
					req.append("<" + key + ">" + entry.getValue() + "</" + key + ">");
	            }
	
	        }
	
	        req.append("</m:" + action + "></SOAP-ENV:Body></SOAP-ENV:Envelope>");
	
	        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
	
	        conn.setRequestMethod("POST");
	        conn.setRequestProperty("SOAPAction", service + "#" + action);
	        conn.setRequestProperty("Content-Type", "text/xml");
	        conn.setRequestProperty("Connection", "Close");
	        conn.setDoOutput(true);
	
	        byte[] bs = req.toString().getBytes();
	
	        conn.setRequestProperty("Content-Length", String.valueOf(bs.length));
	
	        conn.getOutputStream().write(bs);
	
	        Map<String, String> result = new HashMap<String, String>();
	        XMLReader parser = XMLReaderFactory.createXMLReader();
	        parser.setContentHandler(new ResultHandler(result));
	        if (conn.getResponseCode() == 500) {
	        	conn.disconnect();
	            return null;
	        } else {
	            parser.parse(new InputSource(conn.getInputStream()));
	            conn.disconnect();
	            return result;
	        }
    	}catch (Exception e) {
			
		}
    	return null;
    }

    public boolean isLineOn(){
        Map<String, String> result = action("GetStatusInfo", SERVICETYPE, CONTROLURL, null);
        if(result == null){
        	return false;
        }
        
        String connectionStatus = result.get("NewConnectionStatus");
        if (connectionStatus != null
                && connectionStatus.equalsIgnoreCase("Connected")) {
            return true;
        }else{
        	return false;
        }
    }

    public String getOutterIP() {
        Map<String, String> result = action("GetExternalIPAddress", SERVICETYPE, CONTROLURL, null);

        return (result==null)?null:result.get("NewExternalIPAddress");
    }

    public boolean deleteUPnPMapping(int outterPort, String protocol) {
        Map<String, String> args = new HashMap<String, String>();
        args.put("NewRemoteHost", getNewRomoteHost());
        args.put("NewExternalPort", Integer.toString(outterPort));
        args.put("NewProtocol", protocol);
        Map<String, String> result = action("DeletePortMapping", SERVICETYPE, CONTROLURL, args);
        if(result != null){
        	return true;
        }else{
        	return false;
        }
	}
	
	public boolean addUPnPMapping(int outterPort, int innerPort,
            String internalClient, String protocol, String description) {
        Map<String, String> para = new HashMap<String, String>();
        
        para.put("NewRemoteHost", getNewRomoteHost());
        para.put("NewProtocol", protocol);
        para.put("NewEnabled", "1");
        para.put("NewExternalPort", String.valueOf(outterPort));
        para.put("NewInternalPort", String.valueOf(innerPort));
        para.put("NewInternalClient", internalClient);
        para.put("NewPortMappingDescription", description);
        para.put("NewLeaseDuration", "0");

        Map<String, String> result = action("AddPortMapping", SERVICETYPE, CONTROLURL, para);

        if(result == null){
        	return false;
        }
        return result.get("errorCode") == null;
	}
	
	public static String getNewRomoteHost(){
		String stun = PropertiesManager.getValue(PropertiesManager.p_UPnPUseSTUNIP);
		if(stun == null || stun.equals(IConstant.TRUE)){
			return KeepaliveManager.publicShowIP;
		}else{
			return "";
		}
	}
	

	public boolean getUPnPMapping(String protocol,
            int externalPort, final UPnPMapping mapping) {
		
        mapping.setProtocol(protocol);
        mapping.setExternalPort(externalPort);

        Map<String, String> para = new HashMap<String, String>();
        para.put("NewRemoteHost", getNewRomoteHost());
        para.put("NewProtocol", protocol);
        para.put("NewExternalPort", String.valueOf(externalPort));

        Map<String, String> result = action("GetSpecificPortMappingEntry", SERVICETYPE, 
        		CONTROLURL, para);
        if(result == null){
        	return false;
        }
        String internalClient = result.get("NewInternalClient");
        String internalPort = result.get("NewInternalPort");
        String desc = result.get("NewPortMappingDescription");

        if (internalClient != null) {
            mapping.setInternalClient(internalClient);
        }

        if (internalPort != null) {
            try {
                mapping.setInternalPort(
                        Integer.parseInt(internalPort));
            } catch (Exception e) {
            }
        }
        if(desc != null){
        	mapping.setPortMappingDescription(desc);
        }
        return internalClient != null && internalPort != null;
    }


    public InetAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(InetAddress localAddress) {
        this.localAddress = localAddress;
    }

    public String getLocation() {
        return LOCATION;
    }

    public void setLocation(String location) {
        this.LOCATION = location;
    }

    public String getServiceType() {
        return SERVICETYPE;
    }

    public void setServiceType(String serviceType) {
//    	L.V = L.O ? false : LogManager.log("set serverType:" + serviceType);
        this.SERVICETYPE = serviceType;
    }

    public String getControlURL() {
        return CONTROLURL;
    }

    public void setControlURL(String controlURL) {
//    	L.V = L.O ? false : LogManager.log("set controlURL:" + controlURL);
        this.CONTROLURL = controlURL;
    }

    public String getSCPDURL() {
        return SCPDURL;
    }

    public void setSCPDURL(String sCPDURL) {
//    	L.V = L.O ? false : LogManager.log("set SCPDURL:" + sCPDURL);
        this.SCPDURL = sCPDURL;
    }

    public String getURLBase() {
        return URLBASE;
    }

    public void setURLBase(String uRLBase) {
        this.URLBASE = uRLBase.trim();
    }

    public String getFriendlyName() {
        return FRIENDLYNAME;
    }

    public void setFriendlyName(String friendlyName) {
        this.FRIENDLYNAME = friendlyName;
    }

    public String getModelNumber() {
        return MODELNUMBER;
    }

    public void setModelNumber(String modelNumber) {
        this.MODELNUMBER = modelNumber;
    }

    private String compUrl(String dst, String src) {
        if (src != null) {
            if (src.startsWith("http://")) {
                return src;
            } else {
                if (src.startsWith("/")) {
                	return dst + src;
                }else{
                    return dst + "/" + src;
                }
            }
        }
        return dst;
    }

}
