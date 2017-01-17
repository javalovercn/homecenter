package hc.server.ui;

import hc.core.util.JSCore;
import hc.core.util.LogManager;

import java.util.Vector;

import javax.swing.JPanel;

/**
 * {@link ScriptPanel} is used to load JavaScript and execute JavaScript.
 * <BR><BR>
 * a {@link ScriptPanel} can be used in {@link HTMLMlet} and {@link Dialog} just like JPanel.
 * @since 7.36
 */
public class ScriptPanel extends JPanel {
	/**
	 * this action is recommended for HTML <code>div</code> or <code>canvas</code>.
	 * <BR><BR>
	 * Note : it is NOT recommended for HTML <code>button</code>.
	 * @see #onEvent(String, String, String[])
	 * @since 7.36
	 */
	public static final String MOUSE_DRAGGED = JSCore.MOUSE_DRAGGED;
	
	/**
	 * this action is recommended for HTML <code>div</code> or <code>canvas</code>.
	 * <BR><BR>
	 * Note : it is NOT recommended for HTML <code>button</code>.
	 * @see #CLICK
	 * @see #onEvent(String, String, String[])
	 * @since 7.36
	 */
	public static final String MOUSE_CLICKED = JSCore.MOUSE_CLICKED;
	
	/**
	 * this action is recommended for HTML <code>div</code> or <code>canvas</code>.
	 * <BR><BR>
	 * Note : it is NOT recommended for HTML <code>button</code>.
	 * @see #onEvent(String, String, String[])
	 * @since 7.36
	 */
	public static final String MOUSE_ENTERED = JSCore.MOUSE_ENTERED;
	
	/**
	 * this action is recommended for HTML <code>div</code> or <code>canvas</code>.
	 * <BR><BR>
	 * Note : it is NOT recommended for HTML <code>button</code>.
	 * @see #onEvent(String, String, String[])
	 * @since 7.36
	 */
	public static final String MOUSE_EXITED = JSCore.MOUSE_EXITED;
	
	/**
	 * this action is recommended for HTML <code>div</code> or <code>canvas</code>.
	 * <BR><BR>
	 * Note : it is NOT recommended for HTML <code>button</code>.
	 * @see #onEvent(String, String, String[])
	 * @since 7.36
	 */
	public static final String MOUSE_PRESSED = JSCore.MOUSE_PRESSED;
	
	/**
	 * this action is recommended for HTML <code>div</code> or <code>canvas</code>.
	 * <BR><BR>
	 * Note : it is NOT recommended for HTML <code>button</code>.
	 * @see #onEvent(String, String, String[])
	 * @since 7.36
	 */
	public static final String MOUSE_RELEASED = JSCore.MOUSE_RELEASED;
	
	/**
	 * this action is recommended to notify text of HTML <code>textarea</code> or <code>input</code> with type = 'text|password'.
	 * @see #onEvent(String, String, String[])
	 * @since 7.36
	 */
	public static final String NOTIFY = JSCore.NOTIFY;
	
	/**
	 * this action is recommended for changing HTML <code>select</code> or <code>input</code> with type = 'range'.
	 * @see #onEvent(String, String, String[])
	 * @since 7.36
	 */
	public static final String CHANGE = JSCore.CHANGE;
	
	/**
	 * this action is recommended for clicking HTML <code>button</code> or <code>input</code> with type = 'checkbox|radio'.
	 * <BR><BR>
	 * Note : it is NOT recommended for <code>div</code> {@link #MOUSE_CLICKED}
	 * @see #onEvent(String, String, String[])
	 * @since 7.36
	 */
	public static final String CLICK = JSCore.CLICK;
	
	SizeHeightForXML sizeHeightForXML;
	Mlet mlet;
	Vector<String> scriptToDeliver;
	Vector<String> jsToDeliver;
	String innerHTMLToDeliver;
	final Object lock;
	
	public ScriptPanel(){
		super();
		lock = this;
	}
	
