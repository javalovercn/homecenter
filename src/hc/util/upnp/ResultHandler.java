package hc.util.upnp;

import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ResultHandler extends DefaultHandler {

    private Map<String,String> result;

    private String element;

    public void characters(char[] chars, int offset, int length)
            throws SAXException {
        if (element != null) {
            String value = new String(chars,offset,length);
            String old = result.put(element, value);
            if (old != null) {
                result.put(element, old + value);
            }
        }
    }
    
    public ResultHandler(Map<String,String> result) {
        this.result = result;
    }

    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        element = localName;
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        element = null;
    }
}
