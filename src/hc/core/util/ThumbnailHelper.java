package hc.core.util;

public class ThumbnailHelper {

	/**
	 * 计算远屏缩略快照尺寸
	 * @param m_w
	 * @param m_h
	 * @return
	 */
	public static int[] calcThumbnail(int m_w, int m_h, final int pc_w, final int pc_h){
		final int maxSize = (m_h>m_w?m_w:m_h);
		final int levelSize = (m_w <= 240 || m_h <= 240)?120:240;
		final int MAX_SCREEN_SIZE = (maxSize > levelSize) ? levelSize : maxSize;
		if (m_h > m_w) {
			// 直屏机器
//			m_h = (m_w>MAX_SCREEN_SIZE?(MAX_SCREEN_SIZE*m_h/m_w):m_h);
			m_w = (m_w>MAX_SCREEN_SIZE?MAX_SCREEN_SIZE:m_w);
			m_h = m_w * pc_h / pc_w;
		} else {
			// 横屏机器
//			m_w = (m_h>MAX_SCREEN_SIZE?(MAX_SCREEN_SIZE*m_w/m_h):m_w);
			m_h = (m_h>MAX_SCREEN_SIZE?MAX_SCREEN_SIZE:m_h);
			m_w = m_h * pc_w / pc_h;
		}
	
		int[] out = {m_w, m_h};
		return out;
	}

	public static int[] calNewLocXY(boolean isZoomIn, int locX, int locY, int pcW, int pcH, int mobileW, int mobileH){
		if(isZoomIn){
			locX += mobileW / 4;
			locY += mobileH / 4;
		}else{
			locX -= mobileW / 4;
			locY -= mobileH / 4;
			
			if(locX > (pcW - mobileW)){
				locX = pcW - mobileW;//有可能产生负值
			}
			//故locX要置于后
			if(locX < 0){
				locX = 0;
			}

			if(locY > (pcH - mobileH)){
				locY = pcH - mobileH;//有可能产生负值
			}
			//有可能手机分辨率高于电脑，故locY要置于后
			if(locY < 0){
				locY = 0;
			}
		}
		
		final int[] back = {locX, locY};
		return back;
	}

}
