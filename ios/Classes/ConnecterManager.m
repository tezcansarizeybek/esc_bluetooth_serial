//
//  ConnecterManager.m
//  GSDK
//
//

#import "ConnecterManager.h"

@interface ConnecterManager(){
    ConnectMethod currentConnMethod;
}
@end

@implementation ConnecterManager

static ConnecterManager *manager;
static dispatch_once_t once;

+(instancetype)sharedInstance {
    dispatch_once(&once, ^{
        manager = [[ConnecterManager alloc]init];
    });
    return manager;
}

-(void)scanForPeripheralsWithServices:(nullable NSArray<CBUUID *> *)serviceUUIDs options:(nullable NSDictionary<NSString *, id> *)options discover:(void(^_Nullable)(CBPeripheral *_Nullable peripheral,NSDictionary<NSString *, id> *_Nullable advertisementData,NSNumber *_Nullable RSSI))discover{
    [_bleConnecter scanForPeripheralsWithServices:serviceUUIDs options:options discover:discover];
}

-(void)didUpdateState:(void(^)(NSInteger state))state {
    if (_bleConnecter == nil) {
        currentConnMethod = BLUETOOTH;
        [self initConnecter:currentConnMethod];
    }
    [_bleConnecter didUpdateState:state];
}

-(void)initConnecter:(ConnectMethod)connectMethod {
    switch (connectMethod) {
        case BLUETOOTH:
            _bleConnecter = [BLEConnecter new];
            _connecter = _bleConnecter;
            break;
        default:
            break;
    }
}

-(void)stopScan {
    [_bleConnecter stopScan];
}

-(void)connectPeripheral:(CBPeripheral *)peripheral options:(nullable NSDictionary<NSString *,id> *)options timeout:(NSUInteger)timeout connectBlack:(void(^_Nullable)(ConnectState state)) connectState{
    [_bleConnecter connectPeripheral:peripheral options:options timeout:timeout connectBlack:connectState];
}

-(void)connectPeripheral:(CBPeripheral * _Nullable)peripheral options:(nullable NSDictionary<NSString *,id> *)options {
    [_bleConnecter connectPeripheral:peripheral options:options];
}

-(void)write:(NSData *_Nullable)data progress:(void(^_Nullable)(NSUInteger total,NSUInteger progress))progress receCallBack:(void (^_Nullable)(NSData *_Nullable))callBack {
    [_bleConnecter write:data progress:progress receCallBack:callBack];
}

-(void)write:(NSData *)data receCallBack:(void (^)(NSData *))callBack {
#ifdef DEBUG
    NSLog(@"[ConnecterManager] write:receCallBack:");
#endif
    _bleConnecter.writeProgress = nil;
    [_connecter write:data receCallBack:callBack];
}

-(void)write:(NSData *)data {
#ifdef DEBUG
    NSLog(@"[ConnecterManager] write:");
#endif
    _bleConnecter.writeProgress = nil;
    [_connecter write:data];
}

-(void)close {
    if (_connecter) {
        [_connecter close];
    }
    switch (currentConnMethod) {
        case BLUETOOTH:
            _bleConnecter = nil;
            break;
    }
}

@end
