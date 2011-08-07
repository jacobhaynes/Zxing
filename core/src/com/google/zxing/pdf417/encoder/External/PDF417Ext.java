package com.google.zxing.pdf417.encoder.External;

/**
 * The class which is used to produce PDF417 barcodes, it can produce a JPEG,
 * PNG or return a bitmap array.
 * 
 * Author: Jacob Haynes (jacob.haynes@colorado.edu), Sergey Ushakov
 * (sergey@verify.kg) Date: 01.11.2004 (Ushakov) Updated: 08.05.2011 (Haynes)
 */
public class PDF417Ext {
	/** The data in bytes */
	private PDF417_data data;
	/** The number of rows/columns which the PDF417 has */
	private PDF417_size size;
	private PDF417_eccCodewords eccCW;
	private boolean truncated = false;
	/**
	 * The aspect ratio of the resulting image (larger this number, the more
	 * vertically stretched it is
	 */
	private int aspectRatio = 3;
	/** Stores PDF417 data */
	private byte[][][] pattern;
	protected PDF417_eccLevel eccLevel;
	protected PDF417_dataCodewords dataCW;
	protected int[] codewords;

	/** A 2-D array which holds a bitmap of the barcode */
	byte[][] bitarray;

	public PDF417Ext() {
		data = new PDF417_data();
		size = new PDF417_size();
		dataCW = new PDF417_dataCodewords();
		eccLevel = new PDF417_eccLevel();
		eccCW = new PDF417_eccCodewords();
	}
	
	/** Gets a ByteArray of the PDF417 */
	public void createArray() {
		// Ensure the barcode was encoded
		if (pattern == null) {
			encode();
		}
		byte BLACK = 0;
		byte WHITE = 1;

		int w, h, l, cx, cy;
		l = pattern[0].length;
		h = (pattern.length + 4) * aspectRatio;
		w = l * 17;
		if (pattern[0][l - 1].length == 9)
			w++;
		w += 4;

		bitarray = new byte[w + 1][h + 1];
		fillRect(bitarray, 0, 0, w + 1, h + 1, WHITE);

		for (int i = 0; i < pattern.length; i++) {
			byte[][] bytes = pattern[i];
			cx = 2;
			cy = (i + 2) * aspectRatio;
			for (int j = 0; j < bytes.length; j++) {
				byte[] aByte = bytes[j];
				for (int k = 0; k < aByte.length; k++) {
					byte b = aByte[k];
					byte color;
					if (k % 2 == 0)
						color = BLACK;
					else
						color = WHITE;
					fillRect(bitarray, cx, cy, cx + b, cy + aspectRatio, color);
					cx += b;
				}
			}
			fillRect(bitarray, cx, cy, w, cy + aspectRatio, WHITE);
		}
		fillRect(bitarray, 0, (pattern.length + 2) * aspectRatio, w, h, WHITE);
	}

	public void encode() {
		/*
		 * Encode procedure is simple as A, B, C, D ..... :) A) High Level Encoding
		 * 1. Convert data to codewords using Binary Encode Mode. 2. Determine
		 * codeword count. 3. Determine ECC Level, using that codeword count. (if
		 * auto detection) 4. Determine symbol size. 5. Make padding codewords, then
		 * put lenght of dataCodewords before first codeword. 6. Make ECC
		 * calculation 7. Fill out codeword sequence.
		 * 
		 * B) Low level Encoding 1. Initialize pattern array 2. Fill out start
		 * patterns, stop patterns, left and right row indicator. 3. Fill out
		 * codewords.
		 */
		highLevelEncode();
		lowLevelEncode();
	}

	/** Fills a rectangle in a byte array */
	private void fillRect(byte[][] byteArray, int startX, int startY, int endX,
	    int endY, byte color) {
		for (int ii = startX; ii < endX; ii++) {
			for (int jj = startY; jj < endY; jj++) {
				byteArray[ii][jj] = color;
			}
		}
	}

	public byte[][] getArray() {
		return bitarray;
	}

	public byte[][] getScaleArray(int ammount) {
		if (ammount > 1) {
			byte[][] output = new byte[bitarray.length * ammount][bitarray[0].length
			    * ammount];
			for (int ii = 0; ii < bitarray.length; ii++) {
				for (int jj = 0; jj < bitarray[0].length; jj++) {
					byte colorByte = bitarray[ii][jj];
					fillRect(output, ii * ammount, jj * ammount, (ii + 1) * ammount,
					    (jj + 1) * ammount, colorByte);
				}
			}
			return output;
		} else {
			return getArray();
		}
	}

