#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(IoReactNativeCbor, NSObject)

RCT_EXTERN_METHOD(decode: (NSString *)data
                  withResolver: (RCTPromiseResolveBlock)resolve
                  withRejecter: (RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(decodeDocuments: (NSString *)data
                  withResolver: (RCTPromiseResolveBlock)resolve
                  withRejecter: (RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(decodeIssuerSigned: (NSString *)data
                  withResolver: (RCTPromiseResolveBlock)resolve
                  withRejecter: (RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(sign: (NSString *)data
                  withKeyTag: (NSString *)keyTag
                  withResolver: (RCTPromiseResolveBlock)resolve
                  withRejecter: (RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(verify: (NSString *)data
                  withJwk: (NSDictionary *)jwk
                  withResolver: (RCTPromiseResolveBlock)resolve
                  withRejecter: (RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
