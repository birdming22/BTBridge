package com.tenzenway.tcm;

public class Constant {
	public static final int PACKET_SIZE = 18;
	public static final int SAMPLE_RATE = 128;
	public static final int SENSING_LEVEL = 1024;
	public static final int DOMAIN_BOUNDARY = SAMPLE_RATE * 4;
	public static final int FFT_DATA_SIZE = 128;
	
	// for DataLink state machine
	public static final int SYNC_STATE = 0;
	public static final int SEQ_STATE = 1;
	public static final int DATA_STATE = 2;
	
	public static final int DATA_SIZE = 8;
	
	// for DbAdapter
	public static final String DB_NAME = "pressure";
	public static final int DB_VERSION = 1;
}
