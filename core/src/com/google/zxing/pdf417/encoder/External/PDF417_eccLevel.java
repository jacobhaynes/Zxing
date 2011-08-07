package com.google.zxing.pdf417.encoder.External;

/**
 * Author: Sergey Ushakov (sergey@verify.kg) Date: 02.11.2004 Time: 13:05:57
 */
class PDF417_eccLevel {
	private int eccLevel;
	private boolean auto;

	PDF417_eccLevel() {
		auto = true;
	}

	public PDF417_eccLevel(int eccLevel) {
		this.eccLevel = eccLevel;
		auto = false;
	}

	int detect(int dataCodewords) {
		eccLevel = internalDetect(dataCodewords);
		return eccLevel;
	}

	public int getEccLenght() {
		return 2 << eccLevel;
	}

	public int getEccLevel() {
		return eccLevel;
	}
	
	public void setEccLevel(int dataCodeworks) {
	    eccLevel = internalDetect(dataCodeworks);
	}

	private int internalDetect(int dataCodewords) {
		// if auto detection is not needed - don't do it
		if (!auto)
			return eccLevel;

		// See Symbol recomendations about the ECC Level
		if (dataCodewords <= 40)
			return 2;
		if (dataCodewords <= 160)
			return 3;
		if (dataCodewords <= 320)
			return 4;
		if (dataCodewords <= 863)
			return 5;

		// We had encountered very large symbol let's give it the maximum ECC Level
		// the symbol can fit.
		int remind = 927 - dataCodewords;
		if (remind >= 32)
			return 4;
		if (remind >= 16)
			return 3;
		if (remind >= 8)
			return 2;
		if (remind >= 4)
			return 1;

		// Oversize case will be handled later, in symbol encoding procedure.
		return 0;
	}
}
