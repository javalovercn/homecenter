package hc.util;

import hc.res.ImageSrc;
import hc.server.PlatformManager;
import hc.server.util.HCJFrame;

import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;
 
public class TransparentFrame extends HCJFrame {
 
    private Image img;
    public TransparentFrame(Image image) {
        super();
 
        img = image;

        try {
            initialize();// 窗体初始化
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        setVisible(true);

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
        PlatformManager.getService().setWindowShape(this, ImageSrc.getImageShape(img));
        //透明度
        //AWTUtilities.setWindowOpacity
        
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
