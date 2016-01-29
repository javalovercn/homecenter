package hc.server.ui.design.hpj;

public class HPNode {
	public static final int MASK_ROOT = 1 << 10;
	public static final int MASK_MENU = 1 << 9;
	
	public static final int MASK_MSB_FOLDER = 5 << 9;
	public static final int MASK_MSB_ITEM = 5 << 8;
	public static final int MASK_MSB_ROBOT = 1 | MASK_MSB_ITEM;
	public static final int MASK_MSB_DEVICE = 2 | MASK_MSB_ITEM;
	public static final int MASK_MSB_CONVERTER = 3 | MASK_MSB_ITEM;

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

	public static final int MASK_SHARE_NATIVE_FOLDER = 3 << 6;
	public static final int MASK_SHARE_NATIVE = 3 << 5;

	public static final String MAP_FILE_JAR_TYPE = String.valueOf(MASK_RESOURCE_JAR);

	public static final int MASK_SUBMASK = 0xFFFFFFF0;

	public static final int TYPE_MENU_ITEM_CMD = 1 | MASK_MENU_ITEM;
	public static final int TYPE_MENU_ITEM_SCREEN = 2 | MASK_MENU_ITEM;
	public static final int TYPE_MENU_ITEM_CONTROLLER = 3 | MASK_MENU_ITEM;
	public static final int TYPE_MENU_ITEM_FORM = 4 | MASK_MENU_ITEM;
	public static final int TYPE_MENU_ITEM_SUB_MENU = 5 | MASK_MENU_ITEM;
	public static final int TYPE_MENU_ITEM_IOT = 6 | MASK_MENU_ITEM;
	public static final int TYPE_MENU_ITEM_CFG = 7 | MASK_MENU_ITEM;
	public static final int WIZARD_SELECTABLE_MENU_ITEM_SIZE = 4;

	public static String getTypeDesc(final int type) {
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
		}else if(type == TYPE_MENU_ITEM_CFG){
			return "CFG";
		}else if(type == TYPE_MENU_ITEM_IOT){
			return "IoT";
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

	public void setContext(final HPItemContext context) {
		this.context = context;
	}

	public HPNode(final int type, final String name) {
		this.type = type;
		this.name = name;
	}

	public static boolean isNodeType(final int nodeType, final int NodeTypeMask) {
		return (nodeType & MASK_SUBMASK) == NodeTypeMask;
	}
	
	@Override
	public String toString(){
		return name;
	}
	
	@Override
	public boolean equals(final Object obj){
		return toString().equals(obj.toString());
	}
	
	public String validate(){
		return null;
	}
}