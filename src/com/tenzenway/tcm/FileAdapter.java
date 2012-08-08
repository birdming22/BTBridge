package com.tenzenway.tcm;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class FileAdapter {
	private FileOutputStream recFile = null;
	
	FileAdapter() {

	}
	
	public boolean newRecFile(long recId) {
		String filename = Constant.DROPBPX_PATH + String.valueOf(recId) + ".dat";
		
		System.out.println("filename:" + filename);
		
		try {
			if (recFile != null) {
				recFile.close();
			}
			recFile = new FileOutputStream(filename);
		} catch(Exception e) {
			System.out.println(e.getMessage());
			System.out.println("recId: " + recId);
			return false;
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

	public void close() {
		try {
			if (recFile != null) {
				recFile.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		recFile = null;
	}
}