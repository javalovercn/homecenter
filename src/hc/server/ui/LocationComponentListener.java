package hc.server.ui;

import hc.util.PropertiesManager;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

public class LocationComponentListener implements ComponentListener {
	@Override
	public void componentShown(ComponentEvent e) {
	}
	
	@Override
	public void componentResized(ComponentEvent e) {
		saveLocation(e.getComponent());
	}
	
	@Override
	public void componentMoved(ComponentEvent e) {
		saveLocation(e.getComponent());
	}
	
	@Override
	public void componentHidden(ComponentEvent e) {
	}
	
	private void saveLocation(Component component){
		final String title = component.getName();
		final Rectangle r = component.getBounds();

//		System.out.println("saveLocation, x:" + r.x + ", y:" + r.y + ", w:" + r.width + ", h:" + r.height);

		PropertiesManager.setValue(title + PropertiesManager.p_WindowX, String.valueOf(r.x));
		PropertiesManager.setValue(title + PropertiesManager.p_WindowY, String.valueOf(r.y));
		PropertiesManager.setValue(title + PropertiesManager.p_WindowWidth, String.valueOf(r.width));
		PropertiesManager.setValue(title + PropertiesManager.p_WindowHeight, String.valueOf(r.height));
		
		PropertiesManager.saveFile();
	}
	
	public static boolean hasLocation(Component component){
		final String title = component.getName();
		return (PropertiesManager.getValue(title + PropertiesManager.p_WindowX) != null); 
	}
	
	public static boolean loadLocation(Component component){
		final String title = component.getName();
		try{
			component.setBounds(
					Integer.parseInt(PropertiesManager.getValue(title + PropertiesManager.p_WindowX)), 
					Integer.parseInt(PropertiesManager.getValue(title + PropertiesManager.p_WindowY)), 
					Integer.parseInt(PropertiesManager.getValue(title + PropertiesManager.p_WindowWidth)), 
					Integer.parseInt(PropertiesManager.getValue(title + PropertiesManager.p_WindowHeight)));
			return true;
		}catch (Throwable e) {
		}
		return false;
	}
}