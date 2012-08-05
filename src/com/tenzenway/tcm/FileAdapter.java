package com.tenzenway.tcm;

import java.io.FileOutputStream;

public class FileAdapter {
	private FileOutputStream recFile = null;
	
	FileAdapter() {

	}
	
	public boolean newRecFile(long recId) {
		String filename = Constant.DROPBPX_PATH + String.valueOf(recId) + ".dat";
		try {
			recFile = new FileOutputStream(filename);
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
		
		return true;
	}
	
	public void write(byte[] msg, int msgSize) {
		try {
			if (recFile != null) {
				recFile.write(msg, 0, msgSize);
			}
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}

}