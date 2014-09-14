package hc.server.ui;

import hc.core.IConstant;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;

import java.net.URLEncoder;

import javax.swing.JPanel;

/**
 * Mlet is an instance running in HomeCenter server, and the snapshot of Mlet UI is presented on mobile.
 * It looks more like a form, dialog, control panel or game canvas running in mobile.<br>
 * Mlet is extends {@link javax.swing.JPanel JPanel}, so you can bring all JPanel features to mobile UI, no matter whether Android or iOS.
 * <p>for demo, please goto <a target="_blank" href="http://homecenter.mobi/en/pc/steps_mlet.htm">http://homecenter.mobi/en/pc/steps_mlet.htm</a>
 */
public class Mlet extends JPanel implements ICanvas{
	public static final String URL_EXIT = HCURL.URL_CMD_EXIT;
	public static final String URL_SCREEN = HCURL.URL_HOME_SCREEN;
	
	public Mlet(){
		__context = ProjectContext.getProjectContext();
		if(__context != null){//测试用例时(TestMlet.java)，产生null
			__target = __context.__getTargetFromInnerMethod();
		}
	}
	
	/**
	 * @deprecated
	 */
	private String __target;
	/**
	 * @deprecated
	 */
	private ProjectContext __context;
	
	/**
	 * @return for example, screen://myMlet or cmd://playMusic
	 */
	public String getTarget(){
		return __target;
	}
	
	/**
	 * @return current project context
	 */
	public ProjectContext getProjectContext(){
		return __context;
	}
	
	public void go(String url){
		try {
			String encodeURL = URLEncoder.encode(url, IConstant.UTF_8);
			HCURLUtil.sendCmd(HCURL.DATA_CMD_SendPara, HCURL.DATA_PARA_TRANSURL, encodeURL);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
