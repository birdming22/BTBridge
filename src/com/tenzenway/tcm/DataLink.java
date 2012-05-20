package com.tenzenway.tcm;

import android.os.Handler;

/**
 * Data Link is for protocol between android and arduino
 * 
 * @author birdming22
 */
public class DataLink {
	/* Data Format
	 * 
	 * -----------------------------------------------
	 * | 255 | SeqNum | Data1 | Data2 | ... | Data16 |
	 * -----------------------------------------------
	 */
	int state;
	int seqNum = 0;
	int dataCount =0;
	int frameSize = Constant.DATA_SIZE * 2;
	int[] buff = new int[frameSize];
	int[] sensorData = new int[Constant.DATA_SIZE];
	Handler _handler;
	
	DataLink(Handler handler) {
		_handler = handler;
		state = Constant.SYNC_STATE;
	}
	
	// process char of input message
	void _processMessage(int ch) {
		if (state == Constant.SYNC_STATE) {
			if (ch == -1) {
				state = Constant.SEQ_STATE;
			}
		} else if (state == Constant.SEQ_STATE) {
			seqNum = ch;
			state = Constant.DATA_STATE;
		} else if (state == Constant.DATA_STATE) {
			buff[dataCount] = ch;
			dataCount++;
			if (dataCount == frameSize) {
				state = Constant.SYNC_STATE;
				dataCount = 0;
				for (int i=0; i<Constant.DATA_SIZE; i++) {
					sensorData[i] = buff[i * 2 + 1] * 32 + buff[i * 2];
				}
				_handler.obtainMessage(0, Constant.DATA_SIZE, -1, sensorData)
				.sendToTarget();
			}
		}
	}
	
	// read input message from serial adapter
	void readMessage(byte[] msg, int msgSize) {
		for(int i=0; i<msgSize; i++) {
			int ch = (int) msg[i];
			_processMessage(ch);
		}
	}
}
