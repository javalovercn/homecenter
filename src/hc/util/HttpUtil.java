package hc.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.core.util.URLEncoder;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;


public class HttpUtil {
	static {
		System.setProperty("sun.net.client.defaultConnectTimeout", "5000");
		System.setProperty("sun.net.client.defaultReadTimeout", "5000");	 
	}
	public static boolean checkExistNetworkInterface(String name) {
		try {
			if (NetworkInterface.getByName(name) != null) {
				return true;
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isValidEmail(String email){
	      Pattern p = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
	      Matcher m = p.matcher(email);
	      if (m.find()){
	    	  return true;
	      }
	      return false;
	}

	public static InetAddress getInetAddress(String dispname) {
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface
					.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				if (iface.getDisplayName().startsWith(dispname)) {
					Enumeration<InetAddress> iaddresses = iface
							.getInetAddresses();
					while (iaddresses.hasMoreElements()) {
						InetAddress iaddress = iaddresses.nextElement();
						if (java.net.Inet4Address.class.isInstance(iaddress)
								|| java.net.Inet6Address.class
										.isInstance(iaddress)) {
							if ((!iaddress.isLoopbackAddress())
									&& (!iaddress.isLinkLocalAddress())) {
								return iaddress;
							}
						}
					}
					return null;
				}
			}
		} catch (Exception e) {

		}
		return null;
	}

	public static String[] getNetworkInterface() {
		int count = 0;
		String[] temp = new String[30];
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface
					.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
				while (iaddresses.hasMoreElements()) {
					InetAddress iaddress = iaddresses.nextElement();
					System.out.println(iface.getDisplayName() + ":"
							+ iface.getName() + ", "
							+ iaddress.getHostAddress());
					if (java.net.Inet4Address.class.isInstance(iaddress)
							|| java.net.Inet6Address.class.isInstance(iaddress)) {
						if ((!iaddress.isLoopbackAddress())
								&& (!iaddress.isLinkLocalAddress())) {
							String displayName = iface.getDisplayName();
							int idx = displayName.indexOf(" - ");
							if (idx > 0) {
								displayName = displayName.substring(0, idx);
							}
							temp[count++] = displayName + " - "
									+ iface.getName();

						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		String[] out = new String[count];
		for (int i = 0; i < out.length; i++) {
			out[i] = temp[i];
		}
		return out;
	}

	public static final String FORWARD_301_FOREVER = "301";
	public static final String FORWARD_302_TEMP = "302";
	private static final String TagLocation = "Location";

	public static byte[] convertStrIp(String ip) {
		String[] s = ip.split("\\.");
		byte[] out = new byte[s.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = (byte) (Integer.parseInt(s[i]));
		}
		return out;
	}

	/**
	 * 停止使用
	 * 
	 * @param url
	 * @return [url, forward_type]
	 */
	public static String[] getForward(String url) {
		try {
			URLConnection conn = new URL(url).openConnection();

			if (conn instanceof HttpURLConnection) {
				HttpURLConnection httpconn = (HttpURLConnection) conn;
				httpconn.setInstanceFollowRedirects(false);
				httpconn.connect();

				int responseCode = httpconn.getResponseCode();
				httpconn.connect();
				if (responseCode == 200) {
					return null;
				} else if (responseCode == 301 || responseCode == 302) {
					String forwardUrl = conn.getHeaderField(TagLocation);
					String resolveURL = getResolveURL(url, forwardUrl);

					String[] out = new String[2];
					out[0] = resolveURL;
					if (responseCode == 301) {
						out[1] = FORWARD_301_FOREVER;
					} else {
						out[1] = FORWARD_302_TEMP;
					}
					return out;
				}
			}
		} catch (Exception e) {

		}
		return null;
	}

	public static String getResolveURL(String bURL, String relativeURI) {
		if (relativeURI == null) {
			return null;
		}
		int i = relativeURI.indexOf("\"");
		if (i > 0) {
			relativeURI = relativeURI.substring(0, i);
		}

		if (bURL.startsWith("http")) {

		} else {
			bURL = "http://" + bURL;
		}

		try {
			relativeURI = new URL(new URL(bURL), relativeURI).toExternalForm();
			relativeURI = ByteUtil.encodeURI(relativeURI, IConstant.UTF_8);
			return relativeURI;
		} catch (Exception e) {
			return null;
		}
	}
	
	public static String getAjaxForSimu(String url, boolean isTCP) {
		final boolean isSimu = PropertiesManager.isTrue(PropertiesManager.p_IsSimu);
		url = replaceSimuURL(url, isSimu);
		//---------reuseThisCode
		return isTCP?RootServerConnector.getAjaxTCP(url, isSimu):getAjax(url);
		
//		String out = null;
//		if(isTCP){
//			out = RootServerConnector.getAjaxTCP(url, isSimu);
//		}
//		
//		String out2 = getAjax(url);
//		if(out != null){
//			return out;
//		}else{
//			return out2;
//		}
	}

	public static String replaceSimuURL(String url, final boolean isSimu) {
		if(isSimu){
			url = StringUtil.replace(url, "homecenter.mobi", "localhost:80");//192.168.1.101
			url = StringUtil.replace(url, ":80", ":8080");//192.168.1.101
			url = StringUtil.replace(url, "call.php", "callsimu.php");//192.168.1.101
		}
		return url;
	}

	/**
	 * 如果没有成功，则返回null
	 * 
	 * @param url_forward
	 * @return
	 */
	public static String getAjax(String url_forward) {
		try {
			URLConnection conn = new URL(url_forward).openConnection();

			if (conn instanceof HttpURLConnection) {
				HttpURLConnection httpconn = (HttpURLConnection) conn;
				httpconn.setInstanceFollowRedirects(true);
				httpconn.connect();

				int responseCode = httpconn.getResponseCode();
				if (responseCode == 200) {
					int expectedLength = conn.getContentLength();
					if (expectedLength == 0) {
						// 长度为0的特殊情形
						httpconn.disconnect();
						return "";
					}
					InputStream in;
					in = httpconn.getInputStream();
					int MAX_BLOCK_SIZE = 20 * 1024;
					if (expectedLength == -1) {
						expectedLength = 1024;
					} else if (expectedLength > MAX_BLOCK_SIZE) {
						expectedLength = MAX_BLOCK_SIZE;
					}
					byte[] buf = new byte[expectedLength];
					int n;
					int total = 0;

					while ((n = in.read(buf, total, buf.length - total)) != -1) {
						total += n;
						if (total == buf.length) {
							// try to read one more character
							int c = in.read();
							if (c == -1)
								break; // EOF, we're done
							else {
								if (buf.length * 2 <= MAX_BLOCK_SIZE) {
									// need more space in array. Double the
									// array, but don't make
									// it bigger than maxBytes.
									byte[] newbuf = new byte[buf.length * 2];
									System.arraycopy(buf, 0, newbuf, 0,
											buf.length);
									buf = newbuf;
									buf[total++] = (byte) c;
								} else {
									break;
								}
							}
						}
					}

					in.close();
					httpconn.disconnect();

					return new String(buf, 0, total, IConstant.UTF_8);

				} else {
					httpconn.disconnect();
				}
			}
		} catch (Exception e) {

		}
		return null;
	}
	
	public static String getLocalIP(){
		InetAddress inet;
		try {
			inet = InetAddress.getLocalHost();
	        return inet.getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static InetAddress getLocal(){
//		try {
//			因为有可能返回loop型，所以关闭本操作
//			InetAddress.anyLocalAddress();
//			return (InetAddress.getLocalHost());
//		} catch (Throwable e) {
			// sometimes get this when changing host name
			// return first non-loopback one
			InetAddress ia = getServerInetAddress(true);
			if(ia != null){
				return ia;
			}else{
				ia = getServerInetAddress(false);
			}
			
			if(ia != null){
				return ia;
			}else{
				try {
					return (InetAddress.getByName("127.0.0.1"));
				} catch (UnknownHostException e1) {
				}
			}
			return null;
//		}
	}
	
	public static final String AUTO_DETECT_NETWORK = "auto detect";
	
	public static InetAddress getInetAddressByDeviceName(String name){
		return filerInetAddress(getNetworkInterface(name));
	}
	
	private static NetworkInterface getNetworkInterface(String name){
		try{
			final Enumeration nis = NetworkInterface.getNetworkInterfaces();
			while (nis.hasMoreElements()) {
				NetworkInterface ni = (NetworkInterface) nis.nextElement();
				if(ni.getDisplayName().equals(name)){
					return ni;
				}
			}	
		}catch (Exception e) {
		}
		return null;
	}
	
	public static Vector<String> getAllNetworkInterfaces(){
		Vector<String> v = new Vector<String>();
		v.add(AUTO_DETECT_NETWORK);
		
		try{
			final Enumeration nis = NetworkInterface.getNetworkInterfaces();
			while (nis.hasMoreElements()) {
				NetworkInterface ni = (NetworkInterface) nis.nextElement();
				v.add(ni.getDisplayName());
			}	
		}catch (Exception e) {
		}
		return v;
	}

	/**
	 * 过滤loop型、IPv6型，及选择性PointToPoint
	 */
	public static InetAddress getServerInetAddress(final boolean mustP2P) {
		try {
			Enumeration nis = NetworkInterface.getNetworkInterfaces();
			while (nis.hasMoreElements()) {
				NetworkInterface ni = (NetworkInterface) nis.nextElement();
				if(mustP2P && (ni.isPointToPoint() == false)){
					continue;
				}
				InetAddress ia = filerInetAddress(ni);
				if(ia != null){
					return ia;
				}
			}
		} catch (Throwable f) {
		}
		return null;
	}

	public static InetAddress filerInetAddress(NetworkInterface ni) {
		try{
			Enumeration addresses = ni.getInetAddresses();
			while (addresses.hasMoreElements()) {
				InetAddress address = (InetAddress) addresses.nextElement();
				if (address.isLoopbackAddress() || address instanceof Inet6Address) {
					continue;
				}
				return (address);
			}
		}catch (Throwable e) {
		}
		return null;
	}
	
	/**
	 * 非IPv6型的本地InetAddress
	 * @return
	 */
	public static InetAddress getLocalOutAddress(boolean isIPv6) {
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface
					.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
				while (iaddresses.hasMoreElements()) {
					InetAddress iaddress = iaddresses.nextElement();
					if (!iaddress.isLoopbackAddress()
							&& !iaddress.isSiteLocalAddress()
							&& !iaddress.isLinkLocalAddress()) {
						if (isIPv6 == false) {
							if(java.net.Inet4Address.class.isInstance(iaddress)){
								return iaddress;
							}
						}else if(java.net.Inet6Address.class.isInstance(iaddress)){
							return iaddress;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ByteBuffer buildByteBuffer() {
		return ByteBuffer.allocate(1024);
	}
	
	/**
	 * 
	 * @param langURL suchas pc/faq.htm#item7
	 * @return
	 */
	public static boolean browseLangURL(String langURL){
		return HttpUtil.browse(langURL);
	}

	public static String buildLangURL(String langURL, String lang)
			throws UnsupportedEncodingException {
		String url = URLEncoder.encode("http://homecenter.mobi/_lang_/" + langURL, IConstant.UTF_8);
		if(lang == null){
			url = "?to=" + url;
		}else{
			url = "?lang=" + lang + "&to=" + url;
		}
		return "http://homecenter.mobi/gotolang.php" + url;
	}

	public static boolean browse(String donateURL) {
		java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
		if( !desktop.isSupported( java.awt.Desktop.Action.BROWSE ) ) {
		    JOptionPane.showMessageDialog(null, 
		    		"Desktop doesn't support the browse open action (fatal)\r\n" +
		    		"Please browse URL:" + donateURL,
		    		"Unable Open URL", JOptionPane.ERROR_MESSAGE);
		    return false;
		}
		
		try{
			desktop.browse(new java.net.URI(donateURL));
		}catch(Exception ex) {
		    JOptionPane.showMessageDialog(null, ex.getMessage(), 
		    		(String)ResourceUtil.get(IContext.ERROR), JOptionPane.ERROR_MESSAGE);
		    return false;
		}

		return true;
	}

	/**
	 * 批下载文件，单线程方式。如果部分下载成功，即不是全部成功，将返回false，并删除成功部分，维持初始状态。
	 * @param fs
	 * @param base
	 * @return
	 */
	public static boolean download(String[] fs, final String base) {
		try{
		File[] oldFs = new File[fs.length];
		File[] newFs = new File[fs.length];
		boolean isError = false;
		
		for (int i = 0; i < fs.length; i++) {
			final String fileName = fs[i];
			final String tmpFileName = "tmpV" + fileName;
			File tmpDownFile = new File(fileName);
			final File filev = new File(tmpFileName);
			oldFs[i] = tmpDownFile;
			newFs[i] = filev;
			if(HttpUtil.downloadFile(tmpDownFile, filev, fileName, base) == false){
				isError = true;
				break;
			}
		}
		
		if(isError == false){
			for (int i = 0; i < newFs.length; i++) {
				if(oldFs[i].delete() && newFs[i].renameTo(oldFs[i])){
				}else{
					isError = true;
					break;
				}
			}
		}
		
		if(isError){
			for (int j = 0; j <= fs.length; j++) {
				newFs[j].delete();
			}
			return false;
		}
		
		System.gc();
		
		return true;
		}catch (Exception e) {
			
		}
		return false;
	}

	private static boolean downloadFile(File starter, final File filev, final String fileName, final String base) {
		try{
			if((starter.exists() == false) || starter.setWritable(true)){
				InputStream is = null;
				FileOutputStream os = null;
				try{
					URL url = new URL(base + fileName);
					is = url.openStream();
					os = new FileOutputStream(filev);
					byte[] buf = new byte[1024]; //optimize the size of buffer to your need
				    int num;
				    while((num = is.read(buf)) != -1){
				        os.write(buf, 0, num);
				    }
				}finally{
					try{
						is.close();
					}catch (Exception e) {
					}
					try{
						os.close();
					}catch (Exception e) {
					}
				}
				return true;
			}else{
				return false;
			}
		}catch (Exception e) {
		}
		return false;
	}

	public static String replaceIPWithHC(String ip){
		final String root = RootConfig.getInstance().getProperty(RootConfig.p_RootRelayServer);
		if(ip.indexOf(root) >= 0){
			return "homecenter.mobi[" + ip + "]";
		}else{
			return ip;
		}
	}

	public static void notifyStopServer(final boolean isQuery, final JFrame parent) {
		if(ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF){
			if(isQuery){
				JPanel panel = new JPanel(new BorderLayout());
				panel.add(new JLabel("<html>service/configuration is changed and mobile is connecting," +
						"<BR>click '" + (String) ResourceUtil.get(IContext.OK) + "' to break off current mobile connection!" +
						"</html>", App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING), BorderLayout.CENTER);
				panel.add(new JLabel("<html><BR><strong>"+(String)ResourceUtil.get(IContext.TIP)+"</strong> : " +
						"<BR>you would <strong>NOT</strong> restart HomeCenter server," +
						"<BR>just re-login from mobile for the new service/configuration."), BorderLayout.SOUTH);
				final ActionListener listener = new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						doNotifyMobile();
					}
				};
				App.showCenterPanel(panel, 0, 0, "break off connection of mobile", false, null, null, listener, listener, parent, true, false, null, false, false);
			}else{
				doNotifyMobile();
			}
		}
	}

	private static void doNotifyMobile() {
		ContextManager.getContextInstance().send(MsgBuilder.E_TAG_SHUT_DOWN_BETWEEN_CS, RootServerConnector.getHideToken());
		
		//在模拟环境中，由于UDP的后滞性，E_TAG_SHUT_DOWN_BETWEEN_CS基本不被送出
		
		//等待数据包完全发送出去
		try{
			Thread.sleep(1000);
		}catch (Exception e) {
		}
	}
	
	public static boolean download(final File file, final URL url){
		file.delete();
		
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "rw");
		
			int start = 0;
	        int tryNum = 0;
			final int maxTryNum = 3;
			int downloadBS = 0;
			while(tryNum < maxTryNum){
	            try{
		        	HttpURLConnection conn = (HttpURLConnection) url.openConnection();  
		            conn.setRequestMethod("GET");  
//		            conn.setReadTimeout(0);//无穷  
					conn.setRequestProperty("Range", "bytes=" + start + "-");  
		            if (conn.getResponseCode() == 206) {  
		                raf.seek(start + downloadBS);  
		                InputStream inStream = conn.getInputStream();  
		                byte[] b = new byte[1024 * 10];  
		                int len = 0;  
		                while ((len = inStream.read(b)) != -1) {  
		                	raf.write(b, 0, len);  
		                    downloadBS += len;
		                }  
		            }else{
		            	L.V = L.O ? false : LogManager.log("http/206 not support download file.");
		            	break;
		            }
		            conn.disconnect();
		            return true;
	            }catch (Exception e) {
	            	e.printStackTrace();
	            	L.V = L.O ? false : LogManager.log("try more time to download.");
	            	tryNum++;
	            	try{
	            		Thread.sleep(1000);
	            	}catch (Exception ex) {
					}
				}
	        }
		}catch(Throwable t){
		}finally{
			try{
				raf.close();
			}catch (Throwable e) {
			}
		}
		return false;
	}
}
