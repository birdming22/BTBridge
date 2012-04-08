package com.tenzenway.tcm;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class BTDeviceListActivity extends Activity {
 // Debugging
 private static final String TAG = "BTDeviceListActivity";

 // Return Intent extra
 public static String EXTRA_DEVICE_ADDRESS = "device_address";

 // Member fields
 private BluetoothAdapter _btAdapter;
 private ArrayAdapter<String> _pairedDevicesArrayAdapter;
 private ArrayAdapter<String> _newDevicesArrayAdapter;

 // The BroadcastReceiver that listens for discovered devices and changes the title when discovery is finished
 private final BroadcastReceiver _receiver = new BroadcastReceiver() {
 @Override
 public void onReceive(Context context, Intent intent) {
 String action = intent.getAction();

// When discovery finds a device
 if (BluetoothDevice.ACTION_FOUND.equals(action)) {
 // Get the BluetoothDevice object from the Intent
 BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
 // If it's already paired, skip it, because it's been listed already
 if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
 _newDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
 }
 // When discovery is finished, change the Activity title
 } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
 setProgressBarIndeterminateVisibility(false);
 setTitle(R.string.select_device);
 if (_newDevicesArrayAdapter.getCount() == 0) {
 _newDevicesArrayAdapter.add(getResources().getText(R.string.none_found).toString());
 }
 }
 }
 };

 // The on-click listener for all devices in the ListViews
 private OnItemClickListener _deviceClickListener = new OnItemClickListener() {
 @Override
 public void onItemClick(AdapterView av, View v, int arg2, long arg3) {
	 Log.e(TAG, "onItemClick");
 // Cancel discovery because it's costly and we're about to connect
 _btAdapter.cancelDiscovery();

// Get the device MAC address, which is the last 17 chars in the View
 String info = ((TextView) v).getText().toString();
 String address = info.substring(info.length() - 17);

// Create the result Intent and include the MAC address
 Intent intent = new Intent();
 intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

// Set result and finish this Activity
 setResult(Activity.RESULT_OK, intent);
 finish();
 }
 };

 @Override
 protected void onCreate(Bundle savedInstanceState) {
 super.onCreate(savedInstanceState);
 Log.e(TAG, "onCreate");
// Setup the window
 requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
 setContentView(R.layout.device_list);

// Set result CANCELED incase the user backs out
 setResult(Activity.RESULT_CANCELED);

// Initialize the button to perform device discovery
 Button scanButton = (Button) findViewById(R.id.button_scan);
 scanButton.setOnClickListener(new OnClickListener() {
 public void onClick(View v) {
 doDiscovery();
 v.setVisibility(View.GONE);
 Log.e(TAG, "onClick");
 }
 });

// Initialize array adapters. One for already paired devices and one for newly discovered devices
 _pairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
 _newDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

// Find and set up the ListView for paired devices
 ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
 pairedListView.setAdapter(_pairedDevicesArrayAdapter);
 pairedListView.setOnItemClickListener(_deviceClickListener);

// Find and set up the ListView for newly discovered devices
 ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
 newDevicesListView.setAdapter(_newDevicesArrayAdapter);
 newDevicesListView.setOnItemClickListener(_deviceClickListener);

// Register for broadcasts when a device is discovered & discovery has finished
 this.registerReceiver(_receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
 this.registerReceiver(_receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

// Get the local Bluetooth adapter
 _btAdapter = BluetoothAdapter.getDefaultAdapter();

// Get a set of currently paired devices
 Set<BluetoothDevice> pairedDevices = _btAdapter.getBondedDevices();

// If there are paired devices, add each one to the ArrayAdapter
 if (pairedDevices.size() > 0) {
 findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
 for (BluetoothDevice device : pairedDevices) {
 _pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
 }
 } else {
 _pairedDevicesArrayAdapter.add(getResources().getText(R.string.none_paired).toString());
 }
 }

 @Override
 protected void onDestroy() {
 super.onDestroy();

// Make sure we're not doing discovery anymore
 if (_btAdapter != null) {
 _btAdapter.cancelDiscovery();
 }

// Unregister broadcast listeners
 this.unregisterReceiver(_receiver);
 }

 /**
 * Start device discover with the BluetoothAdapter
 */
 private void doDiscovery() {
 Log.d(TAG, "doDiscovery()");

// Indicate scanning in the title
 setProgressBarIndeterminateVisibility(true);
 setTitle(R.string.scanning);

// Turn on sub-title for new devices
 findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

// If we're already discovering, stop it
 if (_btAdapter.isDiscovering()) {
 _btAdapter.cancelDiscovery();
 }

// Request discover from BluetoothAdapter
 _btAdapter.startDiscovery();
 }
}


