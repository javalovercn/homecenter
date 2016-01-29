package hc.server;

import hc.App;
import hc.core.util.CCoreUtil;
import hc.server.util.HCJFrame;
import java.awt.Container;
import java.awt.Window;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class SingleJFrame extends HCJFrame {
	protected final ThreadGroup threadPoolToken = App.getThreadPoolToken();
	private static HashMap<String, Window> frames = new HashMap<String, Window>();
	
	public static void disposeAll(){
		CCoreUtil.checkAccess();
		
		Vector<String> vector = new Vector<String>();
		Iterator<String> names = frames.keySet().iterator();
		while(names.hasNext()){
			vector.add(names.next());
		}
		
		for (int i = 0; i < vector.size(); i++) {
			frames.get(vector.elementAt(i)).dispose();
		}
		
	}
	
	public static void updateAllJFrame(){
		CCoreUtil.checkAccess();
		
		Collection<Window> collect = frames.values();
		Iterator<Window> it = collect.iterator();
		while(it.hasNext()){
			Window sf = it.next();
			if(sf instanceof JDialog){
				updateSkinUI(((JDialog)sf).getContentPane());
			}else if(sf instanceof JFrame){
				updateSkinUI(((JFrame)sf).getContentPane());
			}
		}
	}
	
	public static Window showJFrame(Class frameClass){
		CCoreUtil.checkAccess();
		
		final String className = frameClass.getName();
		Window frame = frames.get(className);
		if(frame == null){
			try {
				frame = (SingleJFrame)frameClass.newInstance();
				addJFrame(className, frame);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			frame.toFront();
		}
		return frame;
	}

	public static void addJFrame(final String className, Window frame) {
		CCoreUtil.checkAccess();
		
		frames.put(className, frame);
	}
	
	@Override
	public void dispose(){
		super.dispose();
		removeJFrame(this.getClass().getName());
	}

	public static void removeJFrame(final String className) {
		CCoreUtil.checkAccess();
		
		frames.remove(className);
	}
	
	public void updateSkinUI(){
		updateSkinUI(getContentPane());
	}
	
	static void updateSkinUI(final Container container){
		App.invokeLaterUI(new Runnable() {
			@Override
			public void run() {
				ConfigPane.updateComponentUI(container);
			}
		});
	}
}
