package com.tenzenway.tcm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

public class Spoka {
	// Debugging
	private static final String TAG = "SPOKA";

	// Unique UUID for this application
	// has to be this precise value for SERIAL port profile (SPP)
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// private static final UUID MY_UUID_SECURE =
	// UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
	// private static final UUID MY_UUID_INSECURE =
	// UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

	private BluetoothSocket _btSocket;
	private InputStream _socketIS;
	private OutputStream _socketOS;
	private final Handler _handler;

	// will need to SYNCHRONISE on this as it's used from multiple threads !
	private Queue<String> _incomingData = new ConcurrentLinkedQueue<String>();
	private StringBuffer _incomingDataStr = new StringBuffer();
	private char _sequence = 0;

	public Spoka(BluetoothAdapter bluetoothAdapter, String address,
			Handler handler) throws IOException {
		Log.d(TAG, "++ CONNECT SPOKA ++");

		_handler = handler;

		// Get the BluetoothDevice object
		BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
		try {
			bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
					address, MY_UUID);
		} catch (IOException e) {
			Log.e(TAG, "listen() failed", e);
		}
		// Get a BluetoothSocket for a connection with the given BluetoothDevice
		try {
			_btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
		} catch (IOException e) {
			Log.e(TAG, "createRfcommSocketToServiceRecord() failed", e);
			throw e;
		}

		// Attempt to connect to the device
		// Always cancel discovery because it will slow down a connection
		bluetoothAdapter.cancelDiscovery();

		// Make a connection to the BluetoothSocket
		try {
			// This is a blocking call and will only return on a successful
			// connection or an exception
			_btSocket.connect();
			// _btSocket.accept();
			Log.d(TAG, "++ connectED SPOKA ++");
		} catch (IOException e) {
			disconnect();
			Log.e(TAG, "Can't connect to the Spoka", e);
			throw e;
		}

