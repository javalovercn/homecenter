package hc.server.ui.design.hpj.ctrl;

import hc.core.util.CtrlItem;
import hc.core.util.CtrlKeySet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;


public class CtrlPanel extends JPanel {
	public static final int BLOCK_WIDTH = 240;
	public static final int HALF_BLOCK_WIDTH = BLOCK_WIDTH / 2;
	private int currSelectKeyValue = -1;
	private final CtrlItem[] itemsOnCanvas = new CtrlItem[CtrlKeySet.MAX_CTRL_ITEM_NUM];
	private final CtrlTotalPanel totalPanel;
	private final Cursor moveCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
	private final Cursor defaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
	final int CELL_WIDTH = 10, START_IDX = 5;
	
	private int pressMouseX, pressMouseY, oriCenterX, oriCenterY;
	
	public CtrlPanel(final CtrlTotalPanel tp) {
		super();
		
		this.totalPanel = tp;
		
		addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(final MouseEvent e) {
				final int currSelect = currSelectKeyValue;
				if(doMove(e)){
					resetPressXY();
					
					if(currSelect >= 0){
						final CtrlItem ci = itemsOnCanvas[currSelect];
						ajustCanvasSize(ci.center_x, ci.center_y);
					}
					return;
				}
			}
			
			@Override
			public void mousePressed(final MouseEvent e) {
				totalPanel.panel_canvas.requestFocus();
				
				final int x = e.getX();
				final int y = e.getY();
				resetPressXY();
				for (int i = 0; i < itemsOnCanvas.length; i++) {
					final CtrlItem ci = itemsOnCanvas[i];
					if(ci != null && x > (ci.center_x - ci.halfWidth) && x < (ci.center_x + ci.halfWidth)
							&& y > (ci.center_y - ci.halfHeight) && y < (ci.center_y + ci.halfHeight)){
						setCurrSelectedItem(i);
						repaint();
						pressMouseX = x;
						pressMouseY = y;
						oriCenterX = ci.center_x;
						oriCenterY = ci.center_y;
						return;
					}
				}
			}

			@Override
			public void mouseExited(final MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(final MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(final MouseEvent e) {
			}
		});
		
		addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(final MouseEvent e) {
			}
			
			@Override
			public void mouseDragged(final MouseEvent e) {
				if(doMove(e)){
					return;
				}
			}
		});
		
		addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(final KeyEvent e) {
			}
			
			@Override
			public void keyReleased(final KeyEvent e) {
				final int k = e.getKeyCode();
				if(k == KeyEvent.VK_DELETE || k == KeyEvent.VK_BACK_SPACE){
					if(currSelectKeyValue >=0){
						totalPanel.removeFromCanvas(currSelectKeyValue);
						repaint();
						
						totalPanel.ctrlMap.removeButtonFromCanvas(currSelectKeyValue);
						resetSelectItem();

						return;
					}
				}
			}
			
			@Override
			public void keyPressed(final KeyEvent e) {
			}
		});
		
		addFocusListener(new FocusListener() {
			@Override
			public void focusLost(final FocusEvent e) {
				resetSelectItem();
				repaint();
			}
			
			@Override
			public void focusGained(final FocusEvent e) {
			}
		});
	}

	private void resetPressXY() {
		pressMouseX = -1;
		pressMouseY = -1;
	}
	
	public void findInitSize(){
		int maxWidth = 0, maxHeight = 0;
		for (int i = 0; i < itemsOnCanvas.length; i++) {
			final CtrlItem ci = itemsOnCanvas[i];
			if(ci != null){
				if(ci.center_x > maxWidth){
					maxWidth = ci.center_x;
				}
				if(ci.center_y > maxHeight){
					maxHeight = ci.center_y;
				}
			}
		}
		
		final Dimension newDimension = new Dimension(maxWidth / BLOCK_WIDTH * BLOCK_WIDTH + BLOCK_WIDTH + HALF_BLOCK_WIDTH, 
				maxHeight / BLOCK_WIDTH * BLOCK_WIDTH + BLOCK_WIDTH + HALF_BLOCK_WIDTH);
//		System.out.println("MaxWidth : " + newDimension.width + ", MaxHeight : " + newDimension.height);
		setPreferredSize(newDimension);
		setSize(newDimension);
	}
	
	@Override
	public void paintComponent(final Graphics g) {
		super.paintComponent(g);
		
		final int width = getWidth(), height = getHeight();

		g.setColor(Color.WHITE);
	    g.fillRect(0, 0, width, height);
	    
	    //绘制最小坐标网格线
		g.setColor(Color.LIGHT_GRAY);
	    for (int i = START_IDX; i < width; ) {
	    	if((((i - START_IDX) % BLOCK_WIDTH) != 0) && (((i + START_IDX) % BLOCK_WIDTH) != 0) &&
	    			(((i + START_IDX) % HALF_BLOCK_WIDTH == 0) || ((i - START_IDX) % HALF_BLOCK_WIDTH == 0))){
	    	}else{
	    		g.drawLine(i, START_IDX, i, height);
	    	}
			i += CELL_WIDTH;
		}
	    for (int i = START_IDX; i < height; ) {
			g.drawLine(START_IDX, i, width, i);
			i += CELL_WIDTH;
		}

	    //绘制区块间边际线
		g.setColor(Color.GRAY);
//	    for (int i = BLOCK_WIDTH; i < width; ) {
//			g.drawLine(i, 0, i, height);
//			i += BLOCK_WIDTH;
//		}
//	    for (int i = BLOCK_WIDTH; i < height; ) {
//			g.drawLine(0, i, width, i);
//			i += BLOCK_WIDTH;
//		}
	    //绘制中轴线
	    for (int i = 0; i < width; ) {
			g.drawLine(i + HALF_BLOCK_WIDTH, START_IDX + 1, i + HALF_BLOCK_WIDTH, height);
			//START_IDX + 1，+1是为了消除顶部明显的黑点
			
			i += BLOCK_WIDTH;
		}
//	    for (int i = 0; i < height; ) {
//			g.drawLine(0, i + HALF_BLOCK_WIDTH, width, i + HALF_BLOCK_WIDTH);
//			i += BLOCK_WIDTH;
//		}

	    //绘制每个工作区块
	    g.setColor(Color.decode("#A7A7E4"));//浅蓝
	    {
		    final int lineWidth = 4;
	    	final int halfLineWidth = lineWidth/2;
			final BasicStroke bs=new BasicStroke(lineWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND);
		    final Graphics2D g2d = (Graphics2D)g;
		    final Stroke oldStrok = g2d.getStroke();
		    final Object oldRender = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		    g2d.setStroke(bs);
		    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		    final int arcSize = 30;
		    for (int j = START_IDX; j < height; ) {
				for (int i = START_IDX; i < width; ) {
					g2d.drawRoundRect(i + halfLineWidth, j + halfLineWidth, 
							BLOCK_WIDTH - lineWidth - 2 * START_IDX, BLOCK_WIDTH- lineWidth - 2 * START_IDX, 
							arcSize, arcSize);
					i += BLOCK_WIDTH;
				}
				j += BLOCK_WIDTH;
			}
		    g2d.setStroke(oldStrok);
		    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldRender);
	    }

	    for (int i = 0; i < itemsOnCanvas.length; i++) {
			final CtrlItem ci = itemsOnCanvas[i];
			if(ci != null){
				final BufferedImage bi = totalPanel.canvas_images[i];
				g.drawImage(bi, ci.center_x - bi.getWidth()/2, ci.center_y - bi.getHeight()/2, null);
			}
		}
	    
	    if(currSelectKeyValue >=0){
	    	final CtrlItem ci = itemsOnCanvas[currSelectKeyValue];
			final BufferedImage bi = totalPanel.canvas_images[currSelectKeyValue];
			final int halfX = bi.getWidth()/2;
			final int halfY = bi.getHeight()/2;
			drawSelectedBox(g, ci.center_x - halfX, ci.center_y - halfY, ci.center_x + halfX, ci.center_y + halfY);
	    }
	}
	
	private void drawSelectedBox(final Graphics g, 
			final int startX, final int startY, final int endX, final int endY){
		final int width = endX - startX, height = endY - startY;
		g.setColor(Color.DARK_GRAY);
		
		final int litterBoxWidth = 4;
		final int halfLitterBoxWidth = litterBoxWidth /2;
		{
			g.drawRect(startX - halfLitterBoxWidth, startY - halfLitterBoxWidth, litterBoxWidth, litterBoxWidth);
			g.drawRect(startX - halfLitterBoxWidth + width, startY - halfLitterBoxWidth, litterBoxWidth, litterBoxWidth);
			g.drawRect(startX - halfLitterBoxWidth, startY - halfLitterBoxWidth + height, litterBoxWidth, litterBoxWidth);
			g.drawRect(startX - halfLitterBoxWidth + width, startY - halfLitterBoxWidth + height, litterBoxWidth, litterBoxWidth);
		}

		g.drawLine(startX + halfLitterBoxWidth, startY, endX - halfLitterBoxWidth, startY);
		g.drawLine(startX, startY + halfLitterBoxWidth, startX, endY - halfLitterBoxWidth);
		g.drawLine(endX, startY + halfLitterBoxWidth, endX, endY - halfLitterBoxWidth);
		g.drawLine(startX + halfLitterBoxWidth, endY, endX - halfLitterBoxWidth, endY);
	}
	
	public void setCurrSelectedItem(final int keyValue){
		currSelectKeyValue = keyValue;		
		setCursor(moveCursor);
	}
	
	public CtrlItem addItemToCanvas(final int keyValue, final int center_x, final int center_y){
		final CtrlItem ci = new CtrlItem();
		final BufferedImage bi = totalPanel.canvas_images[keyValue];
		ci.halfWidth = bi.getWidth()/2;
		ci.halfHeight = bi.getHeight()/2;
		findBestLocationItem(ci, center_x, center_y);
		totalPanel.ctrlMap.updateButtonOnCanvas(keyValue, ci.center_x, ci.center_y);
		itemsOnCanvas[keyValue] = ci;
		
		ajustCanvasSize(ci.center_x, ci.center_y);
		
		return ci;
	}
	
	private void findBestLocationItem(final CtrlItem ci, final int center_x, final int center_y){
		final int halfImgWidth = ci.halfWidth;
		final int halfImgHeight = ci.halfHeight;
		final int panelHalfWidth = center_x/BLOCK_WIDTH*BLOCK_WIDTH + BLOCK_WIDTH/2;
		
		final int halfWidthAndStart = halfImgWidth + START_IDX;
		final int areaXIdx = (center_x - halfWidthAndStart) / BLOCK_WIDTH;
		if(center_x - halfWidthAndStart < 0){
			//出左边界
			ci.center_x = halfWidthAndStart;
		}else if(center_x > panelHalfWidth - START_IDX - CELL_WIDTH && center_x <= panelHalfWidth + CELL_WIDTH + CELL_WIDTH){
			//调中
			ci.center_x = panelHalfWidth;
		}else if((center_x + halfImgWidth) / BLOCK_WIDTH > areaXIdx){
			//跨区块线
			if(center_x / BLOCK_WIDTH > areaXIdx){
				ci.center_x = (areaXIdx + 1) * BLOCK_WIDTH + halfWidthAndStart;
			}else{
				ci.center_x = (areaXIdx + 1) * BLOCK_WIDTH - halfWidthAndStart;
			}
		}else{
			//坐网格
			ci.center_x = center_x/CELL_WIDTH*CELL_WIDTH;
		}
		final int halfHeightAndStart = halfImgHeight + START_IDX;
		final int areaYIdx = (center_y - halfHeightAndStart) / BLOCK_WIDTH;
		if(center_y - halfHeightAndStart < 0){
			//出顶边界
			ci.center_y = halfHeightAndStart;
		}else if((center_y + halfImgHeight) / BLOCK_WIDTH > areaYIdx){
			//跨区块线
			if(center_y / BLOCK_WIDTH > areaYIdx){
				ci.center_y = (areaYIdx + 1) * BLOCK_WIDTH + halfHeightAndStart;
			}else{
				ci.center_y = (areaYIdx + 1) * BLOCK_WIDTH - halfHeightAndStart;
			}
		}else{
			//坐网格
			ci.center_y = center_y/CELL_WIDTH*CELL_WIDTH;
		}
	}
	
	public void removeItemToCanvas(final int keyValue){
		itemsOnCanvas[keyValue] = null;
	}
	
	public void resetSelectItem(){
		currSelectKeyValue = -1;
		setCursor(defaultCursor);
	}

	private boolean doMove(final MouseEvent e) {
		if(pressMouseX >= 0 && pressMouseY >= 0){
			final int x = e.getX();
			final int y = e.getY();
			
			if(x == pressMouseX && y == pressMouseY){
				return false;
			}
			final CtrlItem ctrlItem = itemsOnCanvas[currSelectKeyValue];
			
			findBestLocationItem(ctrlItem, oriCenterX + (x - pressMouseX), 
					oriCenterY + (y - pressMouseY));
			
			repaint();//totalPanel.panel_canvas.
			totalPanel.baseMenuItemPanel.notifyModified();
			totalPanel.ctrlMap.updateButtonOnCanvas(currSelectKeyValue, ctrlItem.center_x, ctrlItem.center_y);
			return true;
		}
		return false;
	}

	private void ajustCanvasSize(final int maxW, final int maxH) {
		Dimension newDimension = null;
		if(maxW / BLOCK_WIDTH * BLOCK_WIDTH + BLOCK_WIDTH > getWidth()){
			newDimension = new Dimension(getWidth() + BLOCK_WIDTH, getHeight());
		}
		if(maxH / BLOCK_WIDTH * BLOCK_WIDTH + BLOCK_WIDTH > getHeight()){
			if(newDimension == null){
				newDimension = new Dimension(getWidth(), getHeight() + BLOCK_WIDTH);
			}else{
				newDimension.height = getHeight() + BLOCK_WIDTH;
			}
		}
		if(newDimension != null){
			if((newDimension.width % BLOCK_WIDTH) < HALF_BLOCK_WIDTH){
				newDimension.width += HALF_BLOCK_WIDTH;
			}
			if((newDimension.height % BLOCK_WIDTH) < HALF_BLOCK_WIDTH){
				newDimension.height += HALF_BLOCK_WIDTH;
			}
//			System.out.println("MaxWidth : " + newDimension.width + ", MaxHeight : " + newDimension.height);
			setPreferredSize(newDimension);
			setSize(newDimension);
		}
	}
}