	/**
	 * High Level Encoding 
	 * 1. Convert data to codewords using Binary Encode Mode.
	 * 2. Determine codeword count. 
	 * 3. Determine ECC Level, using that codeword count. (if auto detection) 
	 * 4. Determine symbol size. 
	 * 5. Make padding codewords, then put lenght of dataCodewords before first codeword. 
	 * 6. Make ECC calculation 7. Fill out codeword sequence.
	 */
	private void highLevelEncode() {
		// A.1 convert codewords to Binary
		dataCW.data = PDF417_utils.binaryModeEncode(data.data);

		// A.2 determine codeword count
		int len = dataCW.data.length + 1;

		// A.3 determine ECC level
		eccLevel.detect(len);

		// A.4 make sure the size is large enough, if not increase it.
		try {
			size.detect(len, eccLevel.getEccLevel());
		} catch (PDF417EncodeException e) {
			size = new PDF417_size(size.getHeight() + 1, size.getWidth() + 1);
			encode();
			return;
		}

		// A.5
		codewords = new int[size.getHeight() * size.getWidth()];
		int[] datawords = new int[codewords.length - eccLevel.getEccLenght()];
		datawords[0] = datawords.length;
		System.arraycopy(dataCW.data, 0, datawords, 1, dataCW.data.length);
		for (int i = dataCW.data.length + 1; i < datawords.length; i++)
			datawords[i] = PDF417_utils.reminderByteCompactLatch;
		dataCW.data = datawords;

		// A.6 calculate error correction
		eccCW = new PDF417_eccCodewords(dataCW.data, eccLevel);

		// A.7
		System.arraycopy(dataCW.data, 0, codewords, 0, dataCW.data.length);
		System.arraycopy(eccCW.data, 0, codewords, dataCW.data.length,
		    eccCW.data.length);
	}

	/**
	 * Low level Encoding 
	 * 1. Initialize pattern array 
	 * 2. Fill out start patterns, stop patterns, left and right row indicator. 
	 * 3. Fill out codewords.
	 */
	private void lowLevelEncode() {
		int pw, ph, i, j;
		ph = size.getHeight();
		pw = size.getWidth();

		if (truncated)
			pattern = new byte[ph][pw + 2][];
		else
			pattern = new byte[ph][pw + 4][];

		for (i = 0; i < ph; i++) {
			pattern[i][0] = PDF417_utils.startPattern;
			pattern[i][1] = PDF417_utils.getPattern(i, lri(i));
			if (!truncated) {
				pattern[i][pw + 3] = PDF417_utils.stopPattern;
				pattern[i][pw + 2] = PDF417_utils.getPattern(i, rri(i));
			}
		}
		int l = 0;
		for (i = 0; i < ph; i++)
			for (j = 2; j < pw + 2; j++)
				pattern[i][j] = PDF417_utils.getPattern(i, codewords[l++]);

		if (l != codewords.length)
			throw new PDF417EncodeException("Internal error.");

	}

	protected int lri(int row) {
		int c = row % 3;
		switch (c) {
			case 0:
				return 30 * (row / 3) + ((size.getHeight() - 1) / 3);
			case 1:
				return 30 * (row / 3) + eccLevel.getEccLevel() * 3
				    + ((size.getHeight() - 1) % 3);
			case 2:
				return 30 * (row / 3) + (size.getWidth() - 1);
		}
		return -1;
	}

	protected int rri(int row) {
		int c = row % 3;
		switch (c) {
			case 0:
				return 30 * (row / 3) + (size.getWidth() - 1);
			case 1:
				return 30 * (row / 3) + ((size.getHeight() - 1) / 3);
			case 2:
				return 30 * (row / 3) + eccLevel.getEccLevel() * 3
				    + ((size.getHeight() - 1) % 3);
		}
		throw new PDF417EncodeException(
		    "Assertion failed while calculating right-row indicator.");
	}

	/** Set data for PDF417 to encode */
	public void setData(byte[] data) {
		this.data = new PDF417_data(data);
	}

	/** Set data for PDF417 to encode */
	public void setData(PDF417_data data) {
		this.data = data;
	}

	/** Sets the Error Correction level for encoding */
	public void setEccLevel(PDF417_eccLevel eccLevel) {
		this.eccLevel = eccLevel;
	}
	
	public void setEccLevelAutomatic(){
	    this.eccLevel = new PDF417_eccLevel();
	    this.eccLevel.setEccLevel(dataCW.data.length);
	}

	/** Changes the number or rows/columns of data the PDF417 has */
	public void setSize(PDF417_size size) {
		this.size = size;
	}
	/** Changes the height and width of the barcode
	 * It determines the number of rows/columns
	 * This size will be increased if there is too much data */
	public void setSize(int height, int width){
		this.size = new PDF417_size(height, width);
	}

	public void setTruncated(boolean truncated) {
		this.truncated = truncated;
	}
	
    public byte[][] getFlippedArray() {
        rotateArray();
        return bitarray;
    }
    
    private void rotateArray(){
      byte[][] temp = new byte[bitarray[0].length][bitarray.length];
      for(int ii = 0; ii < bitarray.length; ii++){
          //This makes the direction consistent on screen when rotating the screen;
          int inverseii = bitarray.length - ii -1;
          for(int jj =0; jj < bitarray[0].length; jj++){
              temp[jj][inverseii] = bitarray[ii][jj];
          }
      }
      bitarray = temp;
    }
}
