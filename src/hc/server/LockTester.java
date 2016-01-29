package hc.server;

import hc.App;
import hc.util.ResourceUtil;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class LockTester {
	private final static int step0Cancle = 0, step1Query = 1, step2Login = 2, step3LockScreen = 3, step5Finish = 5;
	
	private final static ActionListener cancleListener = new HCActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			stepNow = step0Cancle;
		}
	};
	final static String[] text = {
			"{9070}<BR><BR>in some OS and JRE environment, mobile will display black if current screen is locked. <BR>" +
					"click '{1029}' to test it now.",
			"login from mobile<BR><OL>" +
					"<LI>start mobile App,</LI>" +
					"<LI>input <STRONG>[{uuid}]</STRONG> and password,</LI>" +
					"<LI>press '{1010}' button on mobile to connect.</LI>" +
					"<LI>if you see this screen on mobile, click '{1029}'.</LI></OL>",
			""
	};
	private static int stepNow = 0;
	public static void startLockTest(){
		final String testLockingScreen = (String)ResourceUtil.get(9070);
		ActionListener nextActoin = null;
		if(stepNow == step0Cancle){
			nextActoin = new HCActionListener(new Runnable() {
				@Override
				public void run() {
					stepNow = step1Query;
					startLockTest();
				}
			}, App.getThreadPoolToken());
		}else if(stepNow == step1Query){
			nextActoin = new HCActionListener(new Runnable() {
				@Override
				public void run() {
					stepNow = step2Login;
					startLockTest();
				}
			}, App.getThreadPoolToken());
		}else if(stepNow == step2Login){
			nextActoin = new HCActionListener(new Runnable() {
				@Override
				public void run() {
					stepNow = step3LockScreen;
					startLockTest();
				}
			}, App.getThreadPoolToken());
		}else if(stepNow == step3LockScreen){
			nextActoin = new HCActionListener(new Runnable() {
				@Override
				public void run() {
					String howToLock = (String)ResourceUtil.get(9071);
					if(ResourceUtil.isWindowsOS()){
						howToLock = "<UL><LI>按下Win+L</LI><LI>选择“开始→注销”，然后点击“切换用户”</LI></UL>";
						//<UL><LI>press Win+L</LI><LI>click <STRONG>Start</STRONG> button, select <STRONG>logout</STRONG>开始→注销”，然后点击“切换用户”</LI></UL>
					}
					
					stepNow = step0Cancle;
				}
			}, App.getThreadPoolToken());
		}
		
		showStepDialog(testLockingScreen, 
				ResourceUtil.replaceWithI18N(text[stepNow]), 
				(stepNow==step5Finish? (String)ResourceUtil.get(1034): (String)ResourceUtil.get(1029)),//下一步/完成
				nextActoin, cancleListener);
	}
	
	private static void showStepDialog(final String title, final String text, final String nextOrFinish, final ActionListener listener, final ActionListener cancle){
		JPanel panel = new JPanel(new BorderLayout());
//		try {
			panel.add(new JLabel("<html><body style=\"width:600\">" + text + "</body></html>", 
					null, SwingConstants.LEADING), BorderLayout.CENTER);//new ImageIcon(ImageIO.read(ImageSrc.OK_ICON))
//			panel.add(new JLabel("<html><br>The more powerful [" + (String)ResourceUtil.get(9034) + "] is ready for you now!" +
//					"</html>"), BorderLayout.CENTER);
//		} catch (IOException e) {
//		}
		
		final JButton jbOK = new JButton(nextOrFinish);
		App.showCenterPanel(panel, 0, 0, title, cancle==null?false:true, jbOK, null, listener, cancle, null, false, false, null, false, false);
	}
}
