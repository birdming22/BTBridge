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

public class SerialAdapter {
	// Debugging
	private static final String TAG = "SERIAL_ADAPTER";

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
	private Handler _handler;

	// will need to SYNCHRONISE on this as it's used from multiple threads !
	private Queue<String> _incomingData = new ConcurrentLinkedQueue<String>();
	private StringBuffer _incomingDataStr = new StringBuffer();
	private char _sequence = 0;

	public SerialAdapter(BluetoothAdapter bluetoothAdapter, String address,
			Handler handler) throws IOException {
		Log.d(TAG, "++ CONNECT SERIAL ADAPTER ++");

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
			Log.d(TAG, "++ connectED SERIAL ADAPTER ++");
		} catch (IOException e) {
			disconnect();
			Log.e(TAG, "Can't connect to the SERIAL ADAPTER", e);
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

							_handler.obtainMessage(0, readCount, -1, buff)
							.sendToTarget();
						}
					} catch (Exception e) {
						Log.e(TAG, "Can't read message from the SERIAL ADAPTER", e);
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

	public void sendBytes(byte[] bytes) {
		try {
			if (_socketOS == null)
				_socketOS = new BufferedOutputStream(
						_btSocket.getOutputStream(), 1024);

			if (_socketOS != null) {
				_socketOS.write(bytes);
				_socketOS.flush();
			}
		} catch (IOException e) {
			Log.e(TAG, "Can't send message to the SERIAL ADAPTER", e);
		}
	}
}
