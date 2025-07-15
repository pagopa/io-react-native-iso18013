#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(IoReactNativeIso18013, RCTEventEmitter)

RCT_EXTERN_METHOD(start:(NSArray *)certificates
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getQrCodeString:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(close:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(generateResponse:(NSArray *)documents
                 withAcceptedFields:(NSDictionary *)acceptedFields
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(sendResponse:(NSString)response
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(sendErrorResponse:(NSInteger)status
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(generateOID4VPDeviceResponse: (NSString)clientId
                  responseUri: (NSString*)responseUri
                  authorizationRequestNonce: (NSString*)authorizationRequestNonce
                  mdocGeneratedNonce: (NSString*)mdocGeneratedNonce
                  documents: (NSArray*)documents
                  fieldRequestedAndAccepted: (NSString*)fieldRequestedAndAccepted
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup
{
  return YES;
}

@end
