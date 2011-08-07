
package com.google.zxing.pdf417.encoder.External;

/**
 * Author: Sergey Ushakov (sergey@verify.kg) Date: 02.11.2004 Time: 14:22:11
 */
class PDF417_eccCodewords {
    int[] data;

    public PDF417_eccCodewords() {
        data = new int[0];
    }

    public PDF417_eccCodewords(int[] datacw, PDF417_eccLevel level) {
        this.data = PDF417_utils.calculateEcc(datacw, level);
    }
}