		// !!! COMMENTED OUT THE WHOLE INCOMING SERIAL DATA READ, AS THIS SEEMS
		// TO CAUSE THE RECEIVED BYTES TO BE GARBLED !!!
		// start a separate thread that monitors any incoming data, and stores
		// it in a queue
		// this is done so, because we don't seem to have an asynchronous read()
		// on the BluetoothSocket InputStream
		new Thread() {
			@Override
			public void run() {
				byte[] buff = new byte[1024];
				while (true) {
					try {
						if (_socketIS == null) {
							// _socketIS = new BufferedInputStream(
							// _btSocket.getInputStream(), 1024);
							_socketIS = _btSocket.getInputStream();
						} else {
							// blocking on the read when there's nothing...
							int readCount = _socketIS.read(buff);

							_incomingDataStr.append(new String(buff, 0,
									readCount));
							if (_incomingDataStr.length() > Constant.PACKET_SIZE * 2) {
								int syncByteIdx = 0;
								for (int i = 0; i < Constant.PACKET_SIZE * 2; i++) {
									if (_incomingDataStr.charAt(i) > 128) {
										syncByteIdx = i;
										break;
									}
								}
								if (syncByteIdx != 0)
									System.out.println("packet is not sync!");
								CharSequence tmp = _incomingDataStr
										.subSequence(syncByteIdx, syncByteIdx
												+ Constant.PACKET_SIZE);
//								for (int i = 0; i < tmp.length(); i++) {
//									int j = tmp.charAt(i);
//									System.out.println(j);
//								}
								_incomingDataStr.delete(0, syncByteIdx
										+ Constant.PACKET_SIZE);
								_handler.obtainMessage(0, Constant.PACKET_SIZE, -1, tmp)
										.sendToTarget();
							}
						}
					} catch (Exception e) {
						Log.e(TAG, "Can't read message from the Spoka", e);
					}
				}
			}
		}.start();
	}

	public void disconnect() {
		try {
			if (_btSocket != null)
				_btSocket.close();
		} catch (IOException e) {
			Log.e(TAG, "Unable to close() socket during connection failure", e);
		}
	}

	public boolean updateColours(int redPerc, int bluePerc, int orangePerc) {
		if (redPerc > bluePerc) {
			sendBytes((byte) 'I');
		} else {
			sendBytes((byte) 'O');
		}
		/*
		 * byte red = checkPerc(redPerc); byte blue = checkPerc(bluePerc); byte
		 * orange = checkPerc(orangePerc);
		 * 
		 * // 1. send the special char "#" indicating to the Spoka that we want
		 * to // manually update the colours sendBytes((byte) '#');
		 * 
		 * // 2. check that we receive back the "BRO " acknowledgement( Blue,
		 * Red, // Orange) // if(waitForIncomingData("BRO", 100)){ // 3. send
		 * the values & the check sum sendBytes(blue, red, orange, (byte)
		 * (((int) blue + (int) red + (int) orange) % 100));
		 * 
		 * // 4. check that we receive back confirmation // return
		 * waitForIncomingData("OK", 100); // }
		 */
		return false;
	}

	private byte checkPerc(int perc) {
		return (byte) (perc < 0 ? 0 : (perc > 100 ? 100 : perc));
	}

	private void sendBytes(byte... bytes) {
		try {
			if (_socketOS == null)
				_socketOS = new BufferedOutputStream(
						_btSocket.getOutputStream(), 1024);

			if (_socketOS != null) {
				_socketOS.write(bytes);
				_socketOS.flush();
			}
		} catch (IOException e) {
			Log.e(TAG, "Can't send message to the Spoka", e);
		}
	}

	private boolean waitForIncomingData(String expectedData, int timeoutMillis) {
		final long startTime = System.currentTimeMillis();
		byte[] buff = new byte[100];
		String incomingData = "";
		try {
			if (_socketIS == null)
				_socketIS = new BufferedInputStream(_btSocket.getInputStream(),
						1024);

			if (_socketIS != null) {
				while ((!incomingData.contains(expectedData))
						&& (System.currentTimeMillis() - startTime < timeoutMillis)) {
					// blocking on the read when there's nothing...
					int readCount = _socketIS.read(buff);
					Log.e(TAG, "readCount:" + readCount);
					// for some VERY SILLY reason, if we store the bytes here,
					// they come garbled...
					incomingData += new String(buff, 0, readCount);
					Log.e(TAG, "incomingData" + incomingData);
				}
			}
		} catch (IOException e) {
			Log.e(TAG, "Can't receive incoming data", e);
		}

		Log.d(TAG, "RECV: " + incomingData);

		if (incomingData.contains(expectedData)) {
			return true;
		}

		return false;
	}

	// private boolean waitForIncomingData(String expectedData, int
	// timeoutMillis){
	// final long startTime = System.currentTimeMillis();
	//
	// while((! _incomingDataStr.contains(expectedData))
	// && (System.currentTimeMillis() - startTime < timeoutMillis))
	// {
	// final String head = _incomingData.poll();
	// if(head != null) _incomingDataStr += head;
	// Thread.yield();
	// }
	//
	// if(_incomingDataStr.contains(expectedData)){
	// // REMOVE the expected data (and whatever was before) from the string
	// buffer
	// final int pos = _incomingDataStr.indexOf(expectedData) +
	// expectedData.length();
	// _incomingDataStr = _incomingDataStr.substring(pos);
	//
	// return true;
	// }
	//
	// Log.d(TAG, "RECV: " + _incomingDataStr);
	// return false;
	// }

	// /**
	// * @return true if the expected incoming data has been received, false if
	// timeout
	// */
	// private boolean waitForIncomingData(String incomingData, int
	// timeoutMillis){
	// final long startTime = System.currentTimeMillis();
	// // wait until received expected data or timeout
	// while(! peekIncomingData().contains(incomingData) &&
	// (System.currentTimeMillis() - startTime < timeoutMillis)){
	// try {
	// Thread.sleep(10);
	// } catch (InterruptedException e) {
	// // nothing to do
	// }
	// }
	//
	// final int position = peekIncomingData().indexOf(incomingData);
	// if(position > 0){
	// // now remove the data from the incoming queue
	// for(int i=0; i < position + incomingData.length(); i++){
	// _incomingData.poll();
	// }
	// return true; // found the expected string
	// }else{
	// Log.e(TAG, peekIncomingData());
	// }
	//
	// return false; // NOT found the expected string and timed out
	// }
	//
	// private String peekIncomingData() {
	// Byte[] data = _incomingData.toArray(new Byte[]{});
	// byte[] dataChars = new byte[data.length];
	// for(int i=0; i
	// dataChars[i] = data[i];
	// }
	// return new String(dataChars);
	// }
}
