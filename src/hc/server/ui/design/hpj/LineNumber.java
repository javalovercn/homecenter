package hc.server.ui.design.hpj;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JComponent;

public class LineNumber extends JComponent {
	private final static Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 12);
	private final static int HEIGHT = Integer.MAX_VALUE - 1000000;
	// Set right/left margin
	private int lineHeight;
	private int fontLineHeight;
	private int currentRowWidth;
	private int maxRow;
	private FontMetrics fontMetrics;

	/**
	 * Convenience constructor for Text Components
	 */
	public LineNumber(final JComponent component) {
		if (component == null) {
			setFont(DEFAULT_FONT);
		} else {
			setFont(component.getFont());
		}
		setPreferredSize(99);
	}

	public void setPreferredSize(final int row) {
		if (row > maxRow) {
			maxRow = row;
			final int width = fontMetrics.stringWidth(String.valueOf(row));
			final int doubleWidth = width * 2;
			if (currentRowWidth < doubleWidth) {
				currentRowWidth = doubleWidth;
				setPreferredSize(new Dimension(currentRowWidth, HEIGHT));
			}
		}
	}

	@Override
	public void setFont(final Font font) {
		super.setFont(font);
		fontMetrics = getFontMetrics(getFont());
		fontLineHeight = fontMetrics.getHeight();
	}

	public int getLineHeight() {
		if (lineHeight == 0)
			return fontLineHeight;
		else
			return lineHeight;
	}

	public void setLineHeight(final int lineHeight) {
		if (lineHeight > 0)
			this.lineHeight = lineHeight;
	}

	public int getStartOffset() {
		return 2;
	}

	@Override
	public void paintComponent(final Graphics g) {
		final int lineHeight = getLineHeight();
		final int startOffset = getStartOffset();
		final Rectangle drawHere = g.getClipBounds();
		g.setColor(getBackground());// 使用缺省背景色getBackground()
		// g.setColor(Color.YELLOW);
		g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height);
		g.setColor(Color.LIGHT_GRAY);
		final int x1 = drawHere.width - 5;
		g.drawLine(x1, drawHere.y, x1, drawHere.y + drawHere.height);
		g.setColor(new Color(165, 199, 234));// 使用缺省背景色getBackground()
		// g.setColor(Color.YELLOW);
		g.fillRect(drawHere.x, drawHere.y, drawHere.width / 2, drawHere.height);
		// Determine the number of lines to draw in the foreground.
		g.setColor(getForeground());
		final int startLineNumber = (drawHere.y / lineHeight) + 1;
		final int endLineNumber = startLineNumber + (drawHere.height / lineHeight);
		int start = (drawHere.y / lineHeight) * lineHeight + lineHeight - startOffset;
		// System.out.println( startLineNumber + " : " + endLineNumber + " : " +
		// start );
		for (int i = startLineNumber; i <= endLineNumber; i++) {
			final String lineNumber = String.valueOf(i);
			final int width = fontMetrics.stringWidth(lineNumber);
			g.drawString(lineNumber, currentRowWidth - width - currentRowWidth / 2, start);
			start += lineHeight;
		}
		setPreferredSize(endLineNumber);
	}
}
