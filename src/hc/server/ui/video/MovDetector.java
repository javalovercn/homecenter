package hc.server.ui.video;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


public class MovDetector {
	private static final int blockWidth = 10;
	private static final int blockHeight = 10;
	private final int snapWidth, snapHeight;
	private final int[] snapRgbArray;
	private final int rgbSize, endGrayX, endGrayY;
	private final int GRAY_LEVEL_VALUE = (1 << 5);
	private final int GRAY_MASK = 0xFF - (GRAY_LEVEL_VALUE - 1);
	private final int blockArea;
	private static  int maxDiffX = 2, maxDiffY = 2, maxDiffTotal;
	private final int[] grayArray;
	private final int STATUS_EMPTY_MODEL = 0;
	private final int STATUS_WORKING = 1;
	private int grayStatus = STATUS_EMPTY_MODEL;
	
	public static void updateDiffMin(String width, String height){
		int w = Integer.parseInt(width);
		int h = Integer.parseInt(height);
		
		maxDiffX = w / blockWidth;
		if(maxDiffX < 2){
			maxDiffX = 2;
		}
		maxDiffY = h / blockHeight;
		if(maxDiffY < 2){
			maxDiffY = 2;
		}
		maxDiffTotal = maxDiffX * maxDiffY;
	}
	
	public MovDetector(int width, int height, String diffW, String diffH){
		snapWidth = width;
		snapHeight = height;
		
		rgbSize = snapWidth * snapHeight;
		snapRgbArray = new int[rgbSize];

		endGrayX = snapWidth / blockWidth;
		endGrayY = snapHeight / blockHeight;
		
		blockArea = blockHeight * blockWidth;
		
		grayArray = new int[rgbSize];
		
		updateDiffMin(diffW, diffH);
	}
	
	/**
	 * 返回true，表示符合移动目标快照特征。
	 * @param bs
	 * @return
	 */
	public boolean putIn(final int[] bs){
		System.arraycopy(bs, 0, snapRgbArray, 0, rgbSize);
		boolean isHuman = false;
		
		for (int i = 0; i < rgbSize; i++) {
			final int rgb = snapRgbArray[i];
			snapRgbArray[i] = (((rgb >>16 & 0xFF)*38 + (rgb >>8 & 0xFF)*75 + (rgb & 0xFF) * 15) >> 7);//>>7是标准灰色，>>9是转为64级灰色
		}
		
		//每个块合成一个点
		for (int r = 0; r < endGrayY; r++) {
			final int blockOri = r * blockHeight * snapWidth;
			for (int l = 0; l < endGrayX; l++) {
				int graySum = 0;
				final int startOri = blockOri + l * blockWidth;
				for (int k = 0; k < blockHeight; k++) {
					int rowStartIdx = startOri + k * snapWidth;
					final int endN = rowStartIdx + blockWidth;
					while (rowStartIdx < endN) {
						graySum += snapRgbArray[rowStartIdx++];
					}
				}
				snapRgbArray[r * endGrayX + l] = (graySum / blockArea) & GRAY_MASK;
			}
		}
		
		if(grayStatus == STATUS_EMPTY_MODEL){
			grayStatus = STATUS_WORKING;
		}else{
//			final long currentTimeMillis = System.currentTimeMillis();
//			saveGray(endGrayX, endGrayY, snapRgbArray, "A" + currentTimeMillis);
//			saveGray(endGrayX, endGrayY, grayArray, "B" + currentTimeMillis);

			//检测图画变化
			int searchDiff = 0;
			int diffNotNear = 0;
			int diffNear = 0;
//			System.out.println("Start detect...");
			outer:
			for (int i = 0; i < endGrayY; i++) {
				int lastCol = 0;
				int j = i * endGrayX;
				final int endJ = j + endGrayX;
				while (j < endJ) {
//					GRAY_LEVEL_VALUE
					if(snapRgbArray[j] != grayArray[j++]){ 
						if(lastCol + 1 == j){
							diffNear++;
//						}else{
//							diffNotNear++;
						}
						lastCol = j;
						if((diffNear) >= maxDiffTotal){//&& (diffNear > (diffNotNear<<1)
//							System.out.println("save gray");
							isHuman = true;
							break outer;
						}
//						System.out.println("Diff : " + searchDiff + ", diffNear : " + diffNear + ", limit : " + maxDiffTotal);
					}
				}
			}
//			System.out.println("end detect.");
		}
		System.arraycopy(snapRgbArray, 0, grayArray, 0, rgbSize);
		return isHuman;
	}

	private void saveGray(int w, int h, int[] array, String fileName) {
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
//		bi.setRGB(0, 0, w, h, array, 0, w);
		bi.getRaster().setPixels(0, 0, w, h, array);
		try {
			ImageIO.write(bi, "png", new File(CaptureConfig.getInstance().capSaveDir, fileName + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
