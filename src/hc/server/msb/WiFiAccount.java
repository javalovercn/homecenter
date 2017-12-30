package hc.server.msb;

import hc.core.ConfigManager;

/**
 * AirCmds
 * <BR><BR>it is used to configuration WiFi account for physical equipment.
 * <BR><BR>for example, a real WiFi device is airconditioner, the server should broadcast WiFi account to it.
 * <BR>the full commands is <i>ssid:abc#efg;pwd:1234567890;security:WPA2_PSK</i> (NOTE : you should encrypt it here)<BR>
 * after invoke {@link Device#broadcastWiFiAccountAsSSID(String, String)}, 
 * <BR>the following SSIDs will be scanned on air : <BR><BR>
 * <i>AIRCOND56#1#ssid:abc#efg;pwd:123</i><BR>
 * <i>AIRCOND56#2#4567890;security:WPA</i><BR>
 * <i>AIRCOND56#3#2_PSK</i><BR><BR>
 * <STRONG>Important : </STRONG>it is split to three parts, because of the length of an SSID should be a maximum of 32.
 * <BR>a UTF char consists of multiple bytes, it can NOT be split to two commands, so the length of SSID may be less than 32.<BR>
 * <BR><i>56</i> is random integer for prevent conflicts, the system will scan air first and create unique random integer.
 * <BR>to decode from commands, user data is stored from the <STRONG>second</STRONG> '#', 
 * <BR>if same prefix "AIRCOND" with other random integer is scanned also, please choose stronger signal.
 * <BR><BR>After successful configure WiFi account, WiFi account may be changed in the future, if new account is created and input to server, {@link Device#notifyNewWiFiAccount(WiFiAccount)} will be invoked.
 * <BR><BR>A device or HAR package, which will broadcast WiFi account, may be added to server by scan QRCode (touch 'add' icon with QR pattern on your mobile).
 * <BR><BR>You can also broadcast AirCmds from your device to statement a HAR package web link without QRCode.
 * <BR>1. press start connection key on device.
 * <BR>2. your device will broadcast a command like <i>@AIRCOND56#1#http://my.com/a.har</i> (<STRONG>Important : </STRONG>it is begin with '@')
 * <BR>2.1 it is your duty to prevent conflicts if same model is adding from neighbours.
 * <BR>2.2 the air command will be removed in some seconds.
 * <BR>3. touch "add" icon with WiFi pattern (NOT 'add' icon with QR pattern) on your mobile,
 * <BR>4. your server will catch the HAR package web link from air, and download HAR package from Internet.
 * <BR><BR>AirCmds is free to personal and commerce, license is NOT required.
 * <BR><BR>Let's see the detail to broadcast WiFi account
 * <BR>1 after download HAR project to server.
 * <BR>2 if there is a <code>Device</code> in HAR, the method {@link Device#connect()} will be invoked to build connection to new device.
 * <BR>2.1 because of the first connection, <i>projectContext.getProperty("isWiFiSetted", "false").equals("false")</i> return true.
 * <BR>2.2 invoke @{@link Device#getWiFiAccount()} and {@link Device#broadcastWiFiAccountAsSSID(String, String)} to broadcast WiFi account to air. See the following AirCmds for more.
 * <BR>2.3 initial and wait for device connecting
 * <BR>2.4 the real device get WiFi account from air, and build connection to <code>Device</code>.
 * <BR>2.5 successful receive device connection.
 * <BR>2.6 <i>projectContext.setProperty("isWiFiSetted", "true")</i>
 * <BR>2.7 <i>projectContext.saveProperties()</i>
 * <BR><BR><STRONG>Tip : </STRONG>In Designer, click button to add <STRONG>Device</STRONG>, all above is written in template for you.
 */
public class WiFiAccount {
	private final String SSID;
	private final String password;
	private final String securityOption;
	
	public WiFiAccount(final String ssid, final String password, final String securityOption){
		this.SSID = ssid;
		this.password = password;
		this.securityOption = securityOption;
	}
	
	/**
	 * return the SSID of WiFi account, if none return empty string.
	 * @return
	 * @since 7.0
	 */
	public final String getSSID(){
		//方法名被模板使用，如重构改名，请同步
		return SSID;
	}
	
	/**
	 * return the password of the UUID, if none return empty string.
	 * @return
	 * @since 7.0
	 */
	public final String getPassword(){
		//方法名被模板使用，如重构改名，请同步
		return password;
	}
	
	public static final String SECURITY_OPTION_NO_PASSWORD = ConfigManager.WIFI_SECURITY_OPTION_NO_PASSWORD;
	public static final String SECURITY_OPTION_WEP = ConfigManager.WIFI_SECURITY_OPTION_WEP;
	public static final String SECURITY_OPTION_WPA_WPA2_PSK = ConfigManager.WIFI_SECURITY_OPTION_WPA_WPA2_PSK;
	
	/**
	 * @return one of {@link #SECURITY_OPTION_NO_PASSWORD}, {@link #SECURITY_OPTION_WEP}}, {@link #SECURITY_OPTION_WPA_WPA2_PSK}, and other in the future.
	 * @since 7.0
	 */
	public final String getSecurityOption(){
		//方法名被模板使用，如重构改名，请同步
		return securityOption;
	}
}
