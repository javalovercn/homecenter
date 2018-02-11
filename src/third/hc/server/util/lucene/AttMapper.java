package third.hc.server.util.lucene;

import java.util.HashMap;
import java.util.Iterator;

import third.apache.lucene.util.Attribute;

public class AttMapper {
	final static HashMap<Class, Attribute> map = new HashMap<Class, Attribute>(10);
	
	static {
		HashMap<String, String> mss = load();
		final Iterator<String> it = mss.keySet().iterator();
		while(it.hasNext()){
			final String key = it.next();
			final String attClass = mss.get(key);
			
			try{
				map.put(Class.forName(key), (Attribute)Class.forName(attClass).newInstance());
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static Attribute getAttribute(final Class claz){
		return map.get(claz);
	}
	
	private static HashMap<String, String> load(){
		final HashMap<String, String> map = new HashMap<String, String>();
		
		map.put("third.apache.lucene.analysis.tokenattributes.OffsetAttribute", "third.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl");
		map.put("third.apache.lucene.analysis.tokenattributes.TypeAttribute", "third.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl");
		map.put("third.apache.lucene.analysis.tokenattributes.KeywordAttribute", "third.apache.lucene.analysis.tokenattributes.KeywordAttributeImpl");
		map.put("third.apache.lucene.analysis.tokenattributes.PositionLengthAttribute", "third.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl");
		map.put("third.apache.lucene.analysis.tokenattributes.CharTermAttribute", "third.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl");
		map.put("third.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute", "third.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl");

		return map;
	}
}
