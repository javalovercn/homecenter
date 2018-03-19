package hc.server.ui.design;

import hc.server.ui.design.hpj.HCjar;
import hc.util.ResourceUtil;

import java.util.Map;

public class NativeOSManager {
	public final static int OS_UNKNOWN = 0;

	public final static int OS_WINDOW = 1;
	public final static int OS_LINUX = 1 << 1;
	public final static int OS_MAC_OSX = 1 << 2;
	public final static int OS_ANDROID = 1 << 3;

	public final static int OLD_DEFAULT_OS_MASK = OS_WINDOW | OS_LINUX | OS_MAC_OSX | OS_ANDROID;

	public static int getOSMaskFromMap(final Map<String, Object> map, final int idx) {
		final String osMaskStr = (String) map.get(HCjar.replaceIdxPattern(HCjar.SHARE_NATIVE_FILE_OS_MASK, idx));
		int osMask = NativeOSManager.OLD_DEFAULT_OS_MASK;
		if (osMaskStr != null) {
			try {
				osMask = Integer.parseInt(osMaskStr);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return osMask;
	}

	// public static void main(final String[] args) {
	// int mask = OS_UNKNOWN;
	// printMask(mask);
	//
	// mask = maskOS(mask, OS_WINDOW);
	// printMask(mask);
	//
	// mask = maskOS(mask, OS_LINUX);
	// printMask(mask);
	//
	// mask = maskOS(mask, OS_MAC_OSX);
	// printMask(mask);
	//
	// mask = maskOS(mask, OS_ANDROID);
	// printMask(mask);
	// }

	private static void printMask(final int mask) {
		System.out.println("===================");
		if (isMatchOS(OS_WINDOW, mask)) {
			System.out.println("is Window");
		}
		if (isMatchOS(OS_LINUX, mask)) {
			System.out.println("is Linux");
		}
		if (isMatchOS(OS_MAC_OSX, mask)) {
			System.out.println("is MacOS");
		}
		if (isMatchOS(OS_ANDROID, mask)) {
			System.out.println("is Android");
		}
	}

	public static int getOS() {
		int out = OS_UNKNOWN;

		if (ResourceUtil.isAndroidServerPlatform()) {
			out = OS_ANDROID;
		} else {
			if (ResourceUtil.isWindowsOS()) {
				out = OS_WINDOW;
			} else if (ResourceUtil.isLinux()) {
				out = OS_LINUX;
			} else if (ResourceUtil.isMacOSX()) {
				out = OS_MAC_OSX;
			}
		}

		return out;
	}

	/**
	 * check <code>os</code> is in <code>osMask</code>.
	 * 
	 * @param os
	 * @param osMask
	 * @return
	 */
	public static boolean isMatchOS(final int os, final int osMask) {
		return (os & osMask) > 0;
	}
}
