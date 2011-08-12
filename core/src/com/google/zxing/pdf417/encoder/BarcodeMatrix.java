package com.google.zxing.pdf417.encoder;

/**
 * Author: Jacob Haynes (jacobmhaynes@gmail.com)
 * 
 * Holds all of the information for a barcode in a format where it can be easily accessable
 */
public class BarcodeMatrix {
	BarcodeRow[] matrix;
	int currentColumn;
	int currentRow;
	int height;
	int width;
	
	/**
	 * 
	 * @param height the height of the matrix (Rows)
	 * @param width the width of the matrix (Cols)
	 */
	BarcodeMatrix(int height, int width){
		matrix = new BarcodeRow[height+2];
		//Initializes 
		for (BarcodeRow singleRow : matrix){
			singleRow = new BarcodeRow((width+4)*17+1);
		}
		this.width = width*17;
		this.height = height+2;
		
		this.currentColumn = 0;
		this.currentRow = 0;
	}
		
	public void set(int x, int y, byte value){
		matrix[y].set(x, value);
	}
	
	public void setMatrix(int x, int y, boolean black){
		set(x, y, (byte) (black ? 1 : 0));
	}
	
	public void startRow() {
		++currentRow;
	}
	
	public BarcodeRow getCurrentRow() {
		return matrix[currentRow];
	}
	
	public void startBarGroup(){
		//nothing
	}
	public void endBarGroup(){
		//nothing
	}
	
	public byte[][] getMatrix(){
		return getScaledMatrix(1,1);
	}
	
	public byte[][] getScaledMatrix(int Scale){
	  return getScaledMatrix(Scale, Scale);   
	}
	
	public byte[][] getScaledMatrix(int xScale, int yScale){
	      byte[][] matrixOut = new byte[height*yScale][width*xScale];
	      int yMax = height*yScale;
	        for (int ii = 0; ii < yMax; ii++){
	            matrixOut[yMax - ii - 1] = matrix[ii/yScale].getScaledRow(xScale);
	        }
	       return matrixOut;
	}
}
