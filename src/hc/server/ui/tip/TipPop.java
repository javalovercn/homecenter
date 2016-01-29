package hc.server.ui.tip;

import hc.App;
import hc.core.HCTimer;
import hc.res.ImageSrc;
import hc.server.ui.tip.TipPop.CornerPosition;
import hc.util.ResourceUtil;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.TrayIcon.MessageType;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class TipPop {
	final static String BACKGROUND_COLOR = "#ffffe1";
	final static String BORDER_COLOR = "#000000";
	static MessageTipFrame instanceMessageFrame;
	static final int BORDER_WIDTH = 6;
	static final int FOOT_WIDTH = 20;
	
	public enum CornerPosition {
		LEFT_TOP,
		RIGHT_TOP,
		LEFT_DOWN,
		RIGHT_DOWN,
		UN_KNOWN;
	};
	
	static final HCTimer autoClose = new HCTimer("", 1000 * 20, false) {
		@Override
		public final void doBiz() {
			if(instanceMessageFrame != null){
				instanceMessageFrame.close();
			}
		}
	};
	
	public static void setCornerPosition(final int trayLocX, final int trayLocY, final int trayWidth, final int trayHeight){
		CornerPosition position = MessageTipFrame.converTo(trayLocX, trayLocY);
		
		if(instanceMessageFrame == null){
			instanceMessageFrame = new MessageTipFrame();
		}else{
			if(instanceMessageFrame.position != position){
				instanceMessageFrame.dispose();
				instanceMessageFrame = new MessageTipFrame();
			}
		}
		instanceMessageFrame.setCornerPosition(position, trayLocX, trayLocY, trayWidth, trayHeight);
	}
	
	public static void close(){
		if(instanceMessageFrame != null){
			instanceMessageFrame.close();
		}
	}
	
	public static synchronized void displayMessage(final String caption, final String msg, final MessageType messageType){
		if(instanceMessageFrame == null){
			instanceMessageFrame = new MessageTipFrame();
		}else{
			instanceMessageFrame.close();
		}
//		L.V = L.O ? false : LogManager.log("displayMessage " + System.currentTimeMillis());
		instanceMessageFrame.showMessage(caption, msg, messageType);
	}
		
	public static void exit(){
		if(instanceMessageFrame != null){
			instanceMessageFrame.dispose();
			instanceMessageFrame = null;
		}
	}
}

class BorderPanel extends JPanel{
	final int startX, startY, endX, endY;
	final MessageTipFrame mtf;
	
	BorderPanel(MessageTipFrame mtf, int startX, int startY, int endX, int endY){
		this.mtf = mtf;
		
		this.startX = startX;
		this.startY = startY;
		this.endX = endX;
		this.endY = endY;
		
		setPreferredSize(new Dimension(TipPop.BORDER_WIDTH, TipPop.BORDER_WIDTH));
	}
	
//	public Dimension getPreferredSize() {
//        return new Dimension(6, 6);
//    }
	
	public void paint(Graphics g){
		g.setColor(Color.decode(TipPop.BACKGROUND_COLOR));
		g.fillRect(0, 0, getWidth(), getHeight());
		
		g.setColor(Color.decode(TipPop.BORDER_COLOR));
		if(startX == startY){
			if(startY == endY){
				//顶
				g.drawLine(startX, startY, getWidth(), endY);
				
				//绘制缺口，连接Footer部分
				CornerPosition p = mtf.position;
				final int foot_width_minus_two = TipPop.FOOT_WIDTH - 2;
				g.setColor(Color.decode(TipPop.BACKGROUND_COLOR));
				if(p == CornerPosition.LEFT_TOP || p == CornerPosition.RIGHT_TOP){
					int x1;
					if(p == CornerPosition.LEFT_TOP){
						x1 = 1;
					}else{
						x1 = getWidth() - TipPop.FOOT_WIDTH;
					}
					g.drawLine(x1, startY, x1 + foot_width_minus_two, endY);
				}
			}else{
				//左
				g.drawLine(startX, startY, endX, getHeight());
			}
		}else{
			if(startX > startY){
				//右
				g.drawLine(getWidth() - 1, startY, getWidth() - 1, getHeight());
			}else{
				//底
				g.drawLine(startX, getHeight() - 1, getWidth(), getHeight() - 1);

				//绘制缺口，连接Footer部分
				CornerPosition p = mtf.position;
				final int foot_width_minus_two = TipPop.FOOT_WIDTH - 2;
				g.setColor(Color.decode(TipPop.BACKGROUND_COLOR));
				if(p == CornerPosition.LEFT_DOWN || p == CornerPosition.RIGHT_DOWN){
					int x1;
					if(p == CornerPosition.LEFT_DOWN){
						x1 = 1;
					}else{
						x1 = getWidth() - TipPop.FOOT_WIDTH;
					}
					g.drawLine(x1, getHeight() - 1, x1 + foot_width_minus_two, getHeight() - 1);
				}
			}
		}
	}
}

class TransparentPanel extends JPanel{
	TransparentPanel(){
		setOpaque(false);
	}
}

class CornerPanel extends JPanel{
	final BufferedImage bi;
	boolean isPainted = false;
	CornerPanel(BufferedImage bi){
		this.bi = bi;
		setOpaque(false);
	}
	
	@Override
    public Dimension getPreferredSize() {
        return new Dimension(bi.getWidth(), bi.getHeight());
    }
	
	public void paint(Graphics g){
		if(isPainted){
			return;
		}
		isPainted = true;
		g.drawImage(bi, 0, 0, null);
	}
}