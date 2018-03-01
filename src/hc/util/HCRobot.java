package hc.util;

import hc.core.util.ExceptionReporter;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.image.BufferedImage;

public class HCRobot {
	private static Robot robot = null;

	/**
	 * 如果安全限制，则返回null
	 * 
	 * @return
	 */
	public Robot getInstance() {
		if (robot == null) {
			try {
				robot = new Robot();
			} catch (final AWTException e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
		return robot;
	}

	public BufferedImage createScreenCapture() {
		// return robot.createScreenCapture(new Rectangle(
		// dim.width, dim.height));//这里是用来设定截屏的形状和位置，都是可以自己设定的
		return null;
	}

}
