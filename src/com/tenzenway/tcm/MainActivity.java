package com.tenzenway.tcm;

import java.io.IOException;
import java.util.LinkedList;
import android.graphics.Color;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.tenzenway.tcm.MainView.ColValues;

import com.tenzenway.tcm.MainView.ColValues;
import com.tenzenway.tcm.MainView.JoystickListener;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main activity to control the hacked Spoka light.
 * 
 * @author trandi
 */
public class MainActivity extends Activity {
	// Debugging
	private static final String TAG = "SpokaLightBluetooth_MainActivity";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	private TextView _title;
	private BluetoothAdapter _bluetoothAdapter;
	private Spoka _spoka;
	private boolean _connectDevice = false;
	private EditText _debug;
	private String _inData = "";
	private SimpleXYSeries rollHistorySeries = null;
	private LinkedList<Number> rollHistory;
	private XYPlot aprHistoryPlot = null;
	private SimpleXYSeries rollFrequencySeries = null;
	private LinkedList<Number> rollFrequency;
	private XYPlot aprFrequencyPlot = null;
	private DoubleFFT_1D FFT = new DoubleFFT_1D(Constant.FFT_DATA_SIZE);
	private double[] fftArray = new double[Constant.FFT_DATA_SIZE * 2];
	private int dataCount = 0;
	private double[] dataArray = new double[Constant.FFT_DATA_SIZE];

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "+++ ON CREATE +++");

		// Set up the window layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);
		// register this activity with the View, to listen to Joystick events
		((MainView) findViewById(R.id.myMainView))
				.addJoystickListener(new JoystickListener() {
					@Override
					public void onMove(ColValues newColours) {
						if (_spoka != null) {
							final String result = _spoka.updateColours(
									newColours.red, newColours.blue,
									newColours.orange) ? "OK" : "NOK";

							Log.d(TAG, "Update Colours " + result + " ("
									+ newColours + ")");
						}
					}
				});

		// Set up the custom title
		_title = (TextView) findViewById(R.id.title_left_text);
		_title.setText(R.string.app_name);
		_title = (TextView) findViewById(R.id.title_right_text);

		// Get local Bluetooth adapter
		_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (_bluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		_debug = (EditText) findViewById(R.id.editDebug);
		_debug.setText("birdming");

		rollHistory = new LinkedList<Number>();
		rollHistorySeries = new SimpleXYSeries("Time");
		// setup the APR History plot:
		aprHistoryPlot = (XYPlot) findViewById(R.id.aprHistoryPlot);
		aprHistoryPlot.setRangeBoundaries(0, Constant.SENSING_LEVEL,
				BoundaryMode.FIXED);
		aprHistoryPlot.setDomainBoundaries(0, Constant.DOMAIN_BOUNDARY,
				BoundaryMode.FIXED);
		aprHistoryPlot.addSeries(rollHistorySeries, LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.rgb(200, 100, 100),
						Color.BLACK, null));
		aprHistoryPlot.setDomainStepValue(5);
		aprHistoryPlot.setTicksPerRangeLabel(3);
		aprHistoryPlot.setDomainLabel("Sample Index");
		aprHistoryPlot.getDomainLabelWidget().pack();
		aprHistoryPlot.setRangeLabel("Angle (Degs)");
		aprHistoryPlot.getRangeLabelWidget().pack();
		aprHistoryPlot.disableAllMarkup();

		rollFrequency = new LinkedList<Number>();
		rollFrequencySeries = new SimpleXYSeries("Frequency");
		// setup the APR History plot:
		aprFrequencyPlot = (XYPlot) findViewById(R.id.aprFrequencyPlot);
		aprFrequencyPlot.setRangeBoundaries(0, 512, BoundaryMode.FIXED);
		aprFrequencyPlot.setDomainBoundaries(0, Constant.FFT_DATA_SIZE,
				BoundaryMode.FIXED);
		aprFrequencyPlot.addSeries(rollFrequencySeries,
				LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.rgb(200, 100, 100),
						Color.BLACK, null));
		aprFrequencyPlot.setDomainStepValue(5);
		aprFrequencyPlot.setTicksPerRangeLabel(3);
		aprFrequencyPlot.setDomainLabel("Sample Index");
		aprFrequencyPlot.getDomainLabelWidget().pack();
		aprFrequencyPlot.setRangeLabel("Angle (Degs)");
		aprFrequencyPlot.getRangeLabelWidget().pack();
		aprFrequencyPlot.disableAllMarkup();

	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "++ ON START ++");

		if (!_bluetoothAdapter.isEnabled()) {
			Log.d(TAG, "BT is not on");

			// If BT is not on, request that it be enabled.
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			// Asynchronous, the onActivityResult will be called back when
			// finished
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			Log.d(TAG, "BT is on");
			// Bluetooth is already enabled
			// Launch the BTDeviceListActivity to see devices and do scan
			if (!_connectDevice) {
				Intent serverIntent = new Intent(this,
						BTDeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "++ ON DESTROY ++");

		if (_spoka != null)
			_spoka.disconnect();
	}
	public static final double[] forwardMagnitude(double[] input) {
		int N = input.length;
		double[] mag = new double[N];
		double[] c = new double[N];
		double[] s = new double[N];
		double twoPi = 2*Math.PI;
		
		for(int i=0; i<N; i++) {
			for(int j=0; j<N; j++) {
				c[i] += input[j]*Math.cos(i*j*twoPi/N);
				s[i] -= input[j]*Math.sin(i*j*twoPi/N);
			}
			c[i]/=N;
			s[i]/=N;
			
			mag[i]=Math.sqrt(c[i]*c[i]+s[i]*s[i]);
		}
		
		return mag;
	}
	private final Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				// System.out.println(dataCount);
				CharSequence readBuf = (CharSequence) msg.obj;
				_inData = "";
				for (int i = 0; i < msg.arg1; i++) {
					int in = readBuf.charAt(i);
					// System.out.println(in);
					_inData = _inData + ", " + Integer.toString(in);
				}

				if (rollHistory.size() > Constant.DOMAIN_BOUNDARY) {
					for (int i = 0; i < 8; i++)
						rollHistory.removeFirst();
				}
				for (int i = 1; i < 9; i++) {
					int b1 = (int) readBuf.charAt(i * 2);
					int b2 = (int) readBuf.charAt(i * 2 + 1);
					int value = b2 * 32 + b1;
					rollHistory.addLast(value);
					fftArray[dataCount + i - 1] = (double)value;
					dataArray[dataCount + i - 1] = (double)value;
				}
				dataCount += 8;
				rollHistorySeries.setModel(rollHistory,
						SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
				aprHistoryPlot.redraw();

				if (dataCount == Constant.FFT_DATA_SIZE) {
					dataCount = 0;

//					double samplingInterval = 128;// (double) (sampleRate /
													// frequencyOfSignal);
					for (int i = 0; i < Constant.FFT_DATA_SIZE; i++) {
						double angle = (2.0 * Math.PI * i) / 8;
//						 fftArray[i] = (double) (Math.cos(angle) * 512);
//						 dataArray[i] = (double) (Math.cos(angle) * 512);
//						 System.out.println("" + fftArray[i]);
					}
					System.out.println("before fft");
//					FFT.complexForward(fftArray);
//					FFT.realForwardFull(fftArray);
					
					double[] magArray = forwardMagnitude(dataArray);
					int size = rollFrequency.size();
					for (int i = 0; i < size; i++) {
						rollFrequency.removeFirst();
					}
					for(int i=0;i<8;i++)
						System.out.println("" + fftArray[i]);
//						System.out.println("" + magArray[i]);
					for (int i = 0; i < Constant.FFT_DATA_SIZE; i++) {
//						System.out.println("" + fftArray[i]);
//						rollFrequency.addLast(Math.sqrt((fftArray[i * 2]
//								* fftArray[i * 2] + fftArray[i * 2 + 1]
//								* fftArray[i * 2 + 1])));
						 rollFrequency.addLast(magArray[i]);
//							System.out.println("" + magArray[i]);
//						 rollFrequency.addLast(Math.abs(fftArray[i*2]));
					}
					rollFrequencySeries.setModel(rollFrequency,
							SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
					aprFrequencyPlot.redraw();
					for(int i=0; i< fftArray.length; i++)
						fftArray[i]=0;
				}
				break;
			default:
				System.out.println("msg.what not 0!");
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + resultCode);

		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When BTDeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						BTDeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Create a business object which attempts to create to the
				// Spoka !
				// ensureDiscoverable(_bluetoothAdapter);
				try {
					Log.d(TAG, "address:" + address);

					_spoka = new Spoka(_bluetoothAdapter, address, _handler);
					_connectDevice = true;
				} catch (Exception e) {
					Toast.makeText(this, "Can't connect to the SPOKA",
							Toast.LENGTH_SHORT).show();
					finish();
				}
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled
				// Launch the BTDeviceListActivity to see devices and do scan
				Intent serverIntent = new Intent(this,
						BTDeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this,
						"User did not enable Bluetooth or an error occured",
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	private void ensureDiscoverable(BluetoothAdapter bluetoothAdapter) {
		if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Log.d(TAG, "Force discoverable");
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}
}
