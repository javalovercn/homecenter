package hc.server.ui.design.code;

import hc.core.util.Stack;

import java.util.ArrayList;

public class CodeItem implements Comparable<CodeItem>{
	private final static Stack free = new Stack(1024);
	
	public final static CodeItem getFree(){
		final CodeItem out = (CodeItem)free.pop();
		if(out != null){
			return out;
		}else{
			return new CodeItem();
		}
	}
	
	@Override
	public final String toString(){
		String typeStr = "";
		if(type == TYPE_PACKAGE){
			typeStr = "package";
		}else if(type == TYPE_CLASS){
			typeStr = "class";
		}else if(type == TYPE_RESOURCES){
			typeStr = "resources";
		}else if(type == TYPE_METHOD){
			typeStr = "method";
		}else if(type == TYPE_FIELD){
			typeStr = "field";
		}
		return code + ", type : " + typeStr;
	}
	
	public final static void cycle(final CodeItem item){
		item.type = 0;
		item.isPublic = false;
		item.isForMaoHaoOnly = false;
		item.code = "";
		item.fmClass = Object.class.getName();
		item.codeDisplay = "";
		item.codeLowMatch = "";
		free.push(item);
	}
	
	public final static int TYPE_IMPORT = 1;
	public final static int TYPE_PACKAGE = 2;
	public final static int TYPE_CLASS = 3;
	public final static int TYPE_VARIABLE = 4;
	public final static int TYPE_RESOURCES = 5;
	public final static int TYPE_METHOD = 6;
	public final static int TYPE_FIELD = 7;
	
	public int type;
	public boolean isPublic;
	public boolean isForMaoHaoOnly;
	public String code;
	public String fmClass;
	public String codeDisplay;
	public String codeLowMatch;
	
	@Override
	public final int compareTo(final CodeItem o) {
		final int result = this.type - o.type;
		if(result == 0){
			return this.codeLowMatch.compareTo(o.codeLowMatch);//可将bor/Border规在一块
		}else{
			return result;
		}
	}

	public final static boolean contains(final ArrayList<CodeItem> list, final String code) {
		final int sizeList = list.size();
		boolean findSameName = false;
		for (int j = 0; j < sizeList; j++) {
			if(list.get(j).code.equals(code)){
				findSameName = true;
				break;
			}
		}
		return findSameName;
	}
}
