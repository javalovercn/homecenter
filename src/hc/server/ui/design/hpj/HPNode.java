package hc.server.ui.design.hpj;

public class HPNode {
	public static final int MASK_ROOT = 1 << 10;
	public static final int MASK_MENU = 1 << 9;
	public static final int MASK_SHARE_TOP = 1 << 8;
	public static final int MASK_SHARE_RB_FOLDER = 1 << 7;
	public static final int MASK_SHARE_RB = 1 << 5;
	public static final int MASK_MENU_ITEM = 1 << 6;

	public static final int MASK_RESOURCE_FOLDER = 3 << 10;
	public static final int MASK_RESOURCE_FOLDER_OTHER = 1 | MASK_RESOURCE_FOLDER;
	public static final int MASK_RESOURCE_FOLDER_JAR = 2 | MASK_RESOURCE_FOLDER;
	public static final int MASK_RESOURCE_ITEM = 3 << 9;
	public static final int MASK_RESOURCE_JAR = 1 | MASK_RESOURCE_ITEM;
	public static final int MASK_RESOURCE_PNG = 2 | MASK_RESOURCE_ITEM;
	public static final int MASK_RESOURCE_AU = 3 | MASK_RESOURCE_ITEM;
	
	public static final int MASK_EVENT_FOLDER = 3 << 8;
	public static final int MASK_EVENT_ITEM = 3 << 7;

	public static final int MASK_SUBMASK = 0xFFFFFFF0;

	public static final int TYPE_MENU_ITEM_CMD = 1 | MASK_MENU_ITEM;
	public static final int TYPE_MENU_ITEM_SCREEN = 2 | MASK_MENU_ITEM;
	public static final int TYPE_MENU_ITEM_CONTROLLER = 3 | MASK_MENU_ITEM;
	public static final int TYPE_MENU_ITEM_SUB_MENU = 4 | MASK_MENU_ITEM;
	public static final int TYPE_MENU_ITEM_FORM = 5 | MASK_MENU_ITEM;
	public static final int TYPE_MENU_ITEM_SIZE = 3;

	public static String getTypeDesc(int type) {
		if(type == TYPE_MENU_ITEM_CMD){
			return "CMD";
		}else if(type == TYPE_MENU_ITEM_SCREEN){
			return "SCREEN";
		}else if(type == TYPE_MENU_ITEM_CONTROLLER){
			return "CONTROLLER";
		}else if(type == TYPE_MENU_ITEM_FORM){
			return "FORM";
		}else if(type == TYPE_MENU_ITEM_SUB_MENU){
			return "MENU";
		}else{
			return "";//"UNKNOW";
		}
	}

	public int type;
	public String name;
	private HPItemContext context;
	
	public HPItemContext getContext() {
		return context;
	}

	public void setContext(HPItemContext context) {
		this.context = context;
	}

	public HPNode(int type, String name) {
		this.type = type;
		this.name = name;
	}

	public static boolean isNodeType(int nodeType, int NodeTypeMask) {
		return (nodeType & MASK_SUBMASK) == NodeTypeMask;
	}
	
	public String toString(){
		return name;
	}
	
	public boolean equals(Object obj){
		return toString().equals(obj.toString());
	}
	
	public String validate(){
		return null;
	}
}