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

RCT_EXTERN_METHOD(sendResponse:(NSString *)response
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(sendErrorResponse:(NSInteger *)code
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(generateOID4VPDeviceResponse: (NSString *)clientId
                  withResponseUri: (NSString *)responseUri
                  withAuthorizationRequestNonce: (NSString *)authorizationRequestNonce
                  withMdocGeneratedNonce: (NSString *)mdocGeneratedNonce
                  withDocuments: (NSArray *)documents
                  withAcceptedFields: (NSDictionary *)acceptedFields
                  withResolver: (RCTPromiseResolveBlock)resolve
                  withRejecter: (RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup
{
  return YES;
}

@end
