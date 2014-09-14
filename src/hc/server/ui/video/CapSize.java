package hc.server.ui.video;

import java.awt.Dimension;

class CapSize extends Dimension {
	public CapSize() {
	}

	public CapSize(int nWidth, int nHeight) {
		super(nWidth, nHeight);
	}

	public CapSize(Dimension dim) {
		super(dim);
	}

	public boolean equals(Dimension dim) {
		boolean boolResult = true;

		if (dim == null)
			boolResult = false;
		if (boolResult == true)
			boolResult = this.width == dim.width;
		if (boolResult == true)
			boolResult = this.height == dim.height;
		return boolResult;
	}

	public String toString() {
		return "" + this.width + " x " + this.height;
	}
}