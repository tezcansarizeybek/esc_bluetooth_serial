import 'dart:async';

import 'package:esc_bluetooth_serial/esc_bluetooth_serial.dart';
import 'package:flutter/services.dart';
import 'package:rxdart/rxdart.dart';

class EscBluetoothSerial {
  static const MethodChannel _channel = MethodChannel('esc_bluetooth_serial');
  static const nameSpace = 'esc_bluetooth_serial';
  static const EventChannel _stateChannel = EventChannel('$nameSpace/state');
  final StreamController<MethodCall> _methodStreamController = StreamController.broadcast();
  Stream<MethodCall> get _methodStream => _methodStreamController.stream;
  static const int BLE_OFF = 10;
  static const int BLE_ON = 11;
  static const int DISCONNECTED = 0;
  static const int CONNECTED = 1;

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  Future<bool> get isConnected async => await _channel.invokeMethod('isConnected');

  final BehaviorSubject<bool> _isScanning = BehaviorSubject.seeded(false);
  Stream<bool> get isScanning => _isScanning.stream;

  final BehaviorSubject<List<BluetoothDevice>> _scanResults = BehaviorSubject.seeded([]);
  Stream<List<BluetoothDevice>> get scanResults => _scanResults.stream;

  final PublishSubject _stopScanPill = PublishSubject();

  /// Gets the current state of the Bluetooth module
  Stream<int> get state async* {
    yield await _channel.invokeMethod('state').then((s) => s);

    yield* _stateChannel.receiveBroadcastStream().map((s) => s);
  }

  Future stopScan() async {
    await _channel.invokeMethod('stopScan');
    _stopScanPill.add(null);
    _isScanning.add(false);
  }

  Future<List<BluetoothDevice>> getBondedDevices() async {
    final List list = await (_channel.invokeMethod('getBondedDevices'));
    return list.map((map) => BluetoothDevice.fromJson(map)).toList();
  }

  Future<dynamic> connect(BluetoothDevice device) => _channel.invokeMethod('connect', device.toJson());

  Future<dynamic> disconnect() => _channel.invokeMethod('disconnect');

  Future<dynamic> destroy() => _channel.invokeMethod('destroy');

  Future<dynamic> writeData(List<int> bytes) {
    Map<String, Object> args = {};
    args['bytes'] = bytes;
    args['length'] = bytes.length;

    _channel.invokeMethod('writeData', args);

    return Future.value(true);
  }

  EscBluetoothSerial._() {
    _channel.setMethodCallHandler((MethodCall call) async {
      _methodStreamController.add(call);
      return;
    });
  }

  static final EscBluetoothSerial _instance = EscBluetoothSerial._();

  static EscBluetoothSerial get instance => _instance;

  /// Starts a scan for Bluetooth Low Energy devices
  /// Timeout closes the stream after a specified [Duration]
  Stream<BluetoothDevice> scan({
    Duration timeout = const Duration(seconds: 5),
  }) async* {
    if (_isScanning.value == true) {
      throw Exception('Another scan is already in progress.');
    }

    // Emit to isScanning
    _isScanning.add(true);

    final killStreams = <Stream>[];
    killStreams.add(_stopScanPill);

    // Clear scan results list
    _scanResults.add(<BluetoothDevice>[]);

    try {
      await _channel.invokeMethod('startScan');
    } catch (e) {
      print('Error starting scan.');
      _stopScanPill.add(null);
      _isScanning.add(false);
      rethrow;
    }

    yield* EscBluetoothSerial.instance._methodStream
        .where((m) => m.method == "ScanResult")
        .map((m) => m.arguments)
        .takeUntil(Rx.merge(killStreams))
        .doOnDone(stopScan)
        .map((map) {
      final device = BluetoothDevice.fromJson(Map<String, dynamic>.from(map));
      final List<BluetoothDevice>? list = _scanResults.value;
      int newIndex = -1;
      (list ?? []).asMap().forEach((index, e) {
        if (e.address == device.address) {
          newIndex = index;
        }
      });

      if (newIndex != -1) {
        list![newIndex] = device;
      } else {
        (list ?? []).add(device);
      }
      _scanResults.add(list!);
      return device;
    });
  }
}