	final void setSizeHeightForXML(final Mlet mlet, final SizeHeightForXML size){
		synchronized (lock) {
			if(this.mlet != null){
				throw new IllegalAccessError("ScriptPanel is added to parent twice or more!");
			}
			
			this.mlet = mlet;
			this.sizeHeightForXML = size;
			
			if(innerHTMLToDeliver != null){//完成初始id的构造，所以要在scriptToDeliver之前
				sizeHeightForXML.setInnerHTML(mlet, this, innerHTMLToDeliver);
				innerHTMLToDeliver = null;
			}
			
			if(scriptToDeliver != null){//注意：必须要在innerHTMLToDeliver之后
				final int count = scriptToDeliver.size();
				for (int i = 0; i < count; i++) {
					final String JS = scriptToDeliver.elementAt(i);
					sizeHeightForXML.loadScript(mlet, JS);
				}
				scriptToDeliver.clear();
				scriptToDeliver = null;
			}
			
			if(jsToDeliver != null){
				final int count = jsToDeliver.size();
				for (int i = 0; i < count; i++) {
					final String JS = jsToDeliver.elementAt(i);
					sizeHeightForXML.executeScript(mlet, JS);
				}
				jsToDeliver.clear();
				jsToDeliver = null;
			}
		}
	}
	
	/**
	 * load script for current {@link ScriptPanel}.
	 * <BR><BR><STRONG>Warning</STRONG> : all id of elements of HTML created by server is begin with '<STRONG>HC</STRONG>'.
	 * <BR><BR>About cache :<BR>
	 * don't worry about script too large for re-translating to mobile, <BR>
	 * the cache subsystem of HomeCenter will intelligence analysis to determine whether transmission or loading cache from mobile (if script is too small, it will not be cached).
	 * What you should do is put more into one script, because if there is too much pieces of cache in a project, the system will automatically clear the cache and restart caching.
	 * @param script
	 * @since 7.36
	 */
	public final void loadScript(final String script){
		if(script == null){
			return;
		}
		
		synchronized (lock) {
			if(sizeHeightForXML != null){
				sizeHeightForXML.loadScript(mlet, script);
			}else{
				if(scriptToDeliver == null){
					scriptToDeliver = new Vector<String>();
				}
				scriptToDeliver.add(script);
			}
		}
	}
	
	/**
	 * set inner HTML for current {@link ScriptPanel}.
	 * <BR><BR>
	 * for example : <BR>
	 * 1. a JPanel will be translated to <code>&lt;div id='HC_*'&gt;&lt;/div&gt;</code>,<BR>
	 * 2. when invoke <code>setInnerHTML("&lt;div id='myScriptDiv'&gt;some canvas html&lt;/div&gt;")</code>, <BR>
	 * 3. then the result is : <code>&lt;div id='HC_*'&gt;&lt;div id='myScriptDiv'&gt;some canvas html&lt;/div&gt;&lt;/div&gt;</code>
	 *	<BR><BR><STRONG>Warning</STRONG> : all id of elements of HTML created by server is begin with '<STRONG>HC</STRONG>'.
	 * <BR><BR>Note : there is maybe more ScriptPanels in a {@link Dialog} or {@link HTMLMlet}, make sure that IDs are unique among them.
	 * <BR><BR>About cache :<BR>
	 * don't worry about inner HTML too large for re-translating to mobile, <BR>
	 * the cache subsystem of HomeCenter will intelligence analysis to determine whether transmission or loading cache from mobile (if inner HTML is too small, it will not be cached).
	 * If there is too much pieces of cache in a project, system will automatically clear the cache and restart caching.
	 * @param html the inner HTML, for example : <code>&lt;div id='myScriptDiv'&gt;some canvas html&lt;/div&gt;</code>
	 * @see HTMLMlet#setCSS(javax.swing.JComponent, String, String)
	 * @since 7.36
	 */
	public final void setInnerHTML(final String html){
		if(html == null){
			return;
		}
		
		synchronized (lock) {
			if(sizeHeightForXML != null){
				sizeHeightForXML.setInnerHTML(mlet, this, html);
			}else{
				if(innerHTMLToDeliver != null){
					LogManager.errToLog("the older innerHTML will be thrown away for the newest innerHTML!\n" + html);
				}
				innerHTMLToDeliver = html;
			}
		}
	}
	
	/**
	 * execute script on current {@link ScriptPanel}.
	 * <BR><BR>
	 * in general, please initialize this panel by {@link #setInnerHTML(String)} and {@link #loadScript(String)} first.
	 * <BR><BR>
	 * to receive event of input in panel, please overrides {@link #onEvent(String, String, String[])}.
	 * <BR><BR><STRONG>Warning</STRONG> : all id of elements of HTML created by server is begin with '<STRONG>HC</STRONG>'.
	 * @param script
	 * @see #loadScript(String)
	 * @see #setInnerHTML(String)
	 * @see #onEvent(String, String, String[])
	 * @since 7.36
	 */
	public final void executeScript(final String script){
		if(script == null){
			return;
		}
		
		synchronized (lock) {
			if(sizeHeightForXML != null){
				sizeHeightForXML.executeScript(mlet, script);
			}else{
				if(jsToDeliver == null){
					jsToDeliver = new Vector<String>();
				}
				jsToDeliver.add(script);
			}
		}
	}
	
