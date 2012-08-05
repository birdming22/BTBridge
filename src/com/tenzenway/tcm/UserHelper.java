package com.tenzenway.tcm;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.Toast;

public class UserHelper {

	private static final String TAG = "UserHelper";
	private Activity activity;
	private SerialAdapter serialAdapter;
	private FileAdapter fileAdapter;
	private Spinner userSpinner;
	private DbAdapter dbAdapter;
	private long currentUserId = 0;
	private int currentPosition = 0;
	private long currentRecId = 0;
	private boolean isCapturing = false;
	private Button btnLeftUp;
	private Button btnLeftMiddle;
	private Button btnLeftDown;
	private Button btnRightUp;
	private Button btnRightMiddle;
	private Button btnRightDown;
	private Button btnTurnOff;
	private Button btnExport;

	public UserHelper(final Activity activity) {
		this.activity = activity;
		dbAdapter = new DbAdapter(activity);
		fileAdapter = new FileAdapter();
		_loadUser();
		_configButton();
	}

	private void _loadUser() {
		// get default user
		User user = dbAdapter.fetchUserByName("someone");
		// no default user someone, add it
		if (user == null) {
			currentUserId = dbAdapter.addUser("someone", 1977, 7, 7);
		} else {
			currentUserId = user.id;
		}

		// load users
		userSpinner = (Spinner) activity.findViewById(R.id.userSpinner);
		List<User> users = dbAdapter.fetchUserAll();
		ArrayList<String> strUsers = new ArrayList<String>();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
				android.R.layout.simple_spinner_item, strUsers);
		for (int i = 0; i < users.size(); i++) {
			adapter.add(users.get(i).username);
		}
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		userSpinner.setAdapter(adapter);
		userSpinner
				.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						// TODO Auto-generated method stub
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						// TODO Auto-generated method stub

					}
				});

	}
	
	private int _startCapturing(int newPosition) {
		if(isCapturing) {
			if (newPosition != currentPosition) {
				dbAdapter.endTransaction();
			} else {
				Toast.makeText(activity, "Already Capturing!", Toast.LENGTH_LONG).show();
				return 1;
			}
		} else {
			if (serialAdapter != null) {
				byte[] bytes = new byte[1];
				bytes[0] = 'I';
				serialAdapter.sendBytes(bytes);

				isCapturing = true;
			} else {
				Log.w(TAG, "no serial adapter!");
				return 1;
			}
		}
		
		currentPosition = newPosition;
		dbAdapter.beginTransaction();
		currentRecId = dbAdapter.addRecord(currentUserId, currentPosition);
		if(fileAdapter.newRecFile(currentRecId)) {
			Log.w(TAG, "rec file already exist");
		}

		return 0;
	}
	
	private void _stopCapturing() {
		isCapturing = false;
		byte[] bytes = new byte[1];
		bytes[0] = 'O';
		if (serialAdapter != null) {
			serialAdapter.sendBytes(bytes);
		}
		currentPosition = 0;
		currentRecId = 0;
		dbAdapter.endTransaction();
	}

	private void _configButton() {
		btnLeftUp = (Button) activity.findViewById(R.id.leftUp);
		btnLeftUp.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				_startCapturing(Constant.LEFT_UP);
			}
		});
		btnLeftMiddle = (Button) activity.findViewById(R.id.leftMiddle);
		btnLeftMiddle.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				_startCapturing(Constant.LEFT_MIDDLE);
			}
		});
		btnLeftDown = (Button) activity.findViewById(R.id.leftDown);
		btnLeftDown.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				_startCapturing(Constant.LEFT_DOWN);
			}
		});
		btnRightUp = (Button) activity.findViewById(R.id.rightUp);
		btnRightUp.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				_startCapturing(Constant.RIGHT_UP);
			}
		});
		btnRightMiddle = (Button) activity.findViewById(R.id.rightMiddle);
		btnRightMiddle.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				_startCapturing(Constant.RIGHT_MIDDLE);
			}
		});
		btnRightDown = (Button) activity.findViewById(R.id.rightDown);
		btnRightDown.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				_startCapturing(Constant.RIGHT_DOWN);
			}
		});
		
		btnTurnOff = (Button) activity.findViewById(R.id.turnOff);
		btnTurnOff.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				_stopCapturing();
			}
		});

		btnExport = (Button) activity.findViewById(R.id.export);
		btnExport.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				_stopCapturing();
				dbAdapter.exportDb();
			}
		});
	}
	
	void setSerialAdapter(SerialAdapter serialAdapter) {
		this.serialAdapter = serialAdapter;
	}

	void handleMessage(int[] sensorData) {
		for (int i=0; i< Constant.DATA_SIZE; i++) {
			dbAdapter.addData(currentRecId, sensorData[i]);
		}
	}
	
	void saveData(byte[] msg, int msgSize) {
		fileAdapter.write(msg, msgSize);
	}
}
