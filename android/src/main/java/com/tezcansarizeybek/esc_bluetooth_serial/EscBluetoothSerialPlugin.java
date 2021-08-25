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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/** EscBluetoothSerialPlugin */
public class EscBluetoothSerialPlugin implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private ThreadPool threadPool;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "esc_bluetooth_serial");
    channel.setMethodCallHandler(this);
  }


  private int id = 0;
  private BluetoothAdapter mBluetoothAdapter;

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    final Map<String, Object> args = call.arguments();
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    }
    else if(call.method.equals("connect")){
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
        threadPool.addSerialTask(new Runnable() {
          @Override
          public void run() {
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
          }
        });

        result.success(true);
      } else {
        result.error("invalid_argument", "Argument 'address' not found", null);
      }

    }
    else if(call.method.equals("disconnect")){
      result.success(disconnect());
    }
    else if(call.method.equals("destroy")){
      result.success(destroy());
    }
    else if(call.method.equals("writeData")){
      writeData(result, args);
    }
    else if(call.method.equals("getBondedDevices")){
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
    else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

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
    return true;
  }


  @SuppressWarnings("unchecked")
  private void writeData(Result result, Map<String, Object> args) {
    if (args.containsKey("bytes")) {
      final ArrayList<Integer> bytes = (ArrayList<Integer>)args.get("bytes");

      threadPool = ThreadPool.getInstantiation();
      threadPool.addSerialTask(() -> {
        Vector<Byte> vectorData = new Vector<>();
        for(int i = 0; i < bytes.size(); ++i) {
          Integer val = bytes.get(i);
          vectorData.add(Byte.valueOf( Integer.toString(val > 127 ? val-256 : val ) ));
        }

        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(vectorData);
      });
    } else {
      result.error("bytes_empty", "Bytes param is empty", null);
    }
  }
}
