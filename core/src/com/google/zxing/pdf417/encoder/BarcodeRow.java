package com.google.zxing.pdf417.encoder;

public class BarcodeRow {
	byte[] row;
	//A tacker for position in the bar
	int currentLocation;
	
	BarcodeRow(int width){
		this.row = new byte[width];	
		currentLocation = 0;
	}
		
	public void set(int x, byte value){
		row[x] = value;
	}
	
	public void set(int x, boolean black){
		row[x] = (byte) (black ? 1 : 0);
	}
	
	public void addBar(boolean black, int width){
		for (int ii = 0; ii < width; ii++){
			set(currentLocation++, black);
		}
	}
	
	public byte[] getRow(){
		return row;
	}
	
	public byte[] getScaledRow(int scale){
	   byte[] output = new byte[row.length*scale];
	   for(int ii = 0; ii < row.length*scale; ii++){
	       output[ii] = row[ii/scale];
	   }
	   return output;
	}
}
