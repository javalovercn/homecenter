package hc.res;

import hc.util.ResourceUtil;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.Icon;

public class ImageSrc {
	//由于rename不支持"_"，会导致新文件长度为0，所以由hc_bak.log改为hcbak.log
	public static final String HC_LOG_BAK = "hcbak.log";

	public static final String HC_LOG = "hc.log";

	public static final URL SEARCH_ICON = ResourceUtil.getResource("hc/res/search_22.png");
	public static final URL UP_ICON = ResourceUtil.getResource("hc/res/up_22.png");
	public static final URL DOWN_ICON = ResourceUtil.getResource("hc/res/down_22.png");
	public static final URL UP_SMALL_ICON = ResourceUtil.getResource("hc/res/up_22.png");
	public static final URL DOWN_SMALL_ICON = ResourceUtil.getResource("hc/res/down_22.png");
	public static final URL REMOVE_SMALL_ICON = ResourceUtil.getResource("hc/res/remove_22.png");
	public static final URL TEST_SMALL_ICON = ResourceUtil.getResource("hc/res/test_22.png");
	public static final URL ADD_SMALL_ICON = ResourceUtil.getResource("hc/res/add_22.png");
	public static final URL MODIFY_SMALL_ICON = ResourceUtil.getResource("hc/res/modify_22.png");
	public static final URL BUILD_SMALL_ICON = ResourceUtil.getResource("hc/res/build_22.png");
	
	public static final URL OK_ICON = ResourceUtil.getResource("hc/res/ok_22.png");
	public static final URL CANCEL_ICON = ResourceUtil.getResource("hc/res/cancel_22.png");
	
	public static final URL HELP_ICON = ResourceUtil.getResource("hc/res/help_22.png");
	
	public static final URL EXIT_ICON = ResourceUtil.getResource("hc/res/exit.png");
	
	public static final URL DONE_ICON = ResourceUtil.getResource("hc/res/done_22.png");
	public static final URL GO_ICON = ResourceUtil.getResource("hc/res/go_16.png");
	public static final URL NONE_ICON = ResourceUtil.getResource("hc/res/none_16.png");
	
	public static final URL LOCK_ICON = ResourceUtil.getResource("hc/res/lock_22.png");
	
	public static final URL LANG_ICON = ResourceUtil.getResource("hc/res/global_22.png");
	
//	public static final URL CRYPTED_ICON = ResourceUtil.getResource("hc/res/halfencrypted_22.png");
	
	public static final URL PASSWORD_ICON = ResourceUtil.getResource("hc/res/password_22.png");
	
	public static final URL LOG_ICON = ResourceUtil.getResource("hc/res/logdoc_22.png");
	
	public static final URL LOG_BAK_ICON = ResourceUtil.getResource("hc/res/logdbk22.png");
	
	public static final URL NEW_CERTKEY_ICON = ResourceUtil.getResource("hc/res/newck_22.png");
	
	public static final URL DISABLE_DL_CERTKEY_ICON = ResourceUtil.getResource("hc/res/disab_ck_dl.png");
	public static final URL MOVE_TO_ICON = ResourceUtil.getResource("hc/res/moveto.png");
	public static final URL MOVE_TO_G_ICON = ResourceUtil.getResource("hc/res/moveto_g.png");
	
	public static final URL DL_CERTKEY_ICON = ResourceUtil.getResource("hc/res/dl_ckey_22.png");

	public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
		int w = image.getWidth();
		int h = image.getHeight();
		BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = output.createGraphics();
	
	    g2.setComposite(AlphaComposite.Src);//SrcAtop
	    g2.drawImage(image, 0, 0, null);
	    
		Area clear = new Area(new Rectangle(0, 0, w, h));
	    clear.subtract(new Area(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius,
	            cornerRadius)));
		g2.setComposite(AlphaComposite.Clear);
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	            RenderingHints.VALUE_ANTIALIAS_ON);
	    g2.fill(clear);
	    
	    g2.dispose();
	
		return output;
	}

	public static BufferedImage composeImage(BufferedImage base, BufferedImage cover){
		BufferedImage bi = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g2d = bi.createGraphics();
		g2d.setComposite(AlphaComposite.SrcOver);
	    g2d.drawImage(base, 0, 0, null);
		g2d.setComposite(AlphaComposite.SrcOver);
		g2d.drawImage(cover, 0, 0, null);
		return bi;
	}

	public static BufferedImage iconToImage(Icon icon){
	    BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
	    icon.paintIcon(null, image.getGraphics(), 0, 0);
	    return image;
	}

	public static BufferedImage toBufferedImage(Image img){
	    if (img instanceof BufferedImage){
	        return (BufferedImage) img;
	    }

	    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    Graphics2D g = bimage.createGraphics();
	    g.drawImage(img, 0, 0, null);
	    g.dispose();

	    return bimage;
	}

	public static Shape getImageShape(Image img) {
	    ArrayList<Integer> x = new ArrayList<Integer>();
	    ArrayList<Integer> y = new ArrayList<Integer>();
	    final int width = img.getWidth(null);
	    final int height = img.getHeight(null);
	
	    PixelGrabber pg = new PixelGrabber(img, 0, 0, -1, -1, true);
	    try {
	        pg.grabPixels();
	    } catch (InterruptedException e) {
	        e.getStackTrace();
	    }
	    final int pixels[] = (int[]) pg.getPixels();
	
	    // 循环像素
	    for (int i = 0; i < pixels.length; i++) {
	        final int alpha = (pixels[i] >> 24) & 0xff;
	        if (alpha == 0) {
	            continue;
	        } else {
	            x.add(i % width > 0 ? i % width - 1 : 0);
	            y.add(i % width == 0 ? (i == 0 ? 0 : i / width - 1) : i / width);
	        }
	    }
	
	    final int[][] matrix = new int[height][width];
	    for (int i = 0; i < height; i++) {
	        for (int j = 0; j < width; j++) {
	            matrix[i][j] = 0;
	        }
	    }
	
	    for (int c = 0; c < x.size(); c++) {
	        matrix[y.get(c)][x.get(c)] = 1;
	    }
	
	    final Area rec = new Area();
	    int temp = 0;
	
	    for (int i = 0; i < height; i++) {
	        for (int j = 0; j < width; j++) {
	            if (matrix[i][j] == 1) {
	                if (temp == 0)
	                    temp = j;
	                else if (j == width) {
	                    if (temp == 0) {
	                        Rectangle rectemp = new Rectangle(j, i, 1, 1);
	                        rec.add(new Area(rectemp));
	                    } else {
	                        Rectangle rectemp = new Rectangle(temp, i,
	                                j - temp, 1);
	                        rec.add(new Area(rectemp));
	                        temp = 0;
	                    }
	                }
	            } else {
	                if (temp != 0) {
	                    Rectangle rectemp = new Rectangle(temp, i, j - temp, 1);
	                    rec.add(new Area(rectemp));
	                    temp = 0;
	                }
	            }
	        }
	        temp = 0;
	    }
	    return rec;
	}
}
