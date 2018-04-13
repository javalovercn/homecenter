package hc.server.ui.design.code;

import hc.core.util.StringUtil;

public class ClassImportItem {
	public String name;
	public String[] names;
	public int size;
	
	public final void addCName(final String cName) {
		if(names == null) {
			if(name == null) {
				name = cName;
				return;
			}else {
				names = new String[2];
				names[0] = name;
				names[1] = cName;
				size = 2;
				
				name = null;
				return;
			}
		}else {
			if(size == names.length) {
				names = StringUtil.doubleSize(names);
			}
			names[size++] = cName;
		}
	}
}
