package hc.util;

import hc.core.ContextManager;
import hc.core.IContext;
import hc.core.L;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.util.upnp.UPnPDevice;
import hc.util.upnp.UPnPMapping;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class UPnPUtil {
	private static final String TCP = "TCP";
	
	private static InetAddress localAds;
	
	/**
	 * 如果已开通或新开通，返回true；否则返回false
	 * @param ia
	 * @param localPort
	 * @return
	 */
	public static String[] startUPnP(InetAddress ia, int localPort, int oldUPnPPort, String upnpToken){
//		if(hcgd != null){
//			try {
//				if(hcgd.isLineOn()){
//					return true;
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
		String[] out = null;
		if(isSupportUPnP(ia)){		
			//尚未开通UPnP
			out = addMapping(100, ia, localPort, oldUPnPPort, upnpToken);
			if(out == null){
			}else{
				localAds = ia;
			}
		}
		return out;
	}
	
	/**
	 * 退出时，关闭开放的UPnP
	 * @return
	 */
	public static boolean removeUPnPMapping(int port){
		if(removeUPnPMapping(port, localAds)){
			L.V = L.O ? false : LogManager.log("Remove UPnP Mapping at extPort:" + port);
			return true;
		}else{
			return false;
		}
	}
	
	public static UPnPDevice hcgd;
	
    public static final int UPNPPORT = 1900;
    public static final String UPNPIP = "239.255.255.250";
    private static final int TIMEOUT = 10000;

    private static UPnPDevice findLocation(byte[] reply, int len) {

        UPnPDevice device = new UPnPDevice();

        BufferedReader brLine = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(reply, 0, len)));
        String line = null;
        try {
            line = brLine.readLine().trim();
        } catch (IOException ex) {
        	ex.printStackTrace();
        }

        while (line != null) {
        	line = line.trim();
        	if(line.length() > 0){
                int idx = line.indexOf(':');
                if(idx > 0){
					String key = line.substring(0, idx);
	
	                key = key.trim();
	
	                if (key.equalsIgnoreCase("LOCATION")) {
		                device.setLocation(line.substring(idx + 1).trim());
		                break;
	                }
                }
	            try {
	                line = brLine.readLine().trim();
	            } catch (IOException ex) {
	            }
        	}else{
	        	try {
	                line = brLine.readLine().trim();
	            } catch (IOException ex) {
	            }
        	}
        }

        return device;
    }

    public static UPnPDevice discover(InetAddress localAddress){
    	UPnPDevice d = null;
    	
        final String searchHttp = "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: " + UPNPIP + ":" + UPNPPORT + "\r\n" +
                "ST: " + "urn:schemas-upnp-org:device:InternetGatewayDevice:1" + "\r\n" +
                "MAN: \"ssdp:discover\"\r\nMX: 3\r\n\r\n";
        DatagramSocket socket = null;
        
        try {
        	socket = new DatagramSocket(0, localAddress);
            socket.setSoTimeout(TIMEOUT);

            byte[] bs = searchHttp.getBytes();
            DatagramPacket dp = new DatagramPacket(bs, bs.length);
            dp.setAddress(InetAddress.getByName(UPNPIP));
            dp.setPort(UPNPPORT);

            socket.send(dp);

            while (true) {
                dp = new DatagramPacket(new byte[2000], 2000);
                try {
                    socket.receive(dp);

                    d = findLocation(dp.getData(), dp.getLength());

                    d.getURLs();
                    
                    if(d.isGateWay() && d.isLineOn()){
                    	break;
                    }else{
                    	d = null;
                    }
                } catch (Exception ste) {
                	break;
                }
            }
        }catch (Exception e) {
			e.printStackTrace();
		}finally{
            socket.close();
        }
        
        return d;
    }

    private static boolean isCheckedUPnP = false;
    public static boolean isSupportUPnP(InetAddress ia){
    	if(isCheckedUPnP == false){
//			L.V = L.O ? false : LogManager.log("Starting UPnP Discovery Service");
			L.V = L.O ? false : LogManager.log("Looking for UPnP devices from [" + ia.toString() + "]");
			hcgd = discover(ia);
			
			if (null != hcgd) {
				String idg = "["+hcgd.getFriendlyName()+"("+hcgd.getModelNumber()+")]";
				L.V = L.O ? false : LogManager.log("UPnP device found. " + idg);
				ContextManager.getContextInstance().displayMessage((String)ResourceUtil.get(IContext.INFO), 
						"UPnP : " + idg, IContext.INFO, null, 0);
			}
			
			isCheckedUPnP = true;
			
			if(hcgd == null){
			    L.V = L.O ? false : LogManager.log("No UPnP device.");
			}
    	}
    	return (hcgd != null);
    }
	
	/**
	 * 返回0或负数，表示失败
	 * @param beginExteralPort
	 * @param localAddress
	 * @param localPort
	 * @param oldUPnPPort TODO
	 * @return {exteralAddress, exteralPort}，失败返回null
	 */
	private static String[] addMapping(int beginExteralPort, InetAddress localAddress, int localPort, 
			int oldUPnPPort, String upnpToken){
		try{
			if (null != hcgd) {
			} else {
			    return null;
			}
			
//			hcgd = new HCGateDevice(d);
			
	//		InetAddress localAddress = d.getLocalAddress();
	//		L.V = L.O ? false : LogManager.log("Using local address: "+localAddress );
			String externalIPAddress = hcgd.getOutterIP();
//			L.V = L.O ? false : LogManager.log("External address: "+ externalIPAddress);
			UPnPMapping portMapping = new UPnPMapping();
	
//			L.V = L.O ? false : LogManager.log("Attempting to map port "+localPort );
//			L.V = L.O ? false : LogManager.log("Querying device to see if mapping for port "+localPort+" already exists");
	
			if(oldUPnPPort != 0){
				if(hcgd.getUPnPMapping(TCP,oldUPnPPort,portMapping)){
					if(isSelfMapDesc(portMapping.getPortMappingDescription(), upnpToken)){
						L.V = L.O ? false : LogManager.log("Delete UPnP old map at Ext Port:" + oldUPnPPort);
						hcgd.deleteUPnPMapping(oldUPnPPort, TCP);
					}
				}
			}
			while (true) {
				portMapping = new UPnPMapping();
				try{
					if(!hcgd.getUPnPMapping(TCP,beginExteralPort,portMapping)){
					}else{
						if(isSelfMapDesc(portMapping.getPortMappingDescription(), upnpToken)){
							L.V = L.O ? false : LogManager.log("Delete UPnP old map at Ext Port:" + beginExteralPort);
							hcgd.deleteUPnPMapping(beginExteralPort, TCP);
						}else{
							beginExteralPort++;
							continue;
						}
					}
				}catch (Exception e) {
					//UTF-8时，会出现异常
//					e.printStackTrace();
					beginExteralPort++;
					continue;
				}
//			    L.V = L.O ? false : LogManager.log("Sending port mapping request");
	
			    if (hcgd.addUPnPMapping(beginExteralPort, localPort, localAddress.getHostAddress(), TCP, upnpToken)) {
			    	L.V = L.O ? false : LogManager.log("Success add UPnP exteral PortMap [" + externalIPAddress + ":" + beginExteralPort + "]");
			    	String[] out = {HCURLUtil.convertIPv46(externalIPAddress),String.valueOf(beginExteralPort)};
			        return out;
			    } else {
			    	beginExteralPort++;
			    }
			    
			}
		}catch (Exception e) {
			e.printStackTrace();
			L.V = L.O ? false : LogManager.log(e.toString());
		}
		return null;		
	}
	
	public static boolean isSelfMapDesc(String desc, String upnpToken){
		if(desc == null){
			return false;
		}
		return upnpToken.startsWith(desc);
	}
	
	private static boolean removeUPnPMapping(int externalPort, InetAddress localAddress){
		try{
			if (null == hcgd) {
			    return false;
			}else{
				return hcgd.deleteUPnPMapping(externalPort,TCP);
			}
		}catch (Exception e) {
	        L.V = L.O ? false : LogManager.log("Failed at remove UPnP Port");
			L.V = L.O ? false : LogManager.log(e.toString());
			return false;
		}
	}

	/**
	 * 根据用户设定，使用多网卡环境下的指定设备，如果是单一网卡环境，则使用之
	 * @return
	 */
	public static InetAddress loadUserInetAddress(){
		//TODO 优先加载用户指定的网卡，注意：为多网卡环境下
		try{
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
				while (iaddresses.hasMoreElements()) {
					InetAddress iaddress = iaddresses.nextElement();
					if (java.net.Inet4Address.class.isInstance(iaddress) || 
							java.net.Inet6Address.class.isInstance(iaddress)) {
						if ((!iaddress.isLoopbackAddress()) && (!iaddress.isLinkLocalAddress())) {
							return iaddress;
						}
					}
				}
			}
		}catch (Exception e) {
			
		}
		return null;
	}
}
