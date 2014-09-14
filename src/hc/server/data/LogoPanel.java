package hc.server.data;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class LogoPanel extends JPanel {
    private BufferedImage image;
    int maxWidth, maxHeight;

    public LogoPanel(BufferedImage img) {
        image = img;
        this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
    }
    
    public LogoPanel(int maxWidth, int maxHeight) {
    	this.maxWidth = maxWidth;
    	this.maxHeight = maxHeight;
    	
    	nullImage = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
        this.setPreferredSize(new Dimension(maxWidth, maxHeight));
    }
    
    public void setBufferedImage(BufferedImage bi){
    	this.image = bi;
    }

    BufferedImage nullImage;
    
    public void paintComponent(Graphics g) {
    	if(image == null){
    		image = nullImage;
    	}
    	
    	if(maxWidth > 0){
    		//限定尺寸模式，根据外边界自动缩小图片
    		int imageWidth = image.getWidth();
			float fw = imageWidth / (maxWidth * 1.0F);
    		int imageHeight = image.getHeight();
			float fh = imageHeight / (maxHeight * 1.0F);
    		
    		if(fw > 1.0F || fh > 1.0F){
    			//进行缩小
        		if(fw < fh){
        			int realw = (int)(imageWidth / fh);
        			g.drawImage(image, 0, 0, realw, maxHeight, null);
        		}else{
        			int realh = (int)(imageHeight / fw);
        			g.drawImage(image, 0, 0, maxWidth, realh, null);
        		}
    		}else{
    			g.drawImage(image, (maxWidth - imageWidth) / 2, (maxHeight - imageHeight) / 2, imageWidth, imageHeight, null);
    		}
    		
    	}else{
    		//非限定尺寸模式
    		g.drawImage(image, 0, 0, null);
    	}
    }
}