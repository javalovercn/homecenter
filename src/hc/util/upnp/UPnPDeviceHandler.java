package hc.util.upnp;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class UPnPDeviceHandler extends DefaultHandler {

	private static final String _CTRL_MODE = "ctrl";
	private static final String _SERVERTYPE_MODE = "type";
	private static final String _DESC = "desc";

	private UPnPDevice upnpDevice;

	public UPnPDeviceHandler(UPnPDevice device) {
		this.upnpDevice = device;
	}

	private String element;
	private String mode = _DESC;

	public void startElement(String uri, String localName, String qName, Attributes attributes)
			throws SAXException {
		element = localName;
		if (mode == _DESC && element.equalsIgnoreCase("serviceList")) {
			mode = _SERVERTYPE_MODE;
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		element = "";
		if (localName.equalsIgnoreCase("service")) {
			String serviceType = upnpDevice.SERVICETYPE;
			if (serviceType != null && serviceType
					.startsWith("urn:schemas-upnp-org:service:WANCommonInterfaceConfig", 0))
				upnpDevice.isGateWay = true;
			mode = _CTRL_MODE;
		}
	}

	public static final String TAG_CONTROL_URL = "controlURL";
	public static final String TAG_SCPDURL = "SCPDURL";
	public static final String TAG_SERVICE_TYPE = "serviceType";

	public void characters(char[] chars, int offset, int length) throws SAXException {
		if (element.equalsIgnoreCase("URLBase"))
			upnpDevice.setURLBase(new String(chars, offset, length));
		else {
			if (mode == _SERVERTYPE_MODE || mode == _DESC) {
				if (mode == _DESC) {
					if (element.equalsIgnoreCase("friendlyName"))
						upnpDevice.setFriendlyName(new String(chars, offset, length));
					else if (element.equalsIgnoreCase("modelNumber"))
						upnpDevice.setModelNumber(new String(chars, offset, length));
				}
				if (element.equalsIgnoreCase(TAG_SERVICE_TYPE))
					upnpDevice.setServiceType(new String(chars, offset, length));
			} else if (mode == _CTRL_MODE) {
				if (element.equalsIgnoreCase(TAG_SERVICE_TYPE))
					upnpDevice.setServiceType(new String(chars, offset, length));
				else if (element.equalsIgnoreCase(TAG_CONTROL_URL))
					upnpDevice.setControlURL(new String(chars, offset, length));
				else if (element.equalsIgnoreCase(TAG_SCPDURL))
					upnpDevice.setSCPDURL(new String(chars, offset, length));
			}
		}
	}

}
