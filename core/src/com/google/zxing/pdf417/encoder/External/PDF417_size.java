package com.google.zxing.pdf417.encoder.External;

/**
 * Author: Sergey Ushakov (sergey@verify.kg) Date: 01.11.2004 Time: 19:25:30
 */
public class PDF417_size {
	private int height;
	private int width;

	private int sourceHeight;
	private int sourceWidth;

	// zero value means autodetection of that parameter.
	public PDF417_size() {
		this(0, 0);
	}

	public PDF417_size(int aHeight, int aWidth) {
		sourceHeight = height = aHeight;
		sourceWidth = width = aWidth;
	}

	protected void detect(int dataLength, int eccLevel) {
		int len = dataLength + (2 << eccLevel);
		/*
		 * Autodetection is simple math task: minimizing expression ((4+w) * 17 + 1
		 * )* h
		 * 
		 * under certain conditions w * h <= len; 3 <= h <= 90 1 <= w <= 30
		 * 
		 * the first thing came to mind is brute-force.
		 * 
		 * todo: write complete (brute-force) test for testing this method. It shall
		 * help in optimizing this method.
		 * 
		 * By the way: lookup table seems good solution, since it need only 928
		 * values, less then 4 kb.
		 */
		int c, w, h;
		int mc, mw, mh;
		mc = (int) 10e7; // ((4+30) * 17 + 1 )* 90 + 1;
		mw = 0;
		mh = 0;
		int minW = 1;
		int maxW = 30;
		int minH = 3;
		int maxH = 90;

		if (sourceHeight != 0)
			minH = maxH = sourceHeight;
		if (sourceWidth != 0)
			minW = maxW = sourceWidth;
		for (w = minW; w <= maxW; w++)
			for (h = minH; h <= maxH; h++) {
				if (w * h < len)
					continue;
				if (w * h > 928)
					break;
				c = (((4 + w) * 17 + 1) * h) * 5 + w * 300;
				if (c <= mc) {
					mc = c;
					mh = h;
					mw = w;
				}
				;
			}
		if (mw * mh < len)
			throw new PDF417EncodeException("Desirable barcode size too small.");
		width = mw;
		height = mh;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}
}
