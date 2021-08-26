import 'package:flutter/material.dart';
import 'dart:async';

import 'package:esc_bluetooth_serial/esc_bluetooth_serial.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  List _devices = [];

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  Future<void> initPlatformState() async {
    List devices;
    devices = await EscBluetoothSerial.instance.getBondedDevices();

    if (!mounted) return;

    setState(() {
      _devices = devices.map((e) => e.toJson()).toList();
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('$_devices\n'),
        ),
      ),
    );
  }
}