	/**
	 * for example, a inner HTML is following : 
	 * <BR><BR>
	 * <code>&lt;button id='mybutton' onclick="javascript:window.hcserver.click('mybutton');" /&gt;</code>
	 * <BR><BR>when user click the button, the event will be dispatched to this method, 
	 * <BR>the parameter of <code>id</code> is 'mybutton' and parameter of <code>action</code> is {@link #CLICK}.
	 * <BR><BR><STRONG>Warning</STRONG> : all id of elements of HTML created by server is begin with '<STRONG>HC</STRONG>'.
	 * <BR><BR>
	 * Note :<BR>
	 * if there is more than one ScriptPanels in a {@link Dialog} or {@link HTMLMlet}, the events of other ScriptPanel may be dispatched to this method, you should return <code>false</code> to tell server that the event is NOT for current ScriptPanel.
	 * <BR><BR><CENTER><STRONG>Table of Actions</STRONG> (NOT all)</CENTER><BR>
	 * <table border='1'>
	 * <tr>
	 * <th>Reference HTML Tag</th><th>Reference Event</th><th>JavaScript API</th><th>Action</th>
	 * </tr>
	 * 
	 * <tr>
	 * <td>button</td><td>onclick</td><td>window.hcserver.click(id);</td>
	 * <td>{@link #CLICK}</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>input type='checkbox'</td><td>onclick</td><td>window.hcserver.click(id);</td>
	 * <td>{@link #CLICK}</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>input type='radio'</td><td>onclick</td><td>window.hcserver.click(id);</td>
	 * <td>{@link #CLICK}</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>select</td><td>onchange</td>
	 * <td>window.hcserver.change(id, document.getElementById(id).selectedIndex);</td>
	 * <td>{@link #CHANGE}</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>input type='range'</td><td>onchange</td>
	 * <td>window.hcserver.change(id, intValue);</td>
	 * <td>{@link #CHANGE}</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>input type='text'|'password'</td><td>onblur</td>
	 * <td>window.hcserver.notify(id, text);</td>
	 * <td>{@link #NOTIFY}</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>textarea</td><td>onblur</td>
	 * <td>window.hcserver.notify(id, text);</td>
	 * <td>{@link #NOTIFY}</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>div | canvas</td><td>mousedown</td>
	 * <td>window.hcserver.mousePressed(id, e.offsetX, e.offsetY);</td>
	 * <td>{@link #MOUSE_PRESSED}</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>div | canvas</td><td>mouseup</td>
	 * <td>window.hcserver.mouseReleased(id, e.offsetX, e.offsetY);</td>
	 * <td>{@link #MOUSE_RELEASED}</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>div | canvas</td><td>mouseleave</td>
	 * <td>window.hcserver.mouseExited(id, e.offsetX, e.offsetY);</td>
	 * <td>{@link #MOUSE_EXITED}</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>div | canvas</td><td>mouseenter</td>
	 * <td>window.hcserver.mouseEntered(id, e.offsetX, e.offsetY);</td>
	 * <td>{@link #MOUSE_ENTERED}</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>div | canvas</td><td>click</td>
	 * <td>window.hcserver.mouseClicked(id, e.offsetX, e.offsetY);</td>
	 * <td>{@link #MOUSE_CLICKED}</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>div | canvas</td><td>touchmove</td>
	 * <td>window.hcserver.mouseDragged(id, 
						e.touches[0].clientX - leftDiv, 
						e.touches[0].clientY - topDiv);</td>
	 * <td>{@link #MOUSE_DRAGGED}</td>
	 * </tr>
	 * 
	 * </table>
	 * @param id
	 * @param action one of following (but not all) : <BR>
	 * {@link #CLICK}, {@link #CHANGE}, {@link #NOTIFY}, 
	 * {@link #MOUSE_CLICKED}, {@link #MOUSE_DRAGGED}, {@link #MOUSE_ENTERED}, 
	 * {@link #MOUSE_EXITED}, {@link #MOUSE_PRESSED}, {@link #MOUSE_RELEASED}.
	 * @param values
	 * @return true means the event is consumed; false means the event will be dispatched to other {@link ScriptPanel}.
	 * @since 7.36
	 */
	public boolean onEvent(final String id, final String action, final String[] values){//注意：此方法禁止final
		return false;
	}
}
