package hc.server.ui.design;

import hc.core.IConstant;
import hc.core.util.HCURL;
import hc.server.ui.MForm;

public class TestMForm extends MForm {

	public String getID() {
		return "/form1";
	}

	public short getIOMode() {
		return IConstant.IOMODE_IN_OUT;
	}

	public String getInit() {
		return null;//"{}";
	}

	public static String d_cg = "{'ChoiceLabel','1',['男','女'],['Sys_Img','Sys_Img']}, 'Init'={['false','true'],'1'}, 'IO'='3', 'ID'='id_cg'";
	public static String d_df = "{'Datelabel', '3', 'America/Los_Angeles'}, 'Init'={'1326008885609'}, 'IO'='3', 'ID'='id_df'";
	public static String d_img = "{'ImgLabel', 'Sys_Img', '0', 'altText', '1'}, 'Init'={'iniTxt', 'iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAZiS0dEAP8A/wD/oL2nkwAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB9sJFAwmIeDrjusAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAACXElEQVQ4y1WQTYhNYRjHf+9xGTVxZZSFz9E9jERZmGaioREy"+
				"rAgNDbNgYaZGytJKKRvFQqaQ3DsTJhYyGJpZkG8rSnHvxkZqSucc19R1z9znb3HPNde/3nqf932e/8cDgAVgIQAoJK2AE/aHLnVwpOcZpWbxMGP0+aIRICPwVe3HAv5BIQMW8EkhawEEG7ufo1VCGUO+EWfEyVq/L0AJgQKuKqRoAYv+EcK+e71cXC7O+sY7v0oiX1wB8K3mIqTPQmQBewC0I3mHp4LROsUWX4xkhHzRX4vQ"+
				"qJCKBXykDgabDKTqaa3/8439vpjKGPNSznFMwgOG66wvFLxP7gCvBEscTApwHvczRsU5elGRUf1CEm3JQLvBG0FbzYHBYcFtQXu9kyXikMcEnXoJaqfVYEzwGkg5eOuqQ0NUdzEteC0YMzgoGrZd6Uk1pR6d7zqz58PjScF6YD6Aq1NxSQwHLomjkr956c+D5+62xpUYTDvrFjdXcFwwYdCmmSV2C+4YbAH4cl/t+aGK8sN6"+
				"mKLISiKtI+0+e1ACrht8czDBjHpOsMyDHwCzpuKtVWv2xANGgAeEtqLmxINxQb9gOonTURtOcBhhkm5Vy8hOE5oR6oQLKrMSZU/wwmAc4PeGXQAUcvHefK6sQrbcx3+IdIPQRKTvRLqEtPuz3zIoOFBryWfLTYVcXCzkyoPVOgYizZCEdopQRiQxJXVfG5JgI0AhW15TyMUf89nyQK29kItJkXYQGaQ9WOBdJrSb4HqBrlJD"+
				"w3aaO1u+XphYrPL0Uoc6Vh+dE+VzZRwOv2c2fwEzP0r3y/J05gAAAABJRU5ErkJggg==', '2'}, 'IO'='2', 'ID'='id_img'";
	public static String d_space = "{'10', '30'}";
	public static String d_str = "{'label', 'text', '0'}, 'Init'={{'0', '1','16'},'iniTxt'}, 'IO'='2', 'ID'='id_str'";
	public static String d_txtFld = "{'label', 'text', '30', '0'}, 'Init'={'0','UCB_BASIC_LATIN','initTxt'}, 'IO'='3', 'ID'='id_tf'";
	//URLAction(String label, String text, int appearanceMode, String url, Font font, int PreferredWidth, int PreferredHeight)
	public static String d_urlAction = "{'URLAction','Home','2','CmdTitle','screen://" + HCURL.REMOTE_HOME_SCREEN + "',{'64','2','16'}}";
	
	public static String d_form_item = "[{'ChoiceGroup':"+ d_cg +"}]";

	public String getItems() {
		return "{[" +
				"{'ChoiceGroup':"+d_cg+"}," +
				"{'DateField':"+d_df+"}," +
				"{'ImageItem':"+d_img+"}," +
				"{'Spacer':"+d_space+"}," +
				"{'URLAction':"+d_urlAction+"}," +
				"{'StringItem':"+d_str+"}," +
				"{'TextField':"+d_txtFld+"}," +
				"{'MIDTone', 'Init' : <'50', '50', '50'>, 'IO'='2'}," +
				"] }";
	}

	public String getTitle() {
		return "测试Form1";
	}

	@Override
	public void onStart() {
	}

	@Override
	public void onPause() {
	}

	@Override
	public void onResume() {
	}

	@Override
	public void onExit() {
	}

}
