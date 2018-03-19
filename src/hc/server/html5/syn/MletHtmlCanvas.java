package hc.server.html5.syn;

import hc.core.IConstant;
import hc.core.L;
import hc.core.util.ByteUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCJSInterface;
import hc.core.util.JSCore;
import hc.core.util.LangUtil;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;
import hc.server.MultiUsingManager;
import hc.server.ScreenServer;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.DialogHTMLMlet;
import hc.server.ui.HTMLMlet;
import hc.server.ui.ICanvas;
import hc.server.ui.IMletCanvas;
import hc.server.ui.Mlet;
import hc.server.ui.MletSnapCanvas;
import hc.server.ui.ProjectContext;
import hc.server.ui.ScriptCSSSizeHeight;
import hc.server.ui.ScriptPanel;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.ProjResponser;
import hc.server.ui.design.SystemHTMLMlet;
import hc.server.ui.design.code.StyleManager;
import hc.server.ui.design.hpj.HCjar;
import hc.server.util.ai.AIPersistentManager;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.text.JTextComponent;

public class MletHtmlCanvas implements ICanvas, IMletCanvas, HCJSInterface {
	private static final String HC_PRE = "HC_";
	private static final int HC_PRE_LEN = HC_PRE.length();

	private static final String NO_COMPONENT_HCCODE = "no component hccode : ";

	final int width, height;

	public HTMLMlet mlet;
	public ProjectContext projectContext;
	final static boolean isAndroidServer = ResourceUtil.isAndroidServerPlatform();
	public JFrame frame;
	JScrollPane scrollPane;
	private byte[] screenIDbs;
	private char[] screenIDChars;
	private String screenID, title;
	private DifferTodo differTodo;
	private final boolean isSimu;
	final J2SESession coreSS;

	boolean isForDialog;

	public MletHtmlCanvas(final J2SESession coreSS, final int w, final int h) {
		isSimu = PropertiesManager.isSimu();

		this.coreSS = coreSS;
		this.width = w;
		this.height = h;
	}

	private final void printComp(final Component comp, final int deep) {
		final Rectangle rect = comp.getBounds();
		if (L.isInWorkshop) {
			System.out.println(comp.getClass().getName() + deep + ", x : " + rect.x + ", y : " + rect.y + ", w : " + rect.width + ", h : "
					+ rect.height);
		}
		if (comp instanceof Container) {
			final Container container = (Container) comp;
			final int count = container.getComponentCount();
			for (int i = 0; i < count; i++) {
				printComp(container.getComponent(i), deep + 1);
			}
		} else if (comp instanceof JScrollPane) {
			if (L.isInWorkshop) {
				System.out.println("------This is JScrollPane-----");
			}
			printComp(((JScrollPane) comp).getViewport().getView(), deep);
		}
	}

	@Override
	public final void onStart() {
		// differTodo.executeJS(hcj2seScript);

		// differTodo.loadJS("msg = \"hello' abc \\'world\";");

		// 必须置于上两段初始代码传送之后
		final boolean rtl = LangUtil.isRTL(UserThreadResourceUtil.getMobileLocaleFrom(coreSS));
		if (rtl) {
			LogManager.log("setRTL(true) for " + mlet.getTarget());
			differTodo.setLTR(!rtl);
		}

		if (ResourceUtil.isSystemMletOrDialog(mlet)) {
			// 系统级Mlet，和系统级Dialog不加载
		} else {
			final ProjResponser projResponser = ServerUIAPIAgent.getProjResponserMaybeNull(projectContext);
			if (projResponser != null) {
				// AddHar下可能为null
				final String dftStyles = (String) projResponser.map.get(HCjar.PROJ_STYLES);
				final String defaultStyles = (dftStyles == null ? "" : dftStyles.trim());// AddHAR可能出现null
				if (defaultStyles.length() > 0) {
					final ScriptCSSSizeHeight sizeHeight = ServerUIAPIAgent.getScriptCSSSizeHeight(mlet);
					final String replaceVariable = StyleManager.replaceVariable(coreSS, defaultStyles, sizeHeight, projectContext);
					// LogManager.log(replaceVariable);
					differTodo.loadStyles(replaceVariable, true);
				}
			}
		}
		ServerUIAPIAgent.setDiffTodo(mlet, differTodo);
		// printComp(scrolPanel, 0);

		// 必须置于notifyInitDone之前，因为有可能增加Mlet级样式和用户setStylesJComponentXX
		ScreenServer.onStartForMlet(coreSS, projectContext, mlet);
	}

