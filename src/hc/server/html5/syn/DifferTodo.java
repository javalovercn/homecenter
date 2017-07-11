package hc.server.html5.syn;

import hc.core.FastSender;
import hc.core.IConstant;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.cache.CacheManager;
import hc.core.util.ByteUtil;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.core.util.ThreadPriorityManager;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.HCByteArrayOutputStream;
import hc.server.ui.HTMLMlet;
import hc.server.ui.Mlet;
import hc.server.ui.ProjectContext;
import hc.server.ui.ScriptPanel;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.ProjResponser;
import hc.server.util.CacheComparator;
import hc.server.util.ai.AIPersistentManager;
import hc.util.HttpUtil;
import hc.util.JSUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BoundedRangeModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

public class DifferTodo {
	private static final byte[] WINDOW_HCJ2SE_SET_LTR_FALSE = ByteUtil.getBytes("window.hcj2se.setLTR(false);", IConstant.UTF_8);
	private static final byte[] WINDOW_HCJ2SE_SET_LTR_TRUE = ByteUtil.getBytes("window.hcj2se.setLTR(true);", IConstant.UTF_8);
	
	private final HashMap<Integer, Object> map = new HashMap<Integer, Object>(48);
	private HcCodeMap hcCode;
	public static final int HC_CODE_MLET = 0;
	
	public Vector<ScriptPanel> scriptPanels;
	
	public final boolean searchHcCode(final Object obj){
		final int obj_hashcode = (obj == mlet)?HC_CODE_MLET:obj.hashCode();
		
		synchronized (map) {
			if(hcCode != null){
				final int hccode = hcCode.searchObject(obj);//必须优先，因为可能重码
				if(hccode != 0){
					return true;
				}
			}
			
			final Object oldValue = map.get(obj_hashcode);
			if(oldValue == null){
				return false;
			}else if(oldValue == obj){
				return true;
			}else{
				return false;
			}
		}
	}
	
	public final int buildHcCode(final Object obj){
		final int obj_hashcode = (obj == mlet)?HC_CODE_MLET:obj.hashCode();
		
		synchronized (map) {
			if(hcCode != null){
				final int hccode = hcCode.searchObject(obj);//必须优先，因为可能重码
				if(hccode != 0){
					return hccode;
				}
			}
			
			final Object oldValue = map.get(obj_hashcode);
			if(oldValue == null){
				map.put(obj_hashcode, obj);
				return obj_hashcode;
			}else if(oldValue == obj){
				return obj_hashcode;
			}else{
				if(hcCode == null){
					hcCode = new HcCodeMap(map);
				}
				return hcCode.buildHcCode(obj, obj_hashcode);
			}
		}
	}
	
	private final FastSender fastSender;
	
	final private byte[] elementIDBS;
	final int isGizIdx = 0;
	final int isNeedCacheIdx = isGizIdx + 1;
	final int elementIDLenIdx = isNeedCacheIdx + 1;
	final int elementIDIdx = elementIDLenIdx + 2;
	final int elementIDLen;
	final HTMLMlet mlet;
	private final int mobileWarningWidth, mobileWarningHeight;

	byte[] todoBs;
	final int scriptIndex;
	
	final String projID, urlID, softUID;
	final byte[] projIDbs, softUidBS, urlIDbs;
	public final J2SESession coreSS;
	public final ProjectContext projectContext;
	public final ProjResponser resp;
	private final boolean isSocketLimitOn;
	private final Vector<String> allowedDomains;
	
	public DifferTodo(final J2SESession coreSS, final String elementID, final HTMLMlet mlet) {
		this.coreSS = coreSS;
		this.mlet = mlet;
		projectContext = ServerUIAPIAgent.getProjectContextFromMlet(mlet);
		resp = ServerUIAPIAgent.getProjResponserMaybeNull(projectContext);
		
		isSocketLimitOn = resp.isCSCSocketLimitOn();
		if(isSocketLimitOn){
			final Vector<String> tmp = resp.getCSCAllowedDomains();
			if(tmp == null){
				allowedDomains = new Vector<String>(0);
			}else{
				allowedDomains = tmp;
			}
		}else{
			allowedDomains = null;
		}
		
		this.elementIDBS = ByteUtil.getBytes(elementID, IConstant.UTF_8);
		elementIDLen = this.elementIDBS.length;

		this.mobileWarningWidth = UserThreadResourceUtil.getMletWidthFrom(coreSS) + 2;//标准J2SE会比实际宽度多一个像素。
		this.mobileWarningHeight = UserThreadResourceUtil.getMletHeightFrom(coreSS) + 2;
		
		this.projID = projectContext.getProjectID();
		this.projIDbs = ByteUtil.getBytes(projID, IConstant.UTF_8);
		this.softUID = UserThreadResourceUtil.getMobileSoftUID(coreSS);
		this.softUidBS = ByteUtil.getBytes(softUID, IConstant.UTF_8);
		//AddHar可能为null
		this.urlID = elementID;
		this.urlIDbs = elementIDBS;
		
		jsCacheComparer = new CacheComparator(projID, softUID, urlID, projIDbs, softUidBS, urlIDbs) {
			@Override
			public void sendData(final Object[] paras) {
				final byte[] scriptBS = (byte[])paras[0];
				final boolean needGzip = (Boolean)paras[1];
				
				sendJSBytes(scriptBS, 0, scriptBS.length, needGzip, true);//注意：需要通知进行cache
			}
		};
		
		map.put(HC_CODE_MLET, mlet);
		
		this.fastSender = coreSS.context.getFastSender();
		System.arraycopy(js_header1, 0, pngBS, 0, js_header1_len);

		initTotoBS(CacheManager.getMinCacheSize() * 2);
		scriptIndex = elementIDIdx + elementIDLen + 2 + projIDbs.length;//screenIDLen + screenID
	}
	
