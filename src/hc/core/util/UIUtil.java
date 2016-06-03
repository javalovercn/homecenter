package hc.core.util;

/**
 * 最底层参数
 *
 */
public class UIUtil {
	/**
	 * 乐1分辨率:1920*1080像素
	 * 乐1por分辨率:2560*1440像素，像素密度就达到534
	 * iPhone5:640*1136，像素密度就达到326PPI
	 */
	
	public static final int ICON_128 = 128;
	public static final int ICON_64 = 64;
	public static final int ICON_MAX = ICON_128;
	public static final int ICON_DESIGN_SHOW_SIZE = ICON_64;

	public static int calMenuIconSize(final int width, final int height, final int ppi) {
		if (width > 1800 || height > 1800 || ppi > 300) {
			return ICON_128;
		}

		return ICON_64;
	}
	
	/**
	 * 是否是整数倍大小
	 * @param bigSize
	 * @param smallSize
	 * @return
	 */
	public static boolean isIntBeiShu(final int bigSize, final int smallSize){
		return (bigSize != smallSize) && ((bigSize / smallSize * smallSize) == bigSize);
	}

	/**
	 * 返回处理完后的
	 * 
	 * @param zoomData
	 * @param ori_w
	 *            原始宽
	 * @param ori_h
	 *            原始高
	 * @param zoomIn
	 *            放大倍数
	 * @return
	 */
	public static int[] zoomInRGB(int[] zoomData, final int ori_w, final int ori_h, final int zoomIn) {
		final int newSize = ori_w * ori_h * zoomIn * zoomIn;

		if (zoomData.length < newSize) {
			final int[] newzoomdata = new int[newSize];
			System.arraycopy(zoomData, 0, newzoomdata, 0, zoomData.length);
			zoomData = newzoomdata;
		}

		// 像素放大N倍
		int j = newSize - 1;
		int doublej = j;
		for (int i = ori_w * ori_h - 1; i >= 0; i--) {
			final int pixelSource = zoomData[i];
			for (int z = 0; z < zoomIn; z++) {
				zoomData[j--] = pixelSource;
				// zoomInData[j--] = pixelSource;
			}
			if ((i % ori_w) == 0) {
				final int endIdx = j;
				for (int zz = 1; zz < zoomIn; zz++) {
					for (int k = doublej; k > endIdx;) {
						final int pixelDoubleSource = zoomData[k];
						for (int z = 0; z < zoomIn; z++) {
							zoomData[j--] = pixelDoubleSource;
							// zoomInData[j--] = pixelDoubleSource;
						}
						k -= zoomIn;
					}
				}
				doublej = j;
			}
		}

		return zoomData;
	}

	public static final String SYS_ICON_PREFIX = "Sys_";
	
	public static final String SYS_FOLDER_ICON = SYS_ICON_PREFIX + "Folder";
	public static final String SYS_ADD_DEVICE_BY_QR_ICON = SYS_ICON_PREFIX + "AddDeviceQR";
	public static final String SYS_ADD_DEVICE_BY_WIFI_ICON = SYS_ICON_PREFIX + "AddDeviceWiFi";
	public static final String SYS_DEFAULT_ICON = SYS_ICON_PREFIX + "Img";
	
	public static final int COLOR_TRANS_LAY = 0xcc847139;
	public static final int DEFAULT_COLOR_BACKGROUND = COLOR_TRANS_LAY & 0x00FFFFFF;

	/**
	 * 为AndroidServer环境使用本功能，故移到此。
	 * @param fromColor
	 * @param light
	 * @return
	 */
	public static int adjustColor(final int fromColor, final int light){
		int r = (fromColor >> 16 & 0x000000FF) + light;
		int g = (fromColor >> 8 & 0x000000FF) + light;
		int b = (fromColor & 0x000000FF) + light;
		
		if(r > 255){
			r = 255;
		}else if(r < 0){
			r = 0;
		}
		if(g > 255){
			g = 255;
		}else if(g < 0){
			g = 0;
		}
		if(b > 255){
			b = 255;
		}else if(b < 0){
			b = 0;
		}
		return (fromColor & 0xFF000000) | (r << 16) | (g << 8) | b;
	}
	
}