	@Override
	public final void onPause() {
		ScreenServer.onPauseForMlet(coreSS, projectContext, mlet);
	}

	@Override
	public final void onResume() {
		ScreenServer.onResumeForMlet(coreSS, projectContext, mlet);
	}

	@Override
	public final void onExit() {
		onExit(false);
	}

	boolean isExitProcced;

	@Override
	public void onExit(final boolean isAutoReleaseAfterGo) {
		synchronized (this) {
			if (isExitProcced) {
				return;
			} else {
				isExitProcced = true;
			}
		}

		if (L.isInWorkshop) {
			LogManager.log("onExit MletHtmlCanvas : " + mlet.getTarget());
		}

		ScreenServer.onExitForMlet(coreSS, projectContext, mlet, isAutoReleaseAfterGo);
		MultiUsingManager.exit(coreSS, ServerUIAPIAgent.buildScreenID(projectContext.getProjectID(), mlet.getTarget()));
		frame.dispose();

		if (AIPersistentManager.isEnableHCAI()) {
			AIPersistentManager.processJComponentToolTip(coreSS, projectContext, mlet);
		}
	}

	@Override
	public final void setMlet(final J2SESession coreSS, final Mlet mlet, final ProjectContext projectCtx) {
		if (mlet instanceof DialogHTMLMlet) {
			isForDialog = true;
			((DialogHTMLMlet) mlet).resLock.mletCanvas = this;
		}

		this.mlet = (HTMLMlet) mlet;
		ServerUIAPIAgent.setProjectContext(mlet, projectCtx);
		projectContext = projectCtx;

		if (isAndroidServer == false) {
			frame = new JFrame();// 不能入Session会导致block
									// showWindowWithoutWarningBanner，但是Android环境下必须，否则尺寸自适应
		}

		ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, ServerUIAPIAgent.getProjResponserMaybeNull(projectContext),
				new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						scrollPane = new JScrollPane(mlet, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
								JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
						return null;
					}
				});
		differTodo = new DifferTodo(coreSS, screenID, this.mlet);
	}

	/**
	 * in Android server, AddHarHTMLMlet is NOT need to adapter size.
	 * 
	 * @param scrollPane
	 * @return
	 */
	public static boolean isForAddHtml(final JScrollPane scrollPane) {
		if (scrollPane != null) {
			final JViewport jviewport = scrollPane.getViewport();
			if (jviewport != null) {
				final Component view = jviewport.getView();
				if (view != null && view instanceof SystemHTMLMlet) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public final void init() {// in user thread
		// 注意：mlet外加JScrollPane时，在Window XP, JRE
		// 7会出现height不能大于1024的情形，Mac无此问题，故关闭JScrollPane
		// 因为如下会发生，所以加4
		// javax.swing.JScrollPane0, x : 0, y : 0, w : 100, h : 100
		// javax.swing.JViewport1, x : 2, y : 2, w : 96, h : 96

		if (isAndroidServer) {
			frame = new JFrame();// 不能入Session会导致block
									// showWindowWithoutWarningBanner，但是Android环境下必须，否则尺寸自适应
		}

		frame.setContentPane(scrollPane);

		L.V = L.WShop ? false : LogManager.log("HTMLMlet setPreferredSize(" + width + ", " + height + ").");

		if (isAndroidServer) {
			scrollPane.setPreferredSize(new Dimension(width, height));
		} else {
			mlet.setPreferredSize(new Dimension(width, height));
		}

		if (ServerUIAPIAgent.isEnableApplyOrientationWhenRTL(mlet)
				&& ProjectContext.isRTL(UserThreadResourceUtil.getMobileLocaleFrom(coreSS))) {
			frame.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
			LogManager.log("applyComponentOrientation(RIGHT_TO_LEFT) for " + mlet.getTarget());
		}
		frame.pack();// 可能重载某些方法
	}

	@Override
	public final void setScreenIDAndTitle(final String screenID, final String title) {
		this.screenID = screenID;
		this.screenIDbs = ByteUtil.getBytes(screenID, IConstant.UTF_8);
		this.screenIDChars = screenID.toCharArray();
		this.title = title;
	}

	@Override
	public boolean isSameScreenID(final byte[] bs, final int offset, final int len) {
		return ByteUtil.isSame(screenIDbs, 0, screenIDbs.length, bs, offset, len);
	}

	@Override
	public boolean isSameScreenIDIgnoreCase(final char[] chars, final int offset, final int len) {
		if (len == screenIDChars.length) {
			for (int i = 0, j = offset; i < len;) {
				final char c1 = screenIDChars[i++];
				final char c2 = chars[j++];
				if (c1 == c2 || Character.toUpperCase(c1) == Character.toUpperCase(c2)
						|| Character.toLowerCase(c1) == Character.toLowerCase(c2)) {
				} else {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	private final boolean matchCmd(final byte[] cmdbs, final byte[] bs, final int offset) {
		final int len = cmdbs.length;
		for (int i = 0; i < len; i++) {
			if (cmdbs[i] != bs[offset + i]) {
				return false;
			}
		}
		return true;
	}

	private final int searchNextSplitIndex(final byte[] bs, final int startIdx) {
		final int endIdx = bs.length;
		for (int i = startIdx; i < endIdx; i++) {
			if (bs[i] == JSCore.splitBS[0]) {
				final int endSplitBS = JSCore.splitBS.length;
				if (i + JSCore.splitBS.length < endIdx) {
					boolean isDiff = false;
					for (int j = 1; j < endSplitBS; j++) {
						if (bs[i + j] != JSCore.splitBS[j]) {
							isDiff = true;
							break;
						}
					}
					if (isDiff == false) {
						return i;
					}
				}
			}
		}
		return -1;
	}

	@Override
	public final void actionJSInput(final byte[] bs, final int offset, final int len) {
		if (isForDialog) {
			final DialogHTMLMlet dialog = (DialogHTMLMlet) mlet;

			if (dialog.isContinueProcess() == false) {
				return;
			}
		}

		final int length = bs.length;
		final byte[] cloneBS = ByteUtil.byteArrayCacher.getFree(length);
		System.arraycopy(bs, 0, cloneBS, 0, length);

		coreSS.uiEventInput.setHTMLMletEvent(isForDialog);

		// 注意：考虑可能有长时间事件处理，此处不wait
		ServerUIAPIAgent.runInSessionThreadPool(coreSS, ServerUIAPIAgent.getProjResponserMaybeNull(projectContext), new Runnable() {// 事件的先后性保证
			@Override
			public void run() {
				actionJSInputInUser(cloneBS, offset, len);
				ByteUtil.byteArrayCacher.cycle(cloneBS);
			}
		});
	}

	private final void dispatchEventToScriptPanel(final String id, final String action, final String[] args) {
		final Vector<ScriptPanel> scriptPanels = differTodo.scriptPanels;
		if (scriptPanels != null) {
			final int size = scriptPanels.size();
			for (int i = 0; i < size; i++) {
				if (scriptPanels.get(i).onEvent(id, action, args)) {
					return;
				}
			}
		}
	}

	private static final String[] zeroStringArray = {};

	private final void actionJSInputInUser(final byte[] bs, final int offset, final int len) {
		if (isSimu) {
			try {
				LogManager.log("action JS input : " + new String(bs, offset, len, IConstant.UTF_8));
			} catch (final Throwable e1) {
				e1.printStackTrace();
			}
		}

		// -------------------------------------------------------
		// 重要
		// 如果以下增加事件接收，请同步CodeItem.AnonymousClass
		// -------------------------------------------------------
		try {
			if (matchCmd(JSCore.actionExt, bs, offset)) {
				final String cmd = getOneValue(JSCore.actionExt, bs, offset, len);
				actionExt(cmd);
				return;
			} else if (matchCmd(JSCore.clickButton, bs, offset)) {
				final String id = getOneValue(JSCore.clickButton, bs, offset, len);
				clickButton(id);
				// if(id.startsWith(HC_PRE, 0) && clickButton(id)){
				// }else{
				// dispatchEventToScriptPanel(id, JSCore.CLICK_BUTTON,
				// zeroStringArray);
				// }
				return;
			} else if (matchCmd(JSCore.clickRadioButton, bs, offset)) {
				final String id = getOneValue(JSCore.clickRadioButton, bs, offset, len);
				clickRadioButton(id);
				// if(id.startsWith(HC_PRE, 0) && clickRadioButton(id)){
				// }else{
				// dispatchEventToScriptPanel(id, JSCore.CLICK_RADIO_BUTTON,
				// zeroStringArray);
				// }
				return;
			} else if (matchCmd(JSCore.clickCheckbox, bs, offset)) {
				final String id = getOneValue(JSCore.clickCheckbox, bs, offset, len);
				clickCheckbox(id);
				// if(id.startsWith(HC_PRE, 0) && clickCheckbox(id)){
				// }else{
				// dispatchEventToScriptPanel(id, JSCore.CLICK_CHECKBOX,
				// zeroStringArray);
				// }
				return;
			} else if (matchCmd(JSCore.click, bs, offset)) {// 要置于click*之后
				final String id = getOneValue(JSCore.click, bs, offset, len);
				dispatchEventToScriptPanel(id, JSCore.CLICK, zeroStringArray);
				return;
			} else if (matchCmd(JSCore.selectComboBox, bs, offset)) {
				final String[] values = getTwoValue(JSCore.selectComboBox, bs, offset, len);
				final String id = values[0];
				final String arg0 = values[1];
				selectComboBox(id, arg0);
				// if(id.startsWith(HC_PRE, 0) && selectComboBox(id, arg0)){
				// }else{
				// final String[] args = {arg0};
				// dispatchEventToScriptPanel(id, JSCore.SELECT_COMBO_BOX,
				// args);
				// }
				return;
			} else if (matchCmd(JSCore.selectSlider, bs, offset)) {
				final String[] values = getTwoValue(JSCore.selectSlider, bs, offset, len);
				final String id = values[0];
				final String arg0 = values[1];
				selectSlider(id, arg0);
				// if(id.startsWith(HC_PRE, 0) && selectSlider(id, arg0)){
				// }else{
				// final String[] args = {arg0};
				// dispatchEventToScriptPanel(id, JSCore.SELECT_SLIDER, args);
				// }
				return;
			} else if (matchCmd(JSCore.change, bs, offset)) {
				final String[] values = getTwoValue(JSCore.change, bs, offset, len);
				final String id = values[0];
				final String arg0 = values[1];
				final String[] args = { arg0 };
				dispatchEventToScriptPanel(id, JSCore.CHANGE, args);
				return;
			} else if (matchCmd(JSCore.notifyTextFieldValue, bs, offset)) {
				final String[] values = getTwoValue(JSCore.notifyTextFieldValue, bs, offset, len);
				final String id = values[0];
				final String arg0 = values[1];
				notifyTextFieldValue(id, arg0);
				// if(id.startsWith(HC_PRE, 0) && notifyTextFieldValue(id,
				// arg0)){
				// }else{
				// final String[] args = {arg0};
				// dispatchEventToScriptPanel(id,
				// JSCore.NOTIFY_TEXT_FIELD_VALUE, args);
				// }
				return;
			} else if (matchCmd(JSCore.notifyTextAreaValue, bs, offset)) {
				final String[] values = getTwoValue(JSCore.notifyTextAreaValue, bs, offset, len);
				final String id = values[0];
				final String arg0 = values[1];
				notifyTextAreaValue(id, arg0);
				// if(id.startsWith(HC_PRE, 0) && notifyTextAreaValue(id,
				// arg0)){
				// }else{
				// final String[] args = {arg0};
				// dispatchEventToScriptPanel(id, JSCore.NOTIFY_TEXT_AREA_VALUE,
				// args);
				// }
				return;
			} else if (matchCmd(JSCore.notify, bs, offset)) {// 要置于notify*之后
				final String[] values = getTwoValue(JSCore.notify, bs, offset, len);
				final String id = values[0];
				final String arg0 = values[1];
				final String[] args = { arg0 };
				dispatchEventToScriptPanel(id, JSCore.NOTIFY, args);
				return;
			} else if (matchCmd(JSCore.mouseReleased, bs, offset)) {
				final String[] values = getThreeValue(JSCore.mouseReleased, bs, offset, len);
				final String id = values[0];
				final String arg0 = values[1];
				final String arg1 = values[2];
				if (id.startsWith(HC_PRE, 0) && notifyMouseReleased(id, arg0, arg1)) {
				} else {
					final String[] args = { arg0, arg1 };
					dispatchEventToScriptPanel(id, JSCore.MOUSE_RELEASED, args);
				}
				return;
			} else if (matchCmd(JSCore.mousePressed, bs, offset)) {
				final String[] values = getThreeValue(JSCore.mousePressed, bs, offset, len);
				final String id = values[0];
				final String arg0 = values[1];
				final String arg1 = values[2];
				if (id.startsWith(HC_PRE, 0) && notifyMousePressed(id, arg0, arg1)) {
				} else {
					final String[] args = { arg0, arg1 };
					dispatchEventToScriptPanel(id, JSCore.MOUSE_PRESSED, args);
				}
				return;
			} else if (matchCmd(JSCore.mouseExited, bs, offset)) {
				final String[] values = getThreeValue(JSCore.mouseExited, bs, offset, len);
				final String id = values[0];
				final String arg0 = values[1];
				final String arg1 = values[2];
				if (id.startsWith(HC_PRE, 0) && notifyMouseExited(id, arg0, arg1)) {
				} else {
					final String[] args = { arg0, arg1 };
					dispatchEventToScriptPanel(id, JSCore.MOUSE_EXITED, args);
				}
				return;
			} else if (matchCmd(JSCore.mouseEntered, bs, offset)) {
				final String[] values = getThreeValue(JSCore.mouseEntered, bs, offset, len);
				final String id = values[0];
				final String arg0 = values[1];
				final String arg1 = values[2];
				if (id.startsWith(HC_PRE, 0) && notifyMouseEntered(id, arg0, arg1)) {
				} else {
					final String[] args = { arg0, arg1 };
					dispatchEventToScriptPanel(id, JSCore.MOUSE_ENTERED, args);
				}
				return;
			} else if (matchCmd(JSCore.mouseClicked, bs, offset)) {
				final String[] values = getThreeValue(JSCore.mouseClicked, bs, offset, len);
				final String id = values[0];
				final String arg0 = values[1];
				final String arg1 = values[2];
				if (id.startsWith(HC_PRE, 0) && notifyMouseClicked(id, arg0, arg1)) {
				} else {
					final String[] args = { arg0, arg1 };
					dispatchEventToScriptPanel(id, JSCore.MOUSE_CLICKED, args);
				}
				return;
			} else if (matchCmd(JSCore.mouseDragged, bs, offset)) {
				final String[] values = getThreeValue(JSCore.mouseDragged, bs, offset, len);
				final String id = values[0];
				final String arg0 = values[1];
				final String arg1 = values[2];
				if (id.startsWith(HC_PRE, 0) && notifyMouseDragged(id, arg0, arg1)) {
				} else {
					final String[] args = { arg0, arg1 };
					dispatchEventToScriptPanel(id, JSCore.MOUSE_DRAGGED, args);
				}
				return;
			}
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}

		try {
			final String cmds = new String(bs, offset, len, IConstant.UTF_8);
			final String[] splits = StringUtil.splitToArray(cmds, StringUtil.SPLIT_LEVEL_2_JING);
			projectContext.error("unknow JS input event : " + splits[0] + ", cmd : " + cmds);
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	private final boolean notifyMouseReleased(final String id, final String x, final String y) {
		try {
			final Component comp = differTodo.searchComponentByHcCode(Integer.parseInt(id.substring(HC_PRE_LEN)));
			if (comp != null) {
				try {
					final int intX = Integer.parseInt(x);
					final int intY = Integer.parseInt(y);
					final MouseListener[] mlistener = comp.getMouseListeners();
					MouseEvent event = null;
					for (int i = 0; i < mlistener.length; i++) {
						if (event == null) {
							event = buildMouseEvent(comp, MouseEvent.MOUSE_RELEASED, intX, intY);
						}
						mlistener[i].mouseReleased(event);
					}
				} catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}
				return true;
			} else {
				projectContext.error(NO_COMPONENT_HCCODE + id);
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	private final MouseEvent buildMouseEvent(final Component comp, final int eventID, final int x, final int y) {
		return new MouseEvent(comp, eventID, System.currentTimeMillis(), MouseEvent.BUTTON1_MASK, x, y, 1, false) {
			@Override
			public final Point getLocationOnScreen() {
				return super.getLocationOnScreen();// TODO
			}

			@Override
			public final int getXOnScreen() {
				return super.getXOnScreen();// TODO
			}

			@Override
			public final int getYOnScreen() {
				return super.getYOnScreen();// TODO
			}
		};
	}

	private final boolean notifyMousePressed(final String id, final String x, final String y) {
		try {
			final Component comp = differTodo.searchComponentByHcCode(Integer.parseInt(id.substring(HC_PRE_LEN)));
			if (comp != null) {
				try {
					final int intX = Integer.parseInt(x);
					final int intY = Integer.parseInt(y);
					final MouseListener[] mlistener = comp.getMouseListeners();
					MouseEvent event = null;
					for (int i = 0; i < mlistener.length; i++) {
						if (event == null) {
							event = buildMouseEvent(comp, MouseEvent.MOUSE_PRESSED, intX, intY);
						}
						mlistener[i].mousePressed(event);
					}
				} catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}
				return true;
			} else {
				projectContext.error(NO_COMPONENT_HCCODE + id);
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	private final boolean notifyMouseExited(final String id, final String x, final String y) {
		try {
			final Component comp = differTodo.searchComponentByHcCode(Integer.parseInt(id.substring(HC_PRE_LEN)));
			if (comp != null) {
				try {
					final int intX = Integer.parseInt(x);
					final int intY = Integer.parseInt(y);
					final MouseListener[] mlistener = comp.getMouseListeners();
					MouseEvent event = null;
					for (int i = 0; i < mlistener.length; i++) {
						if (event == null) {
							event = buildMouseEvent(comp, MouseEvent.MOUSE_EXITED, intX, intY);
						}
						mlistener[i].mouseExited(event);
					}
				} catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}
				return true;
			} else {
				projectContext.error(NO_COMPONENT_HCCODE + id);
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	private final boolean notifyMouseEntered(final String id, final String x, final String y) {
		try {
			final Component comp = differTodo.searchComponentByHcCode(Integer.parseInt(id.substring(HC_PRE_LEN)));
			if (comp != null) {
				try {
					final int intX = Integer.parseInt(x);
					final int intY = Integer.parseInt(y);
					final MouseListener[] mlistener = comp.getMouseListeners();
					MouseEvent event = null;
					for (int i = 0; i < mlistener.length; i++) {
						if (event == null) {
							event = buildMouseEvent(comp, MouseEvent.MOUSE_ENTERED, intX, intY);
						}
						mlistener[i].mouseEntered(event);
					}
				} catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}
				return true;
			} else {
				projectContext.error(NO_COMPONENT_HCCODE + id);
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	private final boolean notifyMouseClicked(final String id, final String x, final String y) {
		try {
			final Component comp = differTodo.searchComponentByHcCode(Integer.parseInt(id.substring(HC_PRE_LEN)));
			if (comp != null) {
				try {
					final int intX = Integer.parseInt(x);
					final int intY = Integer.parseInt(y);
					final MouseListener[] mlistener = comp.getMouseListeners();
					MouseEvent event = null;
					for (int i = 0; i < mlistener.length; i++) {
						if (event == null) {
							event = buildMouseEvent(comp, MouseEvent.MOUSE_CLICKED, intX, intY);
						}
						mlistener[i].mouseClicked(event);
					}
				} catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}
				return true;
			} else {
				projectContext.error(NO_COMPONENT_HCCODE + id);
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	private final boolean notifyMouseDragged(final String id, final String x, final String y) {
		try {
			final Component comp = differTodo.searchComponentByHcCode(Integer.parseInt(id.substring(HC_PRE_LEN)));
			if (comp != null) {
				try {
					final int intX = Integer.parseInt(x);
					final int intY = Integer.parseInt(y);
					final MouseMotionListener[] mlistener = comp.getMouseMotionListeners();
					MouseEvent event = null;
					for (int i = 0; i < mlistener.length; i++) {
						if (event == null) {
							event = buildMouseEvent(comp, MouseEvent.MOUSE_DRAGGED, intX, intY);
						}
						mlistener[i].mouseDragged(event);
					}
				} catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}
				return true;
			} else {
				projectContext.error(NO_COMPONENT_HCCODE + id);
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	private final String[] getThreeValue(final byte[] actionBS, final byte[] bs, final int offset, final int len) {
		// try {
		// final String str = new String(bs, offset, len, IConstant.UTF_8);
		// } catch (final UnsupportedEncodingException e) {
		// ExceptionReporter.printStackTrace(e);
		// }

		final int firstValueIdx = offset + actionBS.length + JSCore.splitBS.length;
		final int secondSplitIdx = searchNextSplitIndex(bs, firstValueIdx);
		final String value1 = new String(bs, firstValueIdx, secondSplitIdx - firstValueIdx);

		final int secondValueIdx = secondSplitIdx + JSCore.splitBS.length;
		final int threeSplitIdx = searchNextSplitIndex(bs, secondValueIdx);
		final String value2 = new String(bs, secondValueIdx, threeSplitIdx - secondValueIdx);

		final int threeValueIdx = threeSplitIdx + JSCore.splitBS.length;
		final String value3 = new String(bs, threeValueIdx, (len + offset) - threeValueIdx);

		final String[] out = { JSCore.decode(value1), JSCore.decode(value2), JSCore.decode(value3) };
		return out;
	}

	private final String[] getTwoValue(final byte[] actionBS, final byte[] bs, final int offset, final int len) {
		final int firstValueIdx = offset + actionBS.length + JSCore.splitBS.length;

		final int secondSplitIdx = searchNextSplitIndex(bs, firstValueIdx);
		final String value1 = new String(bs, firstValueIdx, secondSplitIdx - firstValueIdx);

		final int secondValueIdx = secondSplitIdx + JSCore.splitBS.length;
		final String value2 = new String(bs, secondValueIdx, (len + offset) - secondValueIdx);

		final String[] out = { JSCore.decode(value1), JSCore.decode(value2) };
		return out;
	}

	private final String getOneValue(final byte[] actionBS, final byte[] bs, final int offset, final int len) {
		final int firstValueIdx = offset + actionBS.length + JSCore.splitBS.length;

		final String id = new String(bs, firstValueIdx, len + offset - firstValueIdx);
		return JSCore.decode(id);
	}

	@Override
	public final void actionExt(final String cmd) {
		// TODO
		System.out.println("actionExt : " + cmd);
	}

	@Override
	public final boolean clickButton(final String id) {
		try {
			final Component btn = differTodo.searchComponentByHcCode(Integer.parseInt(id.substring(HC_PRE_LEN)));// in
																													// user
																													// thread
			if (btn != null && btn instanceof AbstractButton) {
				final MouseEvent e = new MouseEvent(btn, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), MouseEvent.BUTTON1_MASK, 0,
						0, 1, false);
				MletSnapCanvas.processClickOnComponent(btn, e);
				return true;
			} else {
				projectContext.error(NO_COMPONENT_HCCODE + id);
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public final boolean selectSlider(final String id, final String value) {
		try {
			final Component slider = differTodo.searchComponentByHcCode(Integer.parseInt(id.substring(HC_PRE_LEN)));
			if (slider != null && slider instanceof JSlider) {
				final JSlider sliderBar = (JSlider) slider;

				final int intValue = Integer.parseInt(value);
				if (sliderBar.getValue() == intValue) {
					return true;
				}

				sliderBar.setValue(intValue);
				return true;
			} else {
				projectContext.error(NO_COMPONENT_HCCODE + id);
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public final boolean selectComboBox(final String id, final String selectedIndex) {
		try {
			final Component combo = differTodo.searchComponentByHcCode(Integer.parseInt(id.substring(HC_PRE_LEN)));
			if (combo != null && combo instanceof JComboBox) {
				final JComboBox combo2 = (JComboBox) combo;

				final int intSelectedIndex = Integer.parseInt(selectedIndex);
				if (combo2.getSelectedIndex() == intSelectedIndex) {
					return true;
				}

				// final Object oldSelected = combo2.getSelectedItem();
				//
				// //触发ItemEvent[DESELECTED]
				// if(oldSelected != null){
				// final ItemEvent e = new ItemEvent(combo2,
				// ItemEvent.ITEM_STATE_CHANGED, oldSelected,
				// ItemEvent.DESELECTED);
				// MCanvas.dispatchEvent(combo, e);
				// }
				combo2.setSelectedIndex(intSelectedIndex);
				// 注意：执行上步时，会在J2SE环境下自动触发下面两事件和上一行事件。
				// java.awt.event.ItemEvent[ITEM_STATE_CHANGED,item=three,stateChange=SELECTED]
				// java.awt.event.ActionEvent[ACTION_PERFORMED,cmd=comboBoxChanged,

				// //触发ItemEvent[SELECTED]
				// {
				// final Object newSelected = combo2.getItemAt(selectedIndex);
				// final ItemEvent e = new ItemEvent(combo2,
				// ItemEvent.ITEM_STATE_CHANGED, newSelected,
				// ItemEvent.SELECTED);
				// MCanvas.dispatchEvent(combo, e);
				// }
				//
				// //触发ActionEvent
				// MCanvas.doActon(combo);

				// java.awt.event.ItemEvent[ITEM_STATE_CHANGED,item=one,stateChange=DESELECTED]
				// java.awt.event.ItemEvent[ITEM_STATE_CHANGED,item=three,stateChange=SELECTED]
				// java.awt.event.ActionEvent[ACTION_PERFORMED,cmd=comboBoxChanged,
				return true;
			} else {
				projectContext.error(NO_COMPONENT_HCCODE + id);
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public final boolean notifyTextFieldValue(final String id, final String value) {
		try {
			final Component combo = differTodo.searchComponentByHcCode(Integer.parseInt(id.substring(HC_PRE_LEN)));
			if (combo != null && combo instanceof JTextField) {
				final JTextField textField = (JTextField) combo;
				textField.setText(value);
				// MCanvas.doActon(combo);
				return true;
			} else {
				projectContext.error(NO_COMPONENT_HCCODE + id);
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public final String toString() {
		if (mlet != null) {
			return this.getClass().getSimpleName() + ":" + mlet.getTarget();
		} else {
			return super.toString();
		}
	}

	@Override
	public final boolean notifyTextAreaValue(final String id, final String value) {
		try {
			final Component combo = differTodo.searchComponentByHcCode(Integer.parseInt(id.substring(HC_PRE_LEN)));
			if (combo != null && combo instanceof JComponent && JPanelDiff.isTextMultLinesEditor((JComponent) combo)) {
				final JTextComponent textArea = (JTextComponent) combo;
				textArea.setText(value);
				// MCanvas.doActon(combo);
				return true;
			} else {
				projectContext.error(NO_COMPONENT_HCCODE + id);
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public final boolean clickRadioButton(final String id) {
		try {
			final Component combo = differTodo.searchComponentByHcCode(Integer.parseInt(id.substring(HC_PRE_LEN)));
			if (combo != null && combo instanceof JRadioButton) {
				final JRadioButton radioButton = (JRadioButton) combo;
				radioButton.doClick();
				// MCanvas.doActon(combo);
				return true;
			} else {
				projectContext.error(NO_COMPONENT_HCCODE + id);
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public final boolean clickCheckbox(final String id) {
		try {
			final Component combo = differTodo.searchComponentByHcCode(Integer.parseInt(id.substring(HC_PRE_LEN)));
			if (combo != null && combo instanceof JCheckBox) {
				final JCheckBox checkBox = (JCheckBox) combo;
				checkBox.doClick();
				// MCanvas.doActon(combo);
				return true;
			} else {
				projectContext.error(NO_COMPONENT_HCCODE + id);
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public final Mlet getMlet() {
		return mlet;
	}
}
