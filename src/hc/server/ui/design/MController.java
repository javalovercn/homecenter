package hc.server.ui.design;

import hc.core.util.HCURL;
import hc.core.util.StoreableHashMap;
import hc.server.ui.MUIView;
import hc.util.StringBuilderCacher;

public class MController extends MUIView {
	final StoreableHashMap map;
	final String map_str;
	public MController(final StoreableHashMap map, final String map_str) {
		this.map = map;
		this.map_str = map_str;
	}

	@Override
	public String buildJcip(final J2SESession coreSS) {
		final StringBuilder sb = StringBuilderCacher.getFree();
		
		sb.append("{'" + HCURL.CONTROLLER_PROTOCAL + "' : ");
		
		sb.append('{');
		
			appendString(sb, map_str, true);
//				
//			JcipManager.appendArray(sb, getIcons(), true);
//			
//			JcipManager.appendArray(sb, getIconLabels(), true);
//			
//			JcipManager.appendArray(sb, getURLs(), true);
//			
//			appendInt(sb, getNumRow(), true);
//			
//			appendInt(sb, getNumCol(), true);
//			
//			appendBool(sb, isFullMode(), true);
//			
//			appendInt(sb, getSize(), false);
			
		sb.append("}}");
		
		final String out = sb.toString();
		StringBuilderCacher.cycle(sb);
		return out;
	}

}
