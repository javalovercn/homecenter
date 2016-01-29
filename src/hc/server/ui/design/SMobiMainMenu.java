package hc.server.ui.design;

import hc.core.util.HCURL;

public class SMobiMainMenu extends MCanvasMenu {
	public String[][] items = {
			{"远屏", "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAZiS0dEAP8A/wD/oL2nkwAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB9sJFAwmIeDrjusAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAACXElEQVQ4y1WQTYhNYRjHf+9xGTVxZZSFz9E9jERZmGaioREy"+
				"rAgNDbNgYaZGytJKKRvFQqaQ3DsTJhYyGJpZkG8rSnHvxkZqSucc19R1z9znb3HPNde/3nqf932e/8cDgAVgIQAoJK2AE/aHLnVwpOcZpWbxMGP0+aIRICPwVe3HAv5BIQMW8EkhawEEG7ufo1VCGUO+EWfEyVq/L0AJgQKuKqRoAYv+EcK+e71cXC7O+sY7v0oiX1wB8K3mIqTPQmQBewC0I3mHp4LROsUWX4xkhHzRX4vQ"+
				"qJCKBXykDgabDKTqaa3/8439vpjKGPNSznFMwgOG66wvFLxP7gCvBEscTApwHvczRsU5elGRUf1CEm3JQLvBG0FbzYHBYcFtQXu9kyXikMcEnXoJaqfVYEzwGkg5eOuqQ0NUdzEteC0YMzgoGrZd6Uk1pR6d7zqz58PjScF6YD6Aq1NxSQwHLomjkr956c+D5+62xpUYTDvrFjdXcFwwYdCmmSV2C+4YbAH4cl/t+aGK8sN6"+
				"mKLISiKtI+0+e1ACrht8czDBjHpOsMyDHwCzpuKtVWv2xANGgAeEtqLmxINxQb9gOonTURtOcBhhkm5Vy8hOE5oR6oQLKrMSZU/wwmAc4PeGXQAUcvHefK6sQrbcx3+IdIPQRKTvRLqEtPuz3zIoOFBryWfLTYVcXCzkyoPVOgYizZCEdopQRiQxJXVfG5JgI0AhW15TyMUf89nyQK29kItJkXYQGaQ9WOBdJrSb4HqBrlJD"+
				"w3aaO1u+XphYrPL0Uoc6Vh+dE+VzZRwOv2c2fwEzP0r3y/J05gAAAABJRU5ErkJggg==", HCURL.SCREEN_PROTOCAL + "://" + HCURL.REMOTE_HOME_SCREEN},
			{"SubMenu", "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"+
					"tKkq1wAAAABJRU5ErkJggg==", "menu://no1"},
			{"SubForm", "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAZiS0dEAP8A/wD/oL2nkwAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB9sJFAwmIeDrjusAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAACXElEQVQ4y1WQTYhNYRjHf+9xGTVxZZSFz9E9jERZmGaioREy"+
				"rAgNDbNgYaZGytJKKRvFQqaQ3DsTJhYyGJpZkG8rSnHvxkZqSucc19R1z9znb3HPNde/3nqf932e/8cDgAVgIQAoJK2AE/aHLnVwpOcZpWbxMGP0+aIRICPwVe3HAv5BIQMW8EkhawEEG7ufo1VCGUO+EWfEyVq/L0AJgQKuKqRoAYv+EcK+e71cXC7O+sY7v0oiX1wB8K3mIqTPQmQBewC0I3mHp4LROsUWX4xkhHzRX4vQ"+
				"qJCKBXykDgabDKTqaa3/8439vpjKGPNSznFMwgOG66wvFLxP7gCvBEscTApwHvczRsU5elGRUf1CEm3JQLvBG0FbzYHBYcFtQXu9kyXikMcEnXoJaqfVYEzwGkg5eOuqQ0NUdzEteC0YMzgoGrZd6Uk1pR6d7zqz58PjScF6YD6Aq1NxSQwHLomjkr956c+D5+62xpUYTDvrFjdXcFwwYdCmmSV2C+4YbAH4cl/t+aGK8sN6"+
				"mKLISiKtI+0+e1ACrht8czDBjHpOsMyDHwCzpuKtVWv2xANGgAeEtqLmxINxQb9gOonTURtOcBhhkm5Vy8hOE5oR6oQLKrMSZU/wwmAc4PeGXQAUcvHefK6sQrbcx3+IdIPQRKTvRLqEtPuz3zIoOFBryWfLTYVcXCzkyoPVOgYizZCEdopQRiQxJXVfG5JgI0AhW15TyMUf89nyQK29kItJkXYQGaQ9WOBdJrSb4HqBrlJD"+
				"w3aaO1u+XphYrPL0Uoc6Vh+dE+VzZRwOv2c2fwEzP0r3y/J05gAAAABJRU5ErkJggg==", "form://form1"},
			{"退出", "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAZiS0dEAP8A/wD/oL2nkwAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB9sJFAwmIeDrjusAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAACXElEQVQ4y1WQTYhNYRjHf+9xGTVxZZSFz9E9jERZmGaioREy"+
				"rAgNDbNgYaZGytJKKRvFQqaQ3DsTJhYyGJpZkG8rSnHvxkZqSucc19R1z9znb3HPNde/3nqf932e/8cDgAVgIQAoJK2AE/aHLnVwpOcZpWbxMGP0+aIRICPwVe3HAv5BIQMW8EkhawEEG7ufo1VCGUO+EWfEyVq/L0AJgQKuKqRoAYv+EcK+e71cXC7O+sY7v0oiX1wB8K3mIqTPQmQBewC0I3mHp4LROsUWX4xkhHzRX4vQ"+
				"qJCKBXykDgabDKTqaa3/8439vpjKGPNSznFMwgOG66wvFLxP7gCvBEscTApwHvczRsU5elGRUf1CEm3JQLvBG0FbzYHBYcFtQXu9kyXikMcEnXoJaqfVYEzwGkg5eOuqQ0NUdzEteC0YMzgoGrZd6Uk1pR6d7zqz58PjScF6YD6Aq1NxSQwHLomjkr956c+D5+62xpUYTDvrFjdXcFwwYdCmmSV2C+4YbAH4cl/t+aGK8sN6"+
				"mKLISiKtI+0+e1ACrht8czDBjHpOsMyDHwCzpuKtVWv2xANGgAeEtqLmxINxQb9gOonTURtOcBhhkm5Vy8hOE5oR6oQLKrMSZU/wwmAc4PeGXQAUcvHefK6sQrbcx3+IdIPQRKTvRLqEtPuz3zIoOFBryWfLTYVcXCzkyoPVOgYizZCEdopQRiQxJXVfG5JgI0AhW15TyMUf89nyQK29kItJkXYQGaQ9WOBdJrSb4HqBrlJD"+
				"w3aaO1u+XphYrPL0Uoc6Vh+dE+VzZRwOv2c2fwEzP0r3y/J05gAAAABJRU5ErkJggg==", "cmd://exit"},
			{"配置", "Sys_Img", "cmd://config"},
			{"ab22out", "Sys_Img", "cmd://about"},
			{"ab33out", "Sys_Img", "cmd://about"},
			{"ab44out", "Sys_Img", "cmd://about"},
			{"ab55out", "Sys_Img", "cmd://about"},
			{"HelloAbout", "Sys_Img", "cmd://about"},
			{"中华人民共和国", "Sys_Img", "cmd://about"},
			{"ab66out", "Sys_Img", "cmd://about"},
			{"ab77out", "Sys_Img", "cmd://about"},
			};

//	public String[][] items = {
//			{"Remote", "Sys_Img", "screen://ab"},//远屏
//			{"SubMenu", "Sys_Img", "menu://no1"},
//			{"SubForm", "Sys_Img", "form://form1"},
//			{"Exit", "Sys_Img", "cmd://exit"}};//退出
	
	public SMobiMainMenu() {
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
//				e.printStackTrace();
//			}
//
//		    ByteUtil.byteArrayCacher.cycle(out);
//		    
//		    return bi;
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
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
		return 3;
	}

	public int getNumRow() {
		return (items.length/3 + 1);
	}

	public int getSize() {
		return items.length;
	}

	public String getTitle() {
		return "";//"你好，远屏HelloRemote";//;
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
