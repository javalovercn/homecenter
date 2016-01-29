package hc.server.ui;

import hc.server.util.ContextSecurityConfig;
import hc.server.util.ContextSecurityManager;

public class ScreenAdapter {
	public static final int TYPE_MLET = 1;
	public static final int TYPE_SERVER = 2;
	
	public final int width;
	public final int height;
	public final int mobileDPI;
	public final int type;
	public final static int STAND_SCREEN_HEIGHT = 768;

	public ScreenAdapter(final int w, final int h, final int dpi, final int type){
		this.width = w;
		this.height = h;
		if(dpi == 0){
			this.mobileDPI = 120;
		}else{
			this.mobileDPI = dpi;
		}
		this.type = type;
	}
	
	public final int getAdapterWidth(final int screenWidth){
		return width * screenWidth / ScreenAdapter.STAND_SCREEN_HEIGHT;
	}

	public final int getAdapterHeight(final int screenHeight){
		return height * screenHeight / ScreenAdapter.STAND_SCREEN_HEIGHT;
	}
	
	public final int imageSizeToScreenFloat(int size, float zoom) {
		if(type == TYPE_MLET){
			return size;
		}else{
			final int max = Math.max(width, height);
			return (int)( max * size * zoom / ScreenAdapter.STAND_SCREEN_HEIGHT);//UICore.getDeviceDensity()
		}
	}
	
//	/**
//	 * 将一个设定到缺省密度的尺寸。
//	 * @param fileName
//	 * @param defaultDensitySize
//	 * @return
//	 */
//	public Bitmap getDIPBitmap(String fileName, float zoom) {
//		InputStream is = J2SEInitor.class.getResourceAsStream(fileName);
//		Bitmap bitmap = BitmapFactory.decodeStream(is);
//		return getDIPBitmap(bitmap, zoom);
//	}
	
	public static final ScreenAdapter initScreenAdapterFromContext(){
		ContextSecurityConfig csc = ContextSecurityManager.getConfig(Thread.currentThread().getThreadGroup());
		if(csc != null){
			final ProjectContext projectContext = csc.getProjectContext();
			return new ScreenAdapter(projectContext.getMobileWidth(), projectContext.getMobileHeight(), 
					projectContext.getMobileDPI(), TYPE_MLET);
		}
		return null;
	}
}
