package com.tezcansarizeybek.esc_bluetooth_serial;

import androidx.annotation.NonNull;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class EscBluetoothSerialPlugin implements FlutterPlugin, MethodCallHandler, RequestPermissionsResultListener {
  private MethodChannel channel;
  private final Activity activity = new Activity();
  private static final String TAG = "BluetoothBasicPlugin";
  private final int id = 0;
  private ThreadPool threadPool;
  private static final int REQUEST_COARSE_LOCATION_PERMISSIONS = 1451;

  private Result pendingResult;


  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (mBluetoothAdapter == null && !"isAvailable".equals(call.method)) {
      result.error("bluetooth_unavailable", "Bluetooth is unavailable", null);
      return;
    }

    final Map<String, Object> args = call.arguments();

    switch (call.method){
      case "state":
        state(result);
        break;
      case "isAvailable":
        result.success(mBluetoothAdapter != null);
        break;
      case "isOn":
        result.success(mBluetoothAdapter.isEnabled());
        break;
      case "isConnected":
        result.success(threadPool != null);
        break;
      case "startScan": {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(
                  activity,
                  new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                  REQUEST_COARSE_LOCATION_PERMISSIONS);
          pendingResult = result;
          break;
        }
        startScan(result);
        break;
      }
      case "getBondedDevices":
        getDevices(result);
        break;
      case "stopScan":
        stopScan();
        result.success(null);
        break;
      case "connect":
        connect(result, args);
        break;
      case "disconnect":
        result.success(disconnect());
        break;
      case "destroy":
        result.success(destroy());
        break;
      case "writeData":
        writeData(result, args);
        break;
      default:
        result.notImplemented();
        break;
    }

  }

  private void getDevices(Result result){
    List<Map<String, Object>> devices = new ArrayList<>();
    for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
      Map<String, Object> ret = new HashMap<>();
      ret.put("address", device.getAddress());
      ret.put("name", device.getName());
      ret.put("type", device.getType());
      devices.add(ret);
    }

    result.success(devices);
  }

  private void state(Result result){
    try {
      switch(mBluetoothAdapter.getState()) {
        case BluetoothAdapter.STATE_OFF:
          result.success(BluetoothAdapter.STATE_OFF);
          break;
        case BluetoothAdapter.STATE_ON:
          result.success(BluetoothAdapter.STATE_ON);
          break;
        case BluetoothAdapter.STATE_TURNING_OFF:
          result.success(BluetoothAdapter.STATE_TURNING_OFF);
          break;
        case BluetoothAdapter.STATE_TURNING_ON:
          result.success(BluetoothAdapter.STATE_TURNING_ON);
          break;
        default:
          result.success(0);
          break;
      }
    } catch (SecurityException e) {
      result.error("invalid_argument", "Argument 'address' not found", null);
    }

  }

  private void startScan(Result result) {
    Log.d(TAG,"start scan ");

    try {
      startScan();
      result.success(null);
    } catch (Exception e) {
      result.error("startScan", e.getMessage(), null);
    }
  }

  private void invokeMethodUIThread(final BluetoothDevice device) {
    final Map<String, Object> ret = new HashMap<>();
    ret.put("address", device.getAddress());
    ret.put("name", device.getName());
    ret.put("type", device.getType());

    activity.runOnUiThread(
            () -> channel.invokeMethod("ScanResult", ret));
  }

  private final ScanCallback mScanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      BluetoothDevice device = result.getDevice();
      if(device != null && device.getName() != null){
        invokeMethodUIThread(device);
      }
    }
  };

  private void startScan() throws IllegalStateException {
    BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
    if(scanner == null) throw new IllegalStateException("getBluetoothLeScanner() is null. Is the Adapter on?");

    // 0:lowPower 1:balanced 2:lowLatency -1:opportunistic
    ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
    scanner.startScan(null, settings, mScanCallback);
  }

  private void stopScan() {
    BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
    if(scanner != null) scanner.stopScan(mScanCallback);
  }

  private void connect(Result result, Map<String, Object> args) {
    if (args.containsKey("address")) {
      String address = (String) args.get("address");
      disconnect();

      new DeviceConnFactoryManager.Build()
              .setId(id)
              // Set the connection method
              .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.BLUETOOTH)
              // Set the connected Bluetooth mac address
              .setMacAddress(address)
              .build();
      // Open port
      threadPool = ThreadPool.getInstantiation();
      threadPool.addSerialTask(() -> DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort());

      result.success(true);
    } else {
      result.error("invalid_argument", "Argument 'address' not found", null);
    }

  }

  /**
   * Reconnect to recycle the last connected object to avoid memory leaks
   */
  private boolean disconnect(){

    if(DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id]!=null&&DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort!=null) {
      DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].reader.cancel();
      DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort.closePort();
      DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort=null;
    }
    return true;
  }

  private boolean destroy() {
    DeviceConnFactoryManager.closeAllPort();
    if (threadPool != null) {
      threadPool.stopThreadPool();
    }

    return true;
  }

  @SuppressWarnings("unchecked")
  private void writeData(Result result, Map<String, Object> args) {
    if (args.containsKey("bytes")) {
      final ArrayList<Integer> bytes = (ArrayList<Integer>)args.get("bytes");

      threadPool = ThreadPool.getInstantiation();
      threadPool.addSerialTask(() -> {
        Vector<Byte> vectorData = new Vector<>();
        for(int i = 0; i < (bytes != null ? bytes.size() : 0); ++i) {
          Integer val = bytes.get(i);
          vectorData.add(Byte.valueOf( Integer.toString(val > 127 ? val-256 : val ) ));
        }

        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(vectorData);
      });
    } else {
      result.error("bytes_empty", "Bytes param is empty", null);
    }
  }

  @Override
  public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

    if (requestCode == REQUEST_COARSE_LOCATION_PERMISSIONS) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        startScan(pendingResult);
      } else {
        pendingResult.error("no_permissions", "This app requires location permissions for scanning", null);
        pendingResult = null;
      }
      return true;
    }
    return false;

  }


  private BluetoothAdapter mBluetoothAdapter;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "esc_bluetooth_serial");
    BluetoothManager mBluetoothManager = (BluetoothManager) flutterPluginBinding.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
    this.mBluetoothAdapter = mBluetoothManager.getAdapter();
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {

  }


}
