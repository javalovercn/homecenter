package hc.server;

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.MsgBuilder;
import hc.core.data.DataClientAgent;
import hc.core.util.CCoreUtil;
import hc.core.util.HCURL;
import hc.core.util.IHCURLAction;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.server.data.screen.ScreenCapturer;
import hc.server.ui.ClientDesc;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SingleMessageNotify;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class J2SEServerURLAction implements IHCURLAction {
	
	public boolean doBiz(HCURL url) {
		
		final String protocal = url.protocal;
		final String elementID = url.elementID;
		
//		hc.core.L.V=hc.core.L.O?false:LogManager.log("goto " + url.url);
		
		if(protocal == HCURL.SCREEN_PROTOCAL){
			if(elementID.equals(HCURL.REMOTE_HOME_SCREEN)){			
				//主菜单模式下，再次进入时，如果不增加如下时间，会导致块上白
//				try{
//					Thread.sleep(2000);
//				}catch (Exception e) {
//				}

				//送回本地的
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Dimension screenSize = toolkit.getScreenSize();
//				screenSize.width = 280;
//				screenSize.height = 300;
				{
					final byte[] homeBS = new byte[MsgBuilder.UDP_BYTE_SIZE];
					final DataClientAgent ss = new DataClientAgent();
					ss.setBytes(homeBS);
					
					ss.setWidth(screenSize.width);
					ss.setHeight(screenSize.height);
					ContextManager.getContextInstance().send(MsgBuilder.E_SCREEN_REMOTE_SIZE, homeBS, ss.getLength());
				}
				
				ScreenServer.cap = new ScreenCapturer(ClientDesc.clientWidth, ClientDesc.clientHeight,
						screenSize.width, screenSize.height);
				
				ScreenServer.pushScreen(ScreenServer.cap);
				
				return true;
			}
		}else if(protocal == HCURL.CMD_PROTOCAL){
			if(elementID.equals(HCURL.DATA_CMD_EXIT)){
				if(ScreenServer.popScreen() == false){
//					System.out.println("Receiv Exit , and Notify exit by mobile");
					J2SEContext.notifyExitByMobi();
				}
				
				return true;
			}else if(elementID.equals(HCURL.DATA_CMD_SendPara)){
				String para1= url.getParaAtIdx(0);
				if(para1 != null && para1.equals(HCURL.DATA_PARA_RIGHT_CLICK)){
					String value1 = url.getValueofPara(para1);
					if(value1 != null){
						if(value1.equals(HCURL.DATA_PARA_VALUE_CTRL)){
							int x = Integer.parseInt(url.getValueofPara("x"));
							int y = Integer.parseInt(url.getValueofPara("y"));

							hc.core.L.V=hc.core.L.O?false:LogManager.log(ScreenCapturer.OP_STR + "Ctrl + mouse left click at (" + x + ", " + y + ")");
							
							doClickAt(url, ResourceUtil.getAbstractCtrlKeyCode(), x, y);
							return true;
						}else if(value1.equals(HCURL.DATA_PARA_VALUE_SHIFT)){
							int x = Integer.parseInt(url.getValueofPara("x"));
							int y = Integer.parseInt(url.getValueofPara("y"));

							hc.core.L.V=hc.core.L.O?false:LogManager.log(ScreenCapturer.OP_STR + "Shift + mouse left click at (" + x + ", " + y + ")");
							
							doClickAt(url, KeyEvent.VK_SHIFT, x, y);
							return true;
						}
					}
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_CERT_RECEIVED)){
					final String value1 = url.getValueofPara(para1);
					if(value1.equals(CCoreUtil.RECEIVE_CERT_OK)){
						J2SEContext.isTransedToMobileSize = true;
					}

					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_RELOAD_THUMBNAIL)){
					final ScreenCapturer sc = ScreenServer.cap;
					if(sc != null){
						sc.clearCacheThumbnail();
					}
					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_FORBID_CERT_UPDATE)){
					final String value1 = url.getValueofPara(para1);
					if(value1.equals(IConstant.TRUE)){
						if(SingleMessageNotify.isShowMessage(SingleMessageNotify.TYPE_DIALOG_CERT) == false){
							new Thread(){
								public void run(){
									SingleMessageNotify.setShowToType(SingleMessageNotify.TYPE_DIALOG_CERT, true);
									
									//UDP可能导致数据后于disable对话框
									try{
										Thread.sleep(2000);
									}catch (Exception e) {
									}
	//							if(IDArrayGroup.checkAndAdd(IDArrayGroup.MSG_FORBID_UPDATE_CERT)){
									ActionListener al = new ActionListener() {
										@Override
										public void actionPerformed(ActionEvent e) {
											SingleMessageNotify.setShowToType(SingleMessageNotify.TYPE_DIALOG_CERT, false);
										}
									};
									JPanel msgpanel = new JPanel(new BorderLayout());
									msgpanel.add(new JLabel("<html><body style=\"width:500\"><strong>" + (String)ResourceUtil.get(IContext.TIP) + "</strong>" +
											"" + StringUtil.replace((String)ResourceUtil.get(9062), "{forbid}", CCoreUtil.FORBID_UPDATE_CERT)+
											"</body></html>",
											App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEFT), BorderLayout.CENTER);
									App.showCenterPanel(msgpanel, 0, 0, (String)ResourceUtil.get(IContext.TIP), false, null, null, al, al, null, false, true, null, false, true);
//							}
								}
							}.start();
						}
					}

					return true;
				}
			}
		}
		
		return ServerUIUtil.getResponsor().doBiz(url);
	}

	private void doClickAt(HCURL url, int mode, int x, int y) {
		ScreenCapturer.robot.mouseMove(x, y);
		
		ScreenCapturer.robot.keyPress(mode);

		ScreenCapturer.robot.mousePress(InputEvent.BUTTON1_MASK);
		ScreenCapturer.robot.mouseRelease(InputEvent.BUTTON1_MASK);

		ScreenCapturer.robot.keyRelease(mode);
	}

}
