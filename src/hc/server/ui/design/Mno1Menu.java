package hc.server.ui.design;

import hc.core.util.HCURL;


public class Mno1Menu extends MCanvasMenu {

	public String[][] items = {
			{"Remote", "Sys_Img", "screen://" + HCURL.REMOTE_HOME_SCREEN},//远屏
			{"SubMenu", "Sys_Img", "menu://no1"},
			{"SubForm", "Sys_Img", "form://form1"},
			{"Exit", "Sys_Img", "cmd://exit"}};//退出
	
	public Mno1Menu() {
	}
	
//	public static BufferedImage decodeBase64ToImage(String str) {    
//	    byte[] data;
//	    BufferedImage bi = null;
//		try {
//			data = str.getBytes(IConstant.UTF_8);
//		    int len = data.length;    
//		    byte[] out = ByteUtil.byteArrayCacher.getFree(len);
//		    int outsize = ByteUtil.convertToSize(data, len, out);
//
//			InputStream in = new ByteArrayInputStream(out, 0, outsize);
//			try {
//				bi = ImageIO.read(in);
//			} catch (IOException e) {
//				ExceptionReporter.printStackTrace(e);
//			}
//
//		    ByteUtil.byteArrayCacher.cycle(out);
//		    
//		    return bi;
//		} catch (UnsupportedEncodingException e) {
//			ExceptionReporter.printStackTrace(e);
//		}    
//		return null;
//	}
//	
//
//	public static void main(String[] args) {
//		SMobiMainMenu m = new SMobiMainMenu();
//		String[] base64Image = new String[m.items.length];
//		for (int i = 0; i < base64Image.length; i++) {
//			base64Image[i] = m.items[i][1];
//		}
//		
//		BufferedImage[] images = new BufferedImage[base64Image.length];
//		for (int i = 0; i < images.length; i++) {
//			images[i] = decodeBase64ToImage(base64Image[i]);
//		}
//		
//		JDialog d = new HCJDialog();
//		d.setSize(300, 200);
//		d.setIconImage(images[1]);
//		d.show();
//	}
//	
	public String[] getIconLabels() {
		String[] out = new String[items.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = items[i][0];
		}
		return out;
	}

	public String[] getIcons() {
		String[] out = new String[items.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = items[i][1];
		}
		return out;
	}

	public int getNumCol() {
		return 2;
	}

	public int getNumRow() {
		return 2;
	}

	public int getSize() {
		return items.length;
	}

	public String getTitle() {
		return "你好，NO1";//"HelloRemote";//;
	}

	public String[] getURLs() {
		String[] out = new String[items.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = items[i][2];
		}
		return out;
	}

	public boolean isFullMode() {
		return false;
	}

}
