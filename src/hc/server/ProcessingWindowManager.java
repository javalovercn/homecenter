package hc.server;

import hc.App;
import hc.core.ContextManager;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.server.util.HCJDialog;
import hc.server.util.HCJFrame;
import hc.util.UILang;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

public class ProcessingWindowManager {
	private static Window processing;
	private static boolean isPause = false;
	private static Window pauseWindow;
	
	public static void pause(){
		synchronized (ProcessingWindowManager.class) {
			if(isPause){
				return;
			}
			isPause = true;
		}
		
		pauseWindow = processing;
		if(pauseWindow != null){
			pauseWindow.setVisible(false);
		}
		processing = null;
		
	}
	
	public static void resume(){
		synchronized (ProcessingWindowManager.class) {
			if(isPause == false){
				return;
			}
			isPause = false;
		}
		
		final Window currProcessing = processing;
		if(currProcessing != null){
			currProcessing.dispose();
		}
		
		processing = pauseWindow;
		if(pauseWindow != null){
			pauseWindow.setVisible(true);
			pauseWindow.toFront();
		}
		
	}
	
	/**
	 * 不能runLater，否则会导致后面的对象盖掉前面实例。
	 */
	public static void disposeProcessingWindow(){
		CCoreUtil.checkAccess();
		
		final Window processingWind = processing;
		if(processingWind != null){
			processing = null;
			processingWind.dispose();
		}
	}

	/**
	 * 
	 * @param parent
	 * @param isModal
	 * @param msg
	 * @param back 回转对象，长度为1。如果传入为null，表示不需要返回Window实例
	 */
	public static void showCenterMessageOnTop(final Frame parent, final boolean isModal, final String msg, final Window[] back){
		final JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		panel.add(new JLabel(msg, App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING),
				BorderLayout.CENTER);
		
		showCenterMessageOnTop(parent, isModal, panel, back);
	}
	
	public static void showCenterMessageOnTop(final Frame parent, final boolean isModal, final JPanel panel, final Window[] back){
		final Window waiting;
		final Container contentPane;
		
		CCoreUtil.checkAccess();
		
		final boolean isNewFrame = parent == null || isModal == false;
		if(isNewFrame){
			waiting = new HCJFrame(){
				@Override
				public void dispose() {
					super.dispose();
					processing = null;
				}
			};
			((JFrame)waiting).setUndecorated(true);
			contentPane = ((JFrame)waiting).getContentPane();
		}else{
			waiting = new HCJDialog(parent, isModal){
				@Override
				public void dispose() {
					super.dispose();
					processing = null;
				}
			};
			((JDialog)waiting).setUndecorated(true);
			contentPane = ((JDialog)waiting).getContentPane();
		}

		contentPane.add(panel);
		waiting.pack();
		
		if(back != null){
			back[0] = waiting;
		}
		
		disposeProcessingWindow();
		processing = waiting;
		
		if(isNewFrame){
			showCenter(waiting);
		}else{
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run(){
					showCenter(waiting);
				}
			}, App.getThreadPoolToken());
			try{
				//等待上面充分绘制
				Thread.sleep(100);
			}catch (final Exception e) {
			}
		}
	}

	public static void showCenter(final Component frame) {
		try {
			App.invokeAndWaitUI(new Runnable() {//不能直接swingutils.invoke, can not call invokeAndWait from EDT
				@Override
				public void run() {
					frame.applyComponentOrientation(ComponentOrientation
							.getOrientation(UILang.getUsedLocale()));
					final int width = frame.getWidth(), height = frame.getHeight();
					final int w = (Toolkit.getDefaultToolkit().getScreenSize().width - width) / 2;
					final int h = (Toolkit.getDefaultToolkit().getScreenSize().height - height) / 2;
					frame.setLocation(w, h);
					frame.setVisible(true);
					if(frame instanceof Window){
						((Window)frame).toFront();
					}
				}
			});
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
	public static Window showCenterMessage(final String msg){
		final Window[] back = {null};
		showCenterMessageOnTop(null, false, msg, back);
		return back[0];
	}
}
