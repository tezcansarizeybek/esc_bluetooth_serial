//
//  ConnecterManager.h
//  GSDK
//

#import <Foundation/Foundation.h>
#import "BLEConnecter.h"
#import "EthernetConnecter.h"
#import "Connecter.h"

typedef enum : NSUInteger{
    BLUETOOTH,
    ETHERNET
}ConnectMethod;

#define Manager [ConnecterManager sharedInstance]

@interface ConnecterManager : NSObject
@property(nonatomic,strong)BLEConnecter *bleConnecter;
@property(nonatomic,strong)Connecter *connecter;

+(instancetype)sharedInstance;

-(void)close;

-(void)write:(NSData *_Nullable)data progress:(void(^_Nullable)(NSUInteger total,NSUInteger progress))progress receCallBack:(void (^_Nullable)(NSData *_Nullable))callBack;

-(void)write:(NSData *)data receCallBack:(void (^)(NSData *))callBack;

-(void)write:(NSData *)data;

-(void)stopScan;

-(void)didUpdateState:(void(^)(NSInteger state))state;

-(void)connectPeripheral:(CBPeripheral *)peripheral options:(nullable NSDictionary<NSString *,id> *)options timeout:(NSUInteger)timeout connectBlack:(void(^_Nullable)(ConnectState state)) connectState;

-(void)connectPeripheral:(CBPeripheral * _Nullable)peripheral options:(nullable NSDictionary<NSString *,id> *)options;

-(void)scanForPeripheralsWithServices:(nullable NSArray<CBUUID *> *)serviceUUIDs options:(nullable NSDictionary<NSString *, id> *)options discover:(void(^_Nullable)(CBPeripheral *_Nullable peripheral,NSDictionary<NSString *, id> *_Nullable advertisementData,NSNumber *_Nullable RSSI))discover;




@end
