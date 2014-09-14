package hc.util;

import hc.res.ImageSrc;
import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;
import javax.swing.JFrame;
import com.sun.awt.AWTUtilities;
 
public class TransparentFrame extends JFrame {
 
    private Image img;
    public TransparentFrame(Image image) {
        super();
 
        img = image;

        try {
            initialize();// 窗体初始化
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    /**
     * 窗体初始化
     *
     * @throws IOException
     */
    private void initialize() throws IOException {
        this.setSize(img.getWidth(null), img.getHeight(null));
        this.setUndecorated(true);
 
        //Shape形状
        AWTUtilities.setWindowShape(this, ImageSrc.getImageShape(img));
        //透明度
        //AWTUtilities.setWindowOpacity
        
        setVisible(true);
    }
 
    @Override
    public void paint(Graphics g) {
        // super //闪烁
        g.drawImage(img, 0, 0, null);
    }
    
    public void refresh(Image image) {
    	this.img = image;
    	repaint();
    }
 
}