	private final void sendStringJSOrCache(final String script, final boolean needGzip, final boolean enableCache){
		if(isSimu){
			if(script.length() < 1024){//不显示基础脚本
				LogManager.log(script);
			}
		}
		
		if(L.isInWorkshop){
			LogManager.log("sendStringJSOrCache : " + script);
		}
		
		final byte[] noCycleBS = ByteUtil.getBytes(script, IConstant.UTF_8);
		
		sendBytesJSOrCache(noCycleBS, needGzip, enableCache);
	}

	private final void sendBytesJSOrCache(final byte[] noCycleBS, final boolean needGzip, final boolean enableCache) {
		if(enableCache && CacheManager.isMeetCacheLength(noCycleBS.length)){
			final Object[] paras = {noCycleBS, needGzip};
			jsCacheComparer.encodeGetCompare(coreSS, true, noCycleBS, 0, noCycleBS.length, paras);
		}else{
			sendJSBytes(noCycleBS, 0, noCycleBS.length, needGzip, false);//注意：不需要通知进行cache
		}
	}

	final CacheComparator jsCacheComparer;
	
	final byte isNeedGzip = 1;
	final byte isNotNeedGzip = 0;
	final byte isNeedCache = 1;
	final byte isNotNeedCache = 0;

	/**
	 * 使用数据缓存进行数据传输前处理。
	 * 注意：由于本方法被多处引用，所以加锁
	 * @param noCycleBS
	 * @param index
	 * @param len
	 * @param needGzip
	 * @param needCache
	 */
	private synchronized final void sendJSBytes(final byte[] noCycleBS, final int index, final int len, final boolean needGzip, final boolean needCache){
//		if(needGzip){
//			try{
//				bs = GzipUtil.compress(bs, index, len);
//				index = 0;
//				len = bs.length;
//			}catch (Exception e) {
//				ExceptionReporter.printStackTrace(e);
//				return;
//			}
//		}
		
		if(mlet.getStatus() == Mlet.STATUS_EXIT){
			return;
		}
		
		final int maxLen = elementIDIdx + elementIDLen + 2 + projIDbs.length + len;
		if(todoBs.length < maxLen){
			initTotoBS(maxLen);
		}
		
		todoBs[isGizIdx] = (needGzip?isNeedGzip:isNotNeedGzip);
		todoBs[isNeedCacheIdx] = (needCache?isNeedCache:isNotNeedCache);
		
		System.arraycopy(noCycleBS, index, todoBs, scriptIndex, len);
		
		fastSender.sendWrapAction(MsgBuilder.E_BIG_MSG_JS_TO_MOBILE, todoBs, 0, len + scriptIndex);
		
//		TCPSplitTester.sendBigTest(fastSender, len);
	}

	
	private void initTotoBS(final int maxLen) {
		todoBs = new byte[maxLen];
		ByteUtil.integerToTwoBytes(elementIDLen, todoBs, elementIDLenIdx);
		System.arraycopy(elementIDBS, 0, todoBs, elementIDIdx, elementIDLen);
		final int projLenIdx = elementIDLenIdx + 2 + elementIDLen;
		ByteUtil.integerToTwoBytes(projIDbs.length, todoBs, projLenIdx);
		System.arraycopy(projIDbs, 0, todoBs, projLenIdx + 2, projIDbs.length);
	}
	
	final static byte[] stopLoading = ByteUtil.getBytes(HCURL.HTML_LOAD_DONE, IConstant.UTF_8);
	
	public final void setLTR(final boolean isLTR){
		if(isLTR){
			sendJSBytes(WINDOW_HCJ2SE_SET_LTR_TRUE, 0, WINDOW_HCJ2SE_SET_LTR_TRUE.length, false, false);
		}else{
			sendJSBytes(WINDOW_HCJ2SE_SET_LTR_FALSE, 0, WINDOW_HCJ2SE_SET_LTR_FALSE.length, false, false);
		}
	}
	
	private final static byte[] BS_LOAD_STYLE = ByteUtil.getBytes("window.hcj2se.loadStyles(\"", IConstant.UTF_8);
	private final static byte[] BS_LOAD_STYLE_END = ByteUtil.getBytes("\");", IConstant.UTF_8);
	
	private final static byte[] BS_LOAD_JS = ByteUtil.getBytes("window.hcj2se.loadJS(\"", IConstant.UTF_8);
	private final static byte[] BS_LOAD_JS_END = ByteUtil.getBytes("\");", IConstant.UTF_8);

