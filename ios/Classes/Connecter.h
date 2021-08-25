//
//  Connecter.h
//  GSDK
//
#import <Foundation/Foundation.h>
#import "ConnecterBlock.h"

@interface Connecter:NSObject

@property(nonatomic,copy)ReadData readData;
@property(nonatomic,copy)ConnectDeviceState state;

-(void)connect;

-(void)connect:(void(^)(ConnectState state))connectState;

-(void)close;

-(void)write:(NSData *)data receCallBack:(void(^)(NSData *data))callBack;
-(void)write:(NSData *)data;

-(void)read:(void(^)(NSData *data))data;

@end
