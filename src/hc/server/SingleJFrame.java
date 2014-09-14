package hc.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JFrame;

public class SingleJFrame extends JFrame {
	private static HashMap<String, SingleJFrame> frames = new HashMap<String, SingleJFrame>();
	
	public static void updateAllJFrame(){
		Collection<SingleJFrame> collect = frames.values();
		Iterator<SingleJFrame> it = collect.iterator();
		while(it.hasNext()){
			SingleJFrame sf = it.next();
			sf.updateSkinUI();
		}
	}
	
	public static SingleJFrame showJFrame(Class frameClass){
		final String className = frameClass.getName();
		SingleJFrame frame = frames.get(className);
		if(frame == null){
			try {
				frame = (SingleJFrame)frameClass.newInstance();
				frames.put(className, frame);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			frame.toFront();
		}
		return frame;
	}
	
	@Override
	public void dispose(){
		super.dispose();
		frames.remove(this.getClass().getName());
	}
	
	public void updateSkinUI(){
		ConfigPane.updateComponentUI(getContentPane());
	}
}
