package hc.res;

import hc.server.PlatformManager;
import hc.util.ResourceUtil;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import javax.swing.Icon;

public class ImageSrc {
	//由于rename不支持"_"，会导致新文件长度为0，所以由hc_bak.log改为hcbak.log
	public static final String HC_LOG_BAK = "hcbak.log";

	public static final String HC_LOG = "hc.log";

	public static final BufferedImage SEARCH_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/search_22.png"));
	public static final BufferedImage UP_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/up_22.png"));
	public static final BufferedImage DOWN_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/down_22.png"));
	public static final BufferedImage UP_SMALL_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/up_22.png"));
	public static final BufferedImage DOWN_SMALL_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/down_22.png"));
	public static final BufferedImage REMOVE_SMALL_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/remove_22.png"));
	public static final BufferedImage TEST_SMALL_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/test_22.png"));
	public static final BufferedImage ADD_SMALL_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/add_22.png"));
	public static final BufferedImage MODIFY_SMALL_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/modify_22.png"));
	public static final BufferedImage BUILD_SMALL_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/build_22.png"));
	
	public static final BufferedImage OK_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/ok_22.png"));
	public static final BufferedImage CANCEL_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/cancel_22.png"));
	public static final BufferedImage OK_44_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/ok_44.png"));
	public static final BufferedImage CANCEL_44_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/cancel_44.png"));
	
	public static final BufferedImage HELP_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/help_22.png"));
	
	public static final BufferedImage EXIT_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/exit.png"));
	
	public static final BufferedImage DONE_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/done_22.png"));
	public static final BufferedImage GO_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/go_16.png"));
	public static final BufferedImage NONE_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/none_16.png"));
	
	public static final BufferedImage LOCK_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/lock_22.png"));
	
	public static final BufferedImage LANG_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/global_22.png"));
	
//	public static final BufferedImage CRYPTED_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/halfencrypted_22.png"));
	
	public static final BufferedImage PASSWORD_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/password_22.png"));
	
	public static final BufferedImage LOG_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/logdoc_22.png"));
	
	public static final BufferedImage LOG_BAK_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/logdbk22.png"));
	
	public static final BufferedImage NEW_CERTKEY_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/newck_22.png"));
	
	public static final BufferedImage DISABLE_DL_CERTKEY_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/disab_ck_dl.png"));
	public static final BufferedImage MOVE_TO_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/moveto.png"));
	public static final BufferedImage MOVE_TO_G_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/moveto_g.png"));
	
	public static final BufferedImage DL_CERTKEY_ICON = ResourceUtil.getImage(ResourceUtil.getResource("hc/res/dl_ckey_22.png"));

	public static BufferedImage makeRoundedCorner(final BufferedImage image, final int cornerRadius) {
		return PlatformManager.getService().makeRoundedCorner(image, cornerRadius);
	}

	public static BufferedImage composeImage(final BufferedImage base, final BufferedImage cover){
		return PlatformManager.getService().composeImage(base, cover);
	}

	public static BufferedImage iconToImage(final Icon icon){
	    final BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
	    icon.paintIcon(null, image.getGraphics(), 0, 0);
	    return image;
	}

	public static BufferedImage toBufferedImage(final Image img){
	    if (img instanceof BufferedImage){
	        return (BufferedImage) img;
	    }

	    final BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    final Graphics2D g = bimage.createGraphics();
	    g.drawImage(img, 0, 0, null);
	    g.dispose();

	    return bimage;
	}

	public static Shape getImageShape(final Image img) {
	    return PlatformManager.getService().getImageShape(img);
	}
}