	/**
	 * 加载CSS到html->header->style段
	 * for example : body{background-color:#f00;}
	 * @param css
	 */
	public final void loadStyles(String css, final boolean enableCache){
		if(isSocketLimitOn){
			css = HttpUtil.replaceUnallowedDomain(css, allowedDomains);
		}
		
		css = yinHaoPattern.matcher(css).replaceAll(yinHaoEncode);
//		css = css.replace("\"", "\\\"");//改"为\"
		css = JSUtil.replaceNewLine(JSUtil.replaceReturnWithEmtpySpace(css));//修复换行不能执行的问题
		final boolean needGzip = ((css.length()>GZIP_MIN_SIZE)?true:false);
		final byte[] cssBS = ByteUtil.getBytes(css, IConstant.UTF_8);
		
		final int cssBSLen = cssBS.length;
		
		final int maxLen = BS_LOAD_STYLE.length + cssBSLen + BS_LOAD_STYLE_END.length;
		final byte[] noCycleBS = new byte[maxLen];
		
		//转调为window.hcj2se.loadStyles
		System.arraycopy(BS_LOAD_STYLE, 0, noCycleBS, 0, BS_LOAD_STYLE.length);
		System.arraycopy(cssBS, 0, noCycleBS, BS_LOAD_STYLE.length, cssBSLen);
		System.arraycopy(BS_LOAD_STYLE_END, 0, noCycleBS, BS_LOAD_STYLE.length + cssBSLen, BS_LOAD_STYLE_END.length);
		
		sendBytesJSOrCache(noCycleBS, needGzip, enableCache);
	}
	
	static final Pattern yinHaoPattern = Pattern.compile("\"", Pattern.LITERAL);
	static final String yinHaoEncode = Matcher.quoteReplacement("\\\"");
	
	static final String blockScriptPanel = "block Permission [ScriptPanle]";

	final String getBlockScriptMsg() {
		return blockScriptPanel + " in HAR Project  [" + resp.projectID + "].";
	}
	
	/**
	 * 加载JS到html->header->script段。
	 * 注意：它不同于{@link #executeJSWithoutCacheForScriptPanel(String)}
	 * for example : body{background-color:#f00;}
	 * @param js
	 */
	public final void loadScriptForScriptPanel(String js, final boolean isCacheEnabled){
		if(isNoScriptPanelPermission()){
			LogManager.err(getBlockScriptMsg());
			return;
		}
		
		js = yinHaoPattern.matcher(js).replaceAll(yinHaoEncode);
//		js = js.replace("\"", "\\\"");//改"为\"
		js = JSUtil.replaceNewLine(JSUtil.replaceReturnWithEmtpySpace(js));//修复换行不能执行的问题
		final boolean needGzip = ((js.length()>GZIP_MIN_SIZE)?true:false);
		final byte[] jsBS = ByteUtil.getBytes(js, IConstant.UTF_8);
		
		final int maxLen = BS_LOAD_JS.length + jsBS.length + BS_LOAD_JS_END.length;
		final byte[] noCycleBS = new byte[maxLen];
		
		//转调为window.hcj2se.loadJS
		System.arraycopy(BS_LOAD_JS, 0, noCycleBS, 0, BS_LOAD_JS.length);
		System.arraycopy(jsBS, 0, noCycleBS, BS_LOAD_JS.length, jsBS.length);
		System.arraycopy(BS_LOAD_JS_END, 0, noCycleBS, BS_LOAD_JS.length + jsBS.length, BS_LOAD_JS_END.length);
		
		sendBytesJSOrCache(noCycleBS, needGzip, isCacheEnabled);
	}

	private static final int GZIP_MIN_SIZE = 1024 * 100;
	
	/**
	 * 立即执行script。
	 * 它不同于{@link #loadScript(String)}
	 * @param script
	 */
	public final void executeJSWithoutCacheForScriptPanel(final String script){
		if(isNoScriptPanelPermission()){
			LogManager.err(getBlockScriptMsg());
			return;
		}
		
		final boolean needGzip = ((script.length()>GZIP_MIN_SIZE)?true:false);
		sendStringJSOrCache(script, needGzip, false);
	}
	
	public final Component searchComponentByHcCode(final int hcCode){//in user thread
		return searchComponentByHcCode(mlet, hcCode);
	}
	
	public final Component searchComponentByHcCode(final JPanel panel, final int hcCode){//in user thread
		if(buildHcCode(panel) == hcCode){
			return panel;
		}
		
		final int compCount = panel.getComponentCount();
		for (int i = 0; i < compCount; i++) {
			final Component comp = panel.getComponent(i);
			if(comp instanceof JPanel){
				final Component result = searchComponentByHcCode((JPanel)comp, hcCode);
				if(result != null){
					return result;
				}
			}else{
				if(buildHcCode(comp) == hcCode){
					return comp;
				}
			}
		}
		return null;
	}
	
	public final void notifyInitDone(){
		if(L.isInWorkshop){
			LogManager.log("[HTMLMlet:" + mlet.getTarget() + "] notifyInitDone");
		}
		sendBytesJSOrCache(stopLoading, false, false);
	}
	
	public final void notifyRemoveFromPanel(final int hashID){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.removeFromPanel(" ).append( hashID ).append( ");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}
	
