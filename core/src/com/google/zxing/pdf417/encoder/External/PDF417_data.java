package com.google.zxing.pdf417.encoder.External;

/**
 * Author: Jacob Haynes (jacob.haynes@colorado.edu), Sergey Ushakov
 * (sergey@verify.kg) Date: 01.11.2004 Updated 06.06.2011
 */
public class PDF417_data {
	byte[] data;

	// Empty if no default bytes
	PDF417_data() {
		data = new byte[0];
	}

	PDF417_data(byte[] a) {
		data = a;
		// ensure not null;
		if (data == null)
			data = new byte[0];
	}
}
