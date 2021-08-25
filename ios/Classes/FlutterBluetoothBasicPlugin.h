#import <Flutter/Flutter.h>
#import <CoreBluetooth/CoreBluetooth.h>
#import "ConnecterManager.h"

#define NAMESPACE @"esc_bluetooth_serial"

@interface FlutterBluetoothBasicPlugin : NSObject<FlutterPlugin, CBCentralManagerDelegate, CBPeripheralDelegate>
@property(nonatomic,copy)ConnectDeviceState state;
@end

@interface BluetoothPrintStreamHandler : NSObject<FlutterStreamHandler>
@property FlutterEventSink sink;
@end
