#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(IoReactNativeCbor, NSObject)

RCT_EXTERN_METHOD(decode: (NSString)cbor
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(decodeDocuments: (NSString)mdoc
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(decodeIssuerSigned: (NSString)issuerSigned
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(sign: (NSString)payload
                  keyTag: (NSString*)keyTag
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(verify: (NSString)sign1Data
                  jwk: (NSDictionary*)jwk
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
