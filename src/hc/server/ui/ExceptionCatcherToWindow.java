package hc.server.ui;

import hc.App;
import hc.core.ContextManager;
import hc.core.L;
import hc.core.util.LogManager;
import hc.server.HCJRubyException;
import hc.util.ClassUtil;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

public class ExceptionCatcherToWindow {
	
	private Throwable t;
	private String jrubyScript;
	
	private final JFrame owner;
	private final boolean isNeedDetail;
	private final ThreadGroup token;
	
	public ExceptionCatcherToWindow(final JFrame owner, final boolean isNeedDetail){
		this.owner = owner;
		this.isNeedDetail = isNeedDetail;
		token = App.getThreadPoolToken();
	}
	
	public ExceptionCatcherToWindow(final JFrame owner){
		this(owner, false);
	}
	
	boolean isShowing;
	
	public final void setThrowable(final Throwable t){
		if(t instanceof HCJRubyException){
			final HCJRubyException hcjRubyException = (HCJRubyException)t;
			this.t = hcjRubyException.t;
			this.jrubyScript = hcjRubyException.scripts;
		}else{
			this.t = t;
		}
		
		if(ResourceUtil.isAndroidServerPlatform()){//Android Server下不显示
		}else{
			synchronized (this) {
				if(isShowing){
					L.V = L.WShop ? false : LogManager.log("isShowing Error/Stack now, skip next Throwable.");
					return;
				}else{
					isShowing = true;
					if(owner == null){
						showErrorWindow();
					}else{
						showErrorDialog(owner);
					}
				}
			}
		}
	}
	
	public final boolean isNoError(){
		return t == null;
	}
	
	public final boolean hasError(){
		return t != null;
	}
	
	/**
	 * no block current thread
	 */
	private final void showErrorWindow(){
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				App.showCenterPanelMain(buildPanel(), 0, 0, ResourceUtil.getErrorI18N(), false, null, null, null, null, null, false, true, null, true, true);
			}
		}, token);
	}

	private final JPanel buildPanel() {
		final JPanel panel;
		if(L.isInWorkshop || isNeedDetail){
			panel = buildErrorStackPanel();
		}else{
			panel = buildErrorMessagePanel();
		}
		return panel;
	}
	
	/**
	 * block ui thread
	 * @param parent
	 */
	private final void showErrorDialog(final JFrame parent){
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				App.showCenterPanelMain(buildPanel(), 0, 0, ResourceUtil.getErrorI18N(), false, App.buildDefaultOKButton(), null, null, null, parent, true, false, null, true, false);
			}
		}, token);
	}
	
	private final JPanel buildErrorMessagePanel(){
		final JPanel panel = new JPanel(new BorderLayout());
		final JLabel label = new JLabel((String)ResourceUtil.get(9238), App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING);
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}
	
	private final JPanel buildErrorStackPanel() {
		final JPanel panel = new JPanel(new BorderLayout());
		final JLabel label = new JLabel(getCauseMessage(t), App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING);
		final JTextArea area = new JTextArea(buildStackString());
		area.setEditable(false);
		panel.add(label, BorderLayout.NORTH);
		final JScrollPane scrollPane = new JScrollPane(area);
		scrollPane.setPreferredSize(new Dimension(800, 500));
		panel.add(scrollPane, BorderLayout.CENTER);
		
		return panel;
	}
	
	private final String getCauseMessage(final Throwable e){
		final Throwable cause = e.getCause();
		if(cause != null){
			return getCauseMessage(cause);
		}else{
			return e.getMessage();
		}
	}
	
	private final String buildStackString(){
		final StringBuilder sb = StringBuilderCacher.getFree();
		if(jrubyScript != null){
			sb.append("------------------------------begin error script-------------------------------\n");
			sb.append(jrubyScript);
			sb.append("\n-------------------------------end error script-------------------------------\n");
		}
		ClassUtil.buildOneStackWithCause(t, sb, false, true);
		final String out = sb.toString();
		StringBuilderCacher.cycle(sb);
		return out;
	}
}