	public final void notifyAddComboBox(final int containerHashID, final int index, final int hashID, final JComponent jcomp){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.addComboBox(" ).append( containerHashID ).append( ", " ).append( 
				index ).append( ", " ).append( hashID ).append( ", \"" ).append( buildComboBoxSerial(jcomp) ).append( "\");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}

	private final String buildComboBoxSerial(final JComponent jcomp) {
		final JComboBox combo = (JComboBox)jcomp;
		final StringBuilder sb = StringBuilderCacher.getFree();
		final int size = combo.getItemCount();

		for (int i = 0; i < size; i++) {
			if(sb.length() > 0){
				sb.append(StringUtil.SPLIT_LEVEL_2_JING);
			}
			sb.append(combo.getItemAt(i).toString());
		}
		
		final String out = sb.toString();
		StringBuilderCacher.cycle(sb);
		return out;
	}

	public final void notifyAddButton(final int containerHashID, final int index, final int hashID){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.addButton(" ).append( containerHashID ).append( ", " ).append( index ).append( ", " ).append( hashID ).append( ");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}

	public final void notifyAddCheckbox(final int containerHashID, final int index, final int hashID){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.addCheckbox(" ).append( containerHashID ).append( ", " ).append( index ).append( ", " ).append( hashID ).append( ");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}

	public final void notifyChangeRadioButtonGroup(final int hashID, final int groupHashID){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.changeRadioButtonGroup(" ).append( hashID ).append( ", " ).append( groupHashID ).append( ");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}

	public final void notifyAddRadioButton(final int containerHashID, final int index, final int hashID, final int groupHashID){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.addRadioButton(" ).append( containerHashID ).append( ", " ).append( index ).append( ", " ).append( hashID ).append( ", " ).append( groupHashID ).append( ");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}

	public final void notifyAddJProgressBar(final int containerHashID, final int index, final int hashID, final int max, final int value){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.addProgressBar(" ).append( containerHashID ).append( ", " ).append( index ).append( ", " ).append( hashID ).append( ", " ).append( max ).append( ", " ).append( value ).append( ");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}
	
	public final void notifyAddSlider(final int containerHashID, final int index, final int hashID, final int min, final int max, final int value, final int step){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.addSlider(" ).append( containerHashID ).append( ", " ).append( index ).append( ", " ).append( hashID ).append( ", " ).append( min ).append( ", " ).append( max ).append( ", " ).append( value ).append( ", " ).append( step ).append( ");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}
	
	public final void changeSliderModel(final int hashID, final JSlider slider){
		final StringBuilder sb = StringBuilderCacher.getFree();
		
		final BoundedRangeModel model = slider.getModel();
		final int min = model.getMinimum();
		final int max = model.getMaximum();
		final int value = model.getValue();
		final int step = getSliderStep(slider);
		
		sb.append("window.hcj2se.modifySliderModel(").append( hashID ).append( ", " ).append( min ).append( ", " ).append( max ).append( ", " ).append( value ).append( ", " ).append( step ).append( ");");
		sendStringJSOrCache(sb.toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}

	public static int getSliderStep(final JSlider slider) {
		int step = 0;//0表示使用缺省step，不设置
		if(slider.getSnapToTicks() && slider.getMinorTickSpacing() > 0) {
			step = slider.getMinorTickSpacing();
		}
		return step;
	}
	
	public final void notifyAddLabel(final int containerHashID, final int index, final int hashID){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.addLabel(" ).append( containerHashID ).append( ", " ).append( index ).append( ", " ).append( hashID ).append( ");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}

	public final void notifyAddTextField(final int containerHashID, final int index, final int hashID, final int isPassword, final String tip){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.addTextField(" ).append( containerHashID ).append( ", " ).append( index ).append( ", " ).append( hashID ).append( ", " ).append( isPassword ).append( ", \"" ).append( tip ).append( "\");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}

	public final void setDivInnerHTMLForScriptPanel(final int hashID, String innerHTML, final boolean enableCache){
		if(isNoScriptPanelPermission()){
			innerHTML = blockScriptPanel;
		}
		
		innerHTML = yinHaoPattern.matcher(innerHTML).replaceAll(yinHaoEncode);
		innerHTML = JSUtil.replaceNewLine(JSUtil.replaceReturnWithEmtpySpace(innerHTML));//低版本android环境下必须
		
		final boolean needGzip = ((innerHTML.length()>GZIP_MIN_SIZE)?true:false);
		
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.setDivInnerHTML(" ).append( hashID ).append( ", \"" ).append( innerHTML ).append( "\");").toString(), needGzip, enableCache);
		StringBuilderCacher.cycle(sb);
	}

	private final boolean isNoScriptPanelPermission() {
		return ResourceUtil.isSystemMletOrDialog(mlet) == false && resp.hasPermissionScriptPanel == false;
	}
	
	public final void notifyAddTextArea(final int containerHashID, final int index, final int hashID, final String tip){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.addTextArea(" ).append( containerHashID ).append( ", " ).append( index ).append( ", " ).append( hashID ).append( ", \"" ).append( tip ).append( "\");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}

	public final void notifyAddPanel(final int containerHashID, final int index, final int hashID){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.addPanel(" ).append( containerHashID ).append( ", " ).append( index ).append( ", " ).append( hashID ).append( ");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}
	
	public final void setRTLForDiv(final int hashID, final boolean isRTL){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.setLTRForDiv(" ).append( hashID ).append( ", \"" ).append( isRTL?"rtl":"ltr" ).append( "\");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}
	
	/**
	 * 
	 * @param hashID
	 * @param className null for non class for current item.
	 * @param styles null for non style for current item.
	 */
	public final void setStyleForDiv(final int hashID, final String className, String styles){
		if(styles != null){
			if(isSocketLimitOn){
				styles = HttpUtil.replaceUnallowedDomain(styles, allowedDomains);
			}
			styles = StringUtil.formatJS(styles);
		}
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.setDivStyle(" ).append( hashID ).append( ", \"" ).append( className ).append( "\", \"" ).append( styles ).append( "\");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}
	
	public final void setStyleForJCheckBoxText(final int hashID, final String className, String styles){
		if(styles != null){
			if(isSocketLimitOn){
				styles = HttpUtil.replaceUnallowedDomain(styles, allowedDomains);
			}
			
			styles = StringUtil.formatJS(styles);
		}
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.setLabelStyle(" ).append( hashID ).append( ", \"" ).append( className ).append( "\", \"" ).append( styles ).append( "\");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}
	
	public final void setStyleForInputTag(final int eleID, final String className, String styles){
		if(styles != null){
			if(isSocketLimitOn){
				styles = HttpUtil.replaceUnallowedDomain(styles, allowedDomains);
			}
			
			styles = StringUtil.formatJS(styles);
		}
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.setElementStyle(" ).append( eleID ).append( ", \"" ).append( className ).append( "\", \"" ).append( styles ).append( "\");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}

	/**
	 * <input type="checkbox" disabled="disabled">
	 * <input type="radio" name="radiobutton" value="radiobutton" checked>//name="radiobutton"则为一组，以实现单选
	 * <button type="button" disabled="disabled">Click Me!</button>
	 * @param hashID
	 * @param enable
	 */
	public final void notifyModifyJComponentEnable(final int hashID, final boolean enable){
		//document.getElementById(id).disabled = true; 
//		document.getElementById(id).disabled = false;
		final StringBuilder sb = StringBuilderCacher.getFree();
		if(enable){
			sb.append("window.hcj2se.setComponentEnable(" ).append( hashID ).append( ",true);");
		}else{
			sb.append("window.hcj2se.setComponentEnable(" ).append( hashID ).append( ",false);");
		}
		sendStringJSOrCache(sb.toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}
	
	public final void notifyModifyAbstractButtonText(final Object src){
		final AbstractButton absButton = (AbstractButton)src;
		final String value = absButton.getText();
		final int hcCode = buildHcCode(src);
		
		if(src instanceof JToggleButton){//jcheckbox, jradiobutton
			notifyModifyCheckboxText(absButton, hcCode, value);
		}else{
			notifyModifyButtonText(absButton, hcCode, value);
		}
	}
	
	private final void notifyModifyButtonText(final AbstractButton absButton, final int hashID, final String text){
//		var text = document.getElementById(button_id).firstChild;
//		text.data = text.data == "Lock" ? "Unlock" : "Lock";
//		<button> www.w3school.com.cn/tags/tag_button.asp
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.setButtonText(" ).append( hashID ).append( ",\"" ).append( text ).append( "\");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
		
		if(AIPersistentManager.isEnableForSomeComponent && AIPersistentManager.isEnableHCAI()){
			AIPersistentManager.processDiffNotify(false, coreSS, absButton, this.projectContext, AIPersistentManager.setButtonText, text);
		}
	}
	
	/**
	 * 同时适用于JRadioButton
	 * @param hashID
	 * @param text
	 */
	private final void notifyModifyCheckboxText(final AbstractButton absButton, final int hashID, final String text){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.setCheckboxText(" ).append( hashID ).append( ",\"" ).append( text ).append( "\");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
		
		if(AIPersistentManager.isEnableForSomeComponent && AIPersistentManager.isEnableHCAI()){
			AIPersistentManager.processDiffNotify(false, coreSS, absButton, this.projectContext, AIPersistentManager.setCheckboxText, text);
		}
	}
	
	public final void notifyModifySliderValue(final int hashID, final JSlider slider){
		final BoundedRangeModel brm = slider.getModel();
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.changeSliderValue(" ).append( hashID ).append( "," ).append( brm.getMinimum() ).append( "," ).append( brm.getMaximum() ).append( "," ).append( brm.getValue() ).append( ");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
		
		if(AIPersistentManager.isEnableForSomeComponent && AIPersistentManager.isEnableHCAI()){
			AIPersistentManager.processDiffNotify(false, coreSS, slider, this.projectContext, AIPersistentManager.changeSliderValue, String.valueOf(brm.getValue()));
		}
	}
	
	public final void notifyModifyProgressBarValue(final int hashID, final JProgressBar progressBar){//in user thread
		final int value = progressBar.getValue() * 100 / progressBar.getMaximum();
		try{
			Thread.sleep(ThreadPriorityManager.UI_WAIT_MS);//等待诸与JProgressBar.setString或其它逻辑被执行。它们的逻辑有可能在setValue之后。
		}catch (final Exception e) {
		}
		final String text = progressBar.isStringPainted()?progressBar.getString():null;

		final StringBuilder sb = StringBuilderCacher.getFree();
		sb.append("window.hcj2se.setProgressBarValue(").append(hashID).append(",").append(value).append(",\"").append(text).append("\");");
		sendStringJSOrCache(sb.toString(), false, false);		
		StringBuilderCacher.cycle(sb);
		
		if(AIPersistentManager.isEnableForSomeComponent && AIPersistentManager.isEnableHCAI()){
			AIPersistentManager.processDiffNotify(false, coreSS, progressBar, this.projectContext, AIPersistentManager.setProgressBarValue, text + "/" + String.valueOf(value));
		}
	}

	public final void changeComboBoxModel(final int hashID, final JComboBox jcomp){
		final StringBuilder sb = StringBuilderCacher.getFree();
		final int selected = jcomp.getSelectedIndex();
		sb.append("window.hcj2se.changeComboBoxModel(").append(hashID).append(",\"").append(buildComboBoxSerial(jcomp)).append("\",").append(selected).append(");");
		sendStringJSOrCache(sb.toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}
	
	public final void changeComboBoxSelected(final JComboBox jcombobox, final int hashID, final int selected){
		final StringBuilder sb = StringBuilderCacher.getFree();
		sb.append("window.hcj2se.changeComboBoxSelected(").append(hashID).append(",").append(selected).append(");");
		sendStringJSOrCache(sb.toString(), false, false);
		StringBuilderCacher.cycle(sb);
		
		if(AIPersistentManager.isEnableForSomeComponent && AIPersistentManager.isEnableHCAI()){
			AIPersistentManager.processDiffNotify(false, coreSS, jcombobox, this.projectContext, AIPersistentManager.changeComboBoxSelected, String.valueOf(selected));
		}
	}
	
	public final void notifyModifyLabelText(final JLabel label, final int hashID, final String text){
		final StringBuilder sb = StringBuilderCacher.getFree();
		sb.append("window.hcj2se.setLabelText(").append(hashID).append(",\"").append(text).append("\");");
		sendStringJSOrCache(sb.toString(), false, false);
		StringBuilderCacher.cycle(sb);
		
		if(AIPersistentManager.isEnableHCAI()){
			AIPersistentManager.processDiffNotify(true, coreSS, label, this.projectContext, AIPersistentManager.setLabelText, text);
		}
	}
	
	protected final HCByteArrayOutputStream byteArrayOutputStream = new HCByteArrayOutputStream();
	private byte[] pngBS = new byte[100 * 1024];
	private char[] pngChars = new char[pngBS.length];//须与pngBS保持长度大体一致
	
	final static private byte[] js_header1 = "window.hcj2se.addImage(".getBytes();
	final static private int js_header1_len = js_header1.length;
	final static private byte[] js_header2 = ", '".getBytes();
	final static private int js_header2_len = js_header2.length;
	final static private byte[] js_header3 = "');".getBytes();
	final static private int js_header3_len = js_header3.length;
	
	private final BufferedImage getRenderedImage(final Image in){
        final int w = in.getWidth(null);
        final int h = in.getHeight(null);
        final int type = BufferedImage.TYPE_INT_ARGB;
        final BufferedImage out = new BufferedImage(w, h, type);
        final Graphics2D g2 = out.createGraphics();
        g2.drawImage(in, 0, 0, null);
        g2.dispose();
        return out;
    }
	
	/**
	 * image for css : background: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAFWHRTb2Z0d2FyZQBBZG etc...);
	 * @param ico
	 * @param elementID
	 */
	private final synchronized void sendIconJS(final Icon ico, final int hashID){
		if(ico == null){
			return;
		}
		
		if(ico instanceof ImageIcon){
		}else{
			throw new Error("unsupport icon type : " + ico.getClass().getName());
		}
		
		RenderedImage image = null;
		{
			final Image img = ((ImageIcon)ico).getImage();
			if(img instanceof BufferedImage){
				image = (BufferedImage)img;
			}else{
				image = getRenderedImage(img);
			}
		}
		
		try {
			final int maxLen = ((image.getWidth() * image.getHeight()) << 1) + 1000;
			if(pngBS.length < maxLen){
				pngBS = new byte[maxLen];
				pngChars = new char[maxLen];
				System.arraycopy(js_header1, 0, pngBS, 0, js_header1_len);
			}else{
			}

			final byte[] elementIDBS = String.valueOf(hashID).getBytes(IConstant.UTF_8);
			System.arraycopy(elementIDBS, 0, pngBS, js_header1_len, elementIDBS.length);
			
			int nextIndex = js_header1_len + elementIDBS.length;
			System.arraycopy(js_header2, 0, pngBS, nextIndex, js_header2_len);
			nextIndex += js_header2_len;
			
			byteArrayOutputStream.reset(pngBS, nextIndex);
			ImageIO.write(image, "png", byteArrayOutputStream);
			final int pngDataLen = byteArrayOutputStream.size();
			
			final int pngBase64Len = ByteUtil.encodeBase64(pngBS, nextIndex, pngDataLen, pngChars, 0);
			nextIndex += ByteUtil.charToByte(pngChars, 0, pngBase64Len, pngBS, nextIndex);

			System.arraycopy(js_header3, 0, pngBS, nextIndex, js_header3_len);
			
			sendJSBytes(pngBS, 0, nextIndex + js_header3_len, false, false);
			
		} catch (final IOException e1) {
		}
	}
	
	private final boolean isSimu = PropertiesManager.isSimu();

	public final void notifyJComponentLocation(final JComponent component){
		final Rectangle rect = component.getBounds();
		
		final int compWidth = rect.width;
		final int compHeight = rect.height;
		
		if(isSimu){
			LogManager.log("send Component Location, " + component.toString() + " [x : " + rect.x + ", y : " + rect.y + ", w : " + compWidth + ", h : " + compHeight + "]");
		}
		
		if(compWidth > mobileWarningWidth || compHeight > mobileWarningHeight){
			LogManager.warning("size is more than width/height of client, setPreferredSize may be required for component [" + component.toString() + "].");
		}
		
		final StringBuilder sb = StringBuilderCacher.getFree();
		sb.append("window.hcj2se.setLocation(" ).append( buildHcCode(component) ).append( "," ).append( rect.x ).append( "," ).append( rect.y ).append( "," ).append( compWidth ).append( "," ).append( compHeight ).append( ");");
		sendStringJSOrCache(sb.toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}
	
	public final void notifyModifyJComponentVisible(final int hashID, final boolean visible){
		final StringBuilder sb = StringBuilderCacher.getFree();
		if(visible){
			sb.append("window.hcj2se.setJComponentVisible(" ).append( hashID ).append( ",true);");
		}else{
			sb.append("window.hcj2se.setJComponentVisible(" ).append( hashID ).append( ",false);");
		}
		sendStringJSOrCache(sb.toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}
	
	/**
	 * 
	 * @param hashID
	 * @param icon 有可能为null
	 */
	public final void notifyModifyIcon(final int hashID, final Icon icon){
		sendIconJS(icon, hashID);
	}
	
	public final void notifyModifyDisabledIcon(final int hashID, final Icon icon){
		sendIconJS(icon, hashID);
	}
	
	public final void notifyModifyButtonSelected(final int hashID, final boolean selected){
		final StringBuilder sb = StringBuilderCacher.getFree();
		if(selected){
			sb.append("window.hcj2se.setButtonSelected(" ).append( hashID ).append( ",true);");
		}else{
			sb.append("window.hcj2se.setButtonSelected(" ).append( hashID ).append( ",false);");
		}
		sendStringJSOrCache(sb.toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}
	
	/**
	 * <input type="text" name="country" value="China" readonly="readonly" />
	 * @param hashID
	 * @param editable
	 */
	public final void notifyModifyTextComponentEditable(final int hashID, final boolean editable){
		final StringBuilder sb = StringBuilderCacher.getFree();
		if(editable){
			sb.append("window.hcj2se.setTextComponentEditable(" ).append( hashID ).append( ",true);");
		}else{
			sb.append("window.hcj2se.setTextComponentEditable(" ).append( hashID ).append( ",false);");
		}
		sendStringJSOrCache(sb.toString(), false, false);
		StringBuilderCacher.cycle(sb);
	}
	
	public final void notifyModifyTextComponentText(final JTextComponent textComp, final int hashID, final String text){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.setTextComponentText(" ).append( hashID ).append( ",\"" ).append( text ).append( "\");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
		
		if(AIPersistentManager.isEnableForSomeComponent && AIPersistentManager.isEnableHCAI()){
			AIPersistentManager.processDiffNotify(false, coreSS, textComp, this.projectContext, AIPersistentManager.setTextComponentText, text);
		}
	}
	
	public final void notifyModifyTextAreaText(final JTextComponent textComp, final int hashID, final String text){
		final StringBuilder sb;
		sendStringJSOrCache((sb = StringBuilderCacher.getFree()).append("window.hcj2se.setTextAreaText(" ).append( hashID ).append( ",\"" ).append( text ).append( "\");").toString(), false, false);
		StringBuilderCacher.cycle(sb);
		
		if(AIPersistentManager.isEnableForSomeComponent && AIPersistentManager.isEnableHCAI()){
			AIPersistentManager.processDiffNotify(false, coreSS, textComp, this.projectContext, AIPersistentManager.setTextAreaText, text);
		}
	}
	
	public final void addEventListener(final int index, final JComponent addedComponent){
		
		addedComponent.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent evt) {
				final String propertyName = evt.getPropertyName();
				final int phcCode = DifferTodo.this.buildHcCode(addedComponent);
				
				if(propertyName.equals("enabled")){
					final Boolean isEnabledOrEditable = (Boolean)evt.getNewValue();
					notifyModifyJComponentEnable(phcCode, isEnabledOrEditable);
					
					if(addedComponent instanceof AbstractButton){
						final AbstractButton abstractButton = (AbstractButton)addedComponent;
						refreshIcon(phcCode, abstractButton, abstractButton.isSelected());
					}else if(addedComponent instanceof JLabel){
						final JLabel tmpLabel = (JLabel)addedComponent;
						final Icon changedIcon = isEnabledOrEditable?tmpLabel.getIcon():tmpLabel.getDisabledIcon();
						notifyModifyIcon(phcCode, changedIcon);
					}
					return;
				}else if(propertyName.equals("editable")){//适合JTextComponent
					final Boolean isEnabledOrEditable = (Boolean)evt.getNewValue();
					notifyModifyTextComponentEditable(phcCode, isEnabledOrEditable);
					return;
				}else if(propertyName.equals("model")){//适合JComboBox
					if(addedComponent instanceof JComboBox){
						changeComboBoxModel(phcCode, (JComboBox)addedComponent);
						return;
					}else if(addedComponent instanceof JSlider){
						changeSliderModel(phcCode, (JSlider)addedComponent);
						return;
					}
				}else if(propertyName.equals("maximum") || propertyName.equals("minimum")){
					if(addedComponent instanceof JSlider){
						notifyModifySliderValue(phcCode, (JSlider)addedComponent);
						return;
					}
				}
				
				if(addedComponent instanceof AbstractButton || addedComponent instanceof JLabel){
					final int hashCode = phcCode;
					final Object newValue = evt.getNewValue();
					
					if(propertyName.equals(AbstractButton.TEXT_CHANGED_PROPERTY)){
						if(addedComponent instanceof AbstractButton){
							notifyModifyAbstractButtonText(addedComponent);
						}else if(addedComponent instanceof JLabel){
							final JLabel label = (JLabel)addedComponent;
							notifyModifyLabelText(label, phcCode, label.getText());
						}
					}else if(propertyName.equals(AbstractButton.ICON_CHANGED_PROPERTY)){
						notifyModifyIcon(hashCode, (Icon)newValue);
					}else if(propertyName.equals(AbstractButton.DISABLED_ICON_CHANGED_PROPERTY)){
						notifyModifyDisabledIcon(hashCode, (Icon)newValue);
					}
				}
				return;
			}
//			}else if(addedComponent instanceof JTextComponent){//不需要TEXT_CHANGED_PROPERTY
//			notifyModifyTextComponentText(hashCode, newValue);
		});
		
		if(addedComponent instanceof AbstractButton){
			((AbstractButton) addedComponent).addChangeListener(new ChangeListener() {
				boolean isSelected = ((AbstractButton)addedComponent).isSelected();
				@Override
				public void stateChanged(final ChangeEvent e) {
					final AbstractButton abstractButton = (AbstractButton)addedComponent;
					if(isSelected != abstractButton.isSelected()){
						isSelected = !isSelected;
						final int hashID = DifferTodo.this.buildHcCode(addedComponent);
						notifyModifyButtonSelected(hashID, isSelected);
						refreshIcon(hashID, abstractButton, isSelected);
					}
				}
			});
		}
		
		if(addedComponent instanceof JProgressBar){//注意：不同于上段的AbstractButton
			((JProgressBar) addedComponent).addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent e) {
					final int hashID = DifferTodo.this.buildHcCode(addedComponent);
					notifyModifyProgressBarValue(hashID, (JProgressBar)addedComponent);
				}
			});
		}
		
		if(addedComponent instanceof JSlider){//注意：不同于上段的AbstractButton
			((JSlider)addedComponent).addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent e) {
					final int hashID = DifferTodo.this.buildHcCode(addedComponent);
					notifyModifySliderValue(hashID, (JSlider)addedComponent);
				}
			});
		}
		
		if(addedComponent instanceof JComboBox){
			final JComboBox jcombobox = (JComboBox)addedComponent;
			jcombobox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(final ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						final int hashID = DifferTodo.this.buildHcCode(addedComponent);
						changeComboBoxSelected(jcombobox, hashID, jcombobox.getSelectedIndex());
					}
				}
			});
		}
		
		if(addedComponent instanceof JTextComponent){
			final JTextComponent textComp = (JTextComponent)addedComponent;
			textComp.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void removeUpdate(final DocumentEvent e) {
					refresh(addedComponent, textComp);
				}

				public void refresh(final JComponent addedComponent,
						final JTextComponent textComp) {
					final int hashID = DifferTodo.this.buildHcCode(addedComponent);
					JTextComponentDiff.sendModifyText(hashID, DifferTodo.this, textComp, textComp.getText());
				}
				
				@Override
				public void insertUpdate(final DocumentEvent e) {
					refresh(addedComponent, textComp);
				}
				
				@Override
				public void changedUpdate(final DocumentEvent e) {
					refresh(addedComponent, textComp);
				}
			});
		}
		
		addedComponent.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorRemoved(final AncestorEvent event) {
			}
			
			@Override
			public void ancestorMoved(final AncestorEvent event) {
				//FlowLayout addButton1 before addButton2, 
				notifyJComponentLocation(addedComponent);
			}
			
			@Override
			public void ancestorAdded(final AncestorEvent event) {
			}
		});
		
		addedComponent.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(final ComponentEvent e) {
				setComponentVisible(true);
			}
			@Override
			public void componentResized(final ComponentEvent e) {
				notifyJComponentLocation(addedComponent);
			}
			@Override
			public void componentMoved(final ComponentEvent e) {
				notifyJComponentLocation(addedComponent);
			}
			@Override
			public void componentHidden(final ComponentEvent e) {
				setComponentVisible(false);
			}
			private void setComponentVisible(final boolean isVisible){
				notifyModifyJComponentVisible(DifferTodo.this.buildHcCode(addedComponent), isVisible);
			}
		});
		
//		只接收手机端事件回来，不向手机端去推送。
//		if(addedComponent instanceof JTextComponent){
//			JTextComponent textComp = (JTextComponent)addedComponent;
//			textComp.getDocument().addDocumentListener(new DocumentListener() {
//				@Override
//				public void removeUpdate(DocumentEvent e) {
//				}
//				
//				@Override
//				public void insertUpdate(DocumentEvent e) {
//				}
//				
//				@Override
//				public void changedUpdate(DocumentEvent e) {
//				}
//			});
//		}
		
	}//end notifyModifyAddComponent

	private void refreshIcon(final int hashID,
			final AbstractButton abstractButton, final boolean isSelected) {
		if(abstractButton.isEnabled()){
			notifyModifyIcon(hashID, isSelected?abstractButton.getSelectedIcon():abstractButton.getIcon());
		}else{
			notifyModifyIcon(hashID, isSelected?abstractButton.getDisabledSelectedIcon():abstractButton.getDisabledIcon());
		}
	}
}
