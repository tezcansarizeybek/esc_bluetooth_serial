
import 'dart:async';

import 'package:flutter/services.dart';

class EscBluetoothSerial {
  static const MethodChannel _channel = MethodChannel('esc_bluetooth_serial');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
