package hc.server.ui;

import hc.core.L;
import hc.core.util.LogManager;
import hc.server.html5.syn.DifferTodo;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.ProjResponser;

import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class ScriptCSSSizeHeight extends SizeHeight {
	public DifferTodo diffTodo;
	Vector<TodoItem> styleItemToDeliver;
	Vector<CacheString> stylesToDeliver;
	Vector<CacheString> scriptToDeliver;
	Vector<String> jsNoCacheToDeliver;
	boolean isFlushCSS = false;
	private final ProjectContext ctx;
	final ProjResponser pr;
	
	public final void loadCSSImpl(final Mlet mlet, final ProjectContext ctx, final String styles, final boolean isCacheEnabled){
		if(SimuMobile.checkSimuProjectContext(ctx)){
			return;
		}
		
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				if(stylesToDeliver == null){
					stylesToDeliver = new Vector<CacheString>();
				}
				stylesToDeliver.add(new CacheString(styles, isCacheEnabled));
				return;
			}
			loadStylesImpl(mlet, styles, isCacheEnabled);
		}
	}

	final void loadStylesImpl(final Mlet mlet, final String styles, final boolean isCacheEnabled) {
		synchronized(mlet.synLock){
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				diffTodo.loadStyles(styles, isCacheEnabled);
			}
		}
		L.V = L.WShop ? false : LogManager.log(styles);
	}
	
	final void setInnerHTMLForScriptPanel(final Mlet mlet, final ScriptPanel scriptPanel, final String innerHTML, final boolean enableCache){
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				LogManager.errToLog("invalid status to setInnerHTML : \n" + innerHTML);
				return;
			}
			setInnerHTMLImplForScriptPanel(mlet, scriptPanel, innerHTML, enableCache);
		}
		L.V = L.WShop ? false : LogManager.log(innerHTML);
	}

	final void setInnerHTMLImplForScriptPanel(final Mlet mlet, final ScriptPanel scriptPanel, final String innerHTML,
			final boolean enableCache) {
		synchronized(mlet.synLock){
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				final int hashID = diffTodo.buildHcCode(scriptPanel);
				diffTodo.setDivInnerHTMLForScriptPanel(hashID, innerHTML, enableCache);
			}
		}
	}
	
	final void executeScriptWithoutCacheForScriptPanel(final Mlet mlet, final String js){
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				if(jsNoCacheToDeliver == null){
					jsNoCacheToDeliver = new Vector<String>();
				}
				jsNoCacheToDeliver.add(js);
				return;
			}
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				diffTodo.executeJSWithoutCacheForScriptPanel(js);
			}
		}
	}
	
	/**
	 * 
	 * @param js
	 * @since 7.7
	 */
	final void loadScriptForScriptPanel(final Mlet mlet, final String js, final boolean isCacheEnabled){
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				if(scriptToDeliver == null){
					scriptToDeliver = new Vector<CacheString>();
				}
				scriptToDeliver.add(new CacheString(js, isCacheEnabled));
				return;
			}
			loadScriptImplForScriptPanel(mlet, js, isCacheEnabled);
		}
	}

	final void loadScriptImplForScriptPanel(final Mlet mlet, final String js, final boolean isCacheEnabled) {
		synchronized(mlet.synLock){
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				diffTodo.loadScriptForScriptPanel(js, isCacheEnabled);
			}
		}
	}
	
	public final void setCSSImpl(final Mlet mlet, final ProjectContext ctx, final JComponent component, final String className, final String styles){//in user thread
		if(SimuMobile.checkSimuProjectContext(ctx) || component == null){
			return;
		}
		
		if(diffTodo == null && mlet.status > Mlet.STATUS_INIT){
			//MletSnapCanvas模式
			return;
		}
		
		if(className == null && styles == null){
			return;
		}
		
		if(component instanceof JPanel){
			if(mlet instanceof HTMLMlet){
				((HTMLMlet)mlet).setCSSForDiv(component, className, styles);//in user thread
			}
			return;
		}else if(component instanceof JToggleButton){
			doForLabelTag(mlet, (JToggleButton)component, className, styles);//in user thread
			return;
		}
		
		doForInputTag(mlet, TodoItem.FOR_JCOMPONENT, component, className, styles);//in user thread
	}
	
	public final void setCSSForDivImpl(final Mlet mlet, final ProjectContext ctx, final JComponent component, final String className, final String styles){//in user thread
		if(SimuMobile.checkSimuProjectContext(ctx) || component == null){
			return;
		}
		
		if(diffTodo == null && mlet.status > Mlet.STATUS_INIT){
			//MletSnapCanvas模式
			return;
		}
		
		if(className == null && styles == null){
			return;
		}
		
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				if(styleItemToDeliver == null){
					styleItemToDeliver = new Vector<TodoItem>();
				}
				styleItemToDeliver.add(new StyleItem(TodoItem.FOR_DIV, component, className, styles));
				return;
			}
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				diffTodo.setStyleForDiv(diffTodo.buildHcCode(component), className, styles);//in user thread
			}
		}
	}
	
	public final void setRTLForDivImpl(final Mlet mlet, final ProjectContext ctx, final JComponent component, final boolean isRTL){//in user thread
		if(SimuMobile.checkSimuProjectContext(ctx) || component == null){
			return;
		}
		
		if(diffTodo == null && mlet.status > Mlet.STATUS_INIT){
			//MletSnapCanvas模式
			return;
		}
		
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				if(styleItemToDeliver == null){
					styleItemToDeliver = new Vector<TodoItem>();
				}
				styleItemToDeliver.add(new RTLItem(TodoItem.FOR_RTL, component, isRTL));
				return;
			}
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){//由于本方法可能在构造中被调用，而无法确定是否需要后期转发，所以diffTofo条件限于此。
				diffTodo.setRTLForDiv(diffTodo.buildHcCode(component), isRTL);//in user thread
			}
		}
	}
	
	public final void setCSSForToggleImpl(final Mlet mlet, final ProjectContext ctx, final JToggleButton togButton, final String className, final String styles) {
		if(SimuMobile.checkSimuProjectContext(ctx) || togButton == null){
			return;
		}
		
		if(diffTodo == null && mlet.status > Mlet.STATUS_INIT){
			//MletSnapCanvas模式
			return;
		}
		
		if(className == null && styles == null){
			return;
		}
		
		doForInputTag(mlet, TodoItem.FOR_JTOGGLEBUTTON, togButton, className, styles);//in user thread
	}
	
	private final void doForLabelTag(final Mlet mlet, final JToggleButton togButton, final String className, final String styles) {//in user thread
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				if(styleItemToDeliver == null){
					styleItemToDeliver = new Vector<TodoItem>();
				}
				styleItemToDeliver.add(new StyleItem(TodoItem.FOR_JCOMPONENT, togButton, className, styles));
				return;
			}
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){
				diffTodo.setStyleForJCheckBoxText(diffTodo.buildHcCode(togButton), className, styles);//in user thread
			}
		}
	}
	
	private final void doForInputTag(final Mlet mlet, final int forType, final JComponent component, final String className,
			final String styles) {//in user thread
		synchronized(mlet.synLock){
			if(mlet.status == Mlet.STATUS_INIT){
				if(styleItemToDeliver == null){
					styleItemToDeliver = new Vector<TodoItem>();
				}
				styleItemToDeliver.add(new StyleItem(forType, component, className, styles));
				return;
			}
			if(diffTodo != null && mlet.status < Mlet.STATUS_EXIT){
				diffTodo.setStyleForInputTag(diffTodo.buildHcCode(component), className, styles);//in user thread
			}
		}
	}
	
	public ScriptCSSSizeHeight(final J2SESession coreSS, final ProjectContext ctx){
		super(coreSS);
		this.ctx = ctx;
		if(ctx != null){
			this.pr = ServerUIAPIAgent.getProjResponserMaybeNull(ctx);
		}else{
			this.pr = null;
		}
	}
}
