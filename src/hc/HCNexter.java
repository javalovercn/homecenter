package hc;

import hc.core.ConditionWatcher;
import hc.core.IWatcher;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

public class HCNexter {
	final String[] imgUrl;
	public int idx;
	BufferedImage[] imgs;
	
	public HCNexter(String[] urls) throws Exception{
		this.imgUrl = urls;
		imgs = new BufferedImage[urls.length];
		
		imgs[0] = getImg(0);
		
		ConditionWatcher.addWatcher(new IWatcher() {
			
			@Override
			public boolean watch() {
				try{
					for (int i = 1; i < imgUrl.length; i++) {
						imgs[i] = getImg(i);
					}
				}catch (Exception e) {
					
				}
				return true;
			}
			
			@Override
			public void setPara(Object p) {
			}
			
			@Override
			public boolean isCancelable() {
				return false;
			}
			
			@Override
			public void cancel() {
			}
		});
	}

	private BufferedImage getImg(int idx) throws IOException, MalformedURLException {
		return ImageIO.read(new URL("http://homecenter.mobi/images/" + imgUrl[idx]));
	}
}
