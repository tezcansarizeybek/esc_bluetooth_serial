package com.tezcansarizeybek.esc_bluetooth_serial;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** EscBluetoothSerialPlugin */
public class EscBluetoothSerialPlugin implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "esc_bluetooth_serial");
    channel.setMethodCallHandler(this);
  }


  private int id = 0;
  private ThreadPool threadPool;

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
        threadPool = ThreadPool.getInstantiation();
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

    }
    else if(call.method.equals("destroy")){

    }
    else if(call.method.equals("writeData")){

    }
    else if(call.method.equals("getBondedDevices")){

    }
    else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }
}
