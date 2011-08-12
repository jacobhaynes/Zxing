/*
 * Copyright 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.pdf417.encoder;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.pdf417.encoder.PDF417;

/**
 * @author Jacob Haynes (jacobmhaynes@gmail.com)
 */
public final class PDF417Encoder {

    /**
     * This takes an array holding the values of the PDF 417
     * 
     * @param input a byte array of information with 0 is black, and 1 is white
     * @return BitMatrix of the input
     */
    private static BitMatrix bitMatrixFrombitArray(byte[][] input) {
        //Creates a small whitespace boarder around the barcode
        int whiteSpace = 30;
       
        //Creates the bitmatrix with extra space for whtespace
        BitMatrix output = new BitMatrix(input.length + 2 * whiteSpace, input[0].length + 2 * whiteSpace);
        output.clear();
        for (int ii = 0; ii < input.length; ii++) {
            for (int jj = 0; jj < input[0].length; jj++) {
                // Zero is white in the bytematrix
                if (input[ii][jj] == 1)
                    output.set(ii + whiteSpace, jj + whiteSpace);
            }
        }
        return output;
    }
    
    public static BitMatrix encode(String content, int Height, int Width) {
        PDF417 encoder = new PDF417();
        int lineThickness = 3;
        int aspectRatio = 8;
        int errorCorrectionLevel = 3;
        boolean rotated = false;
        
        //Used to find the best ratio automatically
        if(Height*aspectRatio < Width){
            encoder.preferredRatio = 1;
        } else { 
          encoder.preferredRatio = 1;   
        }
        
        //No error correction at the moment
        encoder.generateBarcodeLogic(content, errorCorrectionLevel);
        
        // Give it data to be encoded
        //encoderExt.setData(content.getBytes());
        // Find the Error correction level automatically

        //encoderExt.encode();
        //encoderExt.createArray();
        byte[][] originalScale = encoder.barcodeMatrix.getScaledMatrix(lineThickness, 
                aspectRatio*lineThickness);
        if ((Height > Width) ^ (originalScale[0].length < originalScale.length)) {
            originalScale = rotateArray(originalScale);
            rotated = true;
        }

        int scaleX = Width / originalScale[0].length;
        int scaleY = Height / originalScale.length;

        int scale = 1;
        if (scaleX < scaleY) {
            scale = scaleX;
        } else {
            scale = scaleY;
        }

        if (scale > 1) {
            byte[][] scaledMatrix = encoder.barcodeMatrix.getScaledMatrix(scale*lineThickness, 
                   scale*aspectRatio*lineThickness);
            if (rotated){
                return bitMatrixFrombitArray(rotateArray(scaledMatrix));
            } else {
                return bitMatrixFrombitArray(scaledMatrix);
            }
        } else {
            return bitMatrixFrombitArray(originalScale);
        }
    }
    
    /** Takes and rotates the it 90 degrees */
    private static byte[][] rotateArray(byte[][] bitarray) {
        byte[][] temp = new byte[bitarray[0].length][bitarray.length];
        for (int ii = 0; ii < bitarray.length; ii++) {
            // This makes the direction consistent on screen when rotating the
            // screen;
            int inverseii = bitarray.length - ii - 1;
            for (int jj = 0; jj < bitarray[0].length; jj++) {
                temp[jj][inverseii] = bitarray[ii][jj];
            }
        }
        return temp;
    }

    private PDF417Encoder() {
    }

}
