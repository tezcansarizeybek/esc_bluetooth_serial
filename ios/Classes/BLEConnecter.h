//
//  Connecter.h
//  GSDK
//

#import "Connecter.h"
#import <CoreBluetooth/CoreBluetooth.h>

@interface BLEConnecter :Connecter

@property(nonatomic,strong)CBCharacteristic *airPatchChar;
@property(nonatomic,strong)CBCharacteristic *transparentDataWriteChar;
@property(nonatomic,strong)CBCharacteristic *transparentDataReadOrNotifyChar;
@property(nonatomic,strong)CBCharacteristic *connectionParameterChar;

@property(nonatomic,strong)CBUUID *transServiceUUID;
@property(nonatomic,strong)CBUUID *transTxUUID;
@property(nonatomic,strong)CBUUID *transRxUUID;
@property(nonatomic,strong)CBUUID *disUUID1;
@property(nonatomic,strong)CBUUID *disUUID2;
@property(nonatomic,strong)NSArray *serviceUUID;

@property(nonatomic,copy)DiscoverDevice discover;
@property(nonatomic,copy)UpdateState updateState;
@property(nonatomic,copy)WriteProgress writeProgress;

@property(nonatomic,assign)NSUInteger datagramSize;

@property(nonatomic,strong)CBPeripheral *connPeripheral;

- (void)configureTransparentServiceUUID: (NSString *)serviceUUID txUUID:(NSString *)txUUID rxUUID:(NSString *)rxUUID;

-(void)scanForPeripheralsWithServices:(nullable NSArray<CBUUID *> *)serviceUUIDs options:(nullable NSDictionary<NSString *, id> *)options discover:(void(^_Nullable)(CBPeripheral *_Nullable peripheral,NSDictionary<NSString *, id> *_Nullable advertisementData,NSNumber *_Nullable RSSI))discover;

-(void)stopScan;

-(void)didUpdateState:(void(^_Nullable)(NSInteger state))state;

-(void)connectPeripheral:(CBPeripheral *_Nullable)peripheral options:(nullable NSDictionary<NSString *,id> *)options timeout:(NSUInteger)timeout connectBlack:(void(^_Nullable)(ConnectState state)) connectState;

-(void)connectPeripheral:(CBPeripheral * _Nullable)peripheral options:(nullable NSDictionary<NSString *,id> *)options;

-(void)closePeripheral:(nonnull CBPeripheral *)peripheral;

-(void)write:(NSData *_Nullable)data progress:(void(^_Nullable)(NSUInteger total,NSUInteger progress))progress receCallBack:(void (^_Nullable)(NSData *_Nullable))callBack;

-(void)writeValue:(NSData *)data forCharacteristic:(nonnull CBCharacteristic *)characteristic type:(CBCharacteristicWriteType)type;
@end
