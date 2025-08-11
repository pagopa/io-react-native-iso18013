import IOWalletCBOR
import IOWalletProximity
import CryptoKit

@objc(IoReactNativeCbor)
class IoReactNativeCbor: NSObject {
  
  @objc func decode(
    _ cbor: String,
    resolver resolve: RCTPromiseResolveBlock,
    rejecter reject: RCTPromiseRejectBlock
  ) {
    do{
      let data = try Base64Utils.decodeBase64OrBase64URL(base: cbor)
      guard let json = CborCose.jsonFromCBOR(data: data) else {
        // We don't have the exact error here as this method returns nil upon failure
        reject(ModuleErrorCodes.decodeError.rawValue, "Unable to decode CBOR", nil)
        return
      }
      resolve(json);
      
    }catch{
      reject(ModuleErrorCodes.decodeError.rawValue, error.localizedDescription, error)
    }
  }
  
  @objc func decodeDocuments(
    _ mdoc: String,
    resolver resolve: RCTPromiseResolveBlock,
    rejecter reject: RCTPromiseRejectBlock
  ) {
    do{
      let data = try Base64Utils.decodeBase64OrBase64URL(base: mdoc)
      guard let json = CborCose.decodeCBOR(data: data, true, true) else {
        // We don't have the exact error here as this method returns nil upon failure
        reject(ModuleErrorCodes.decodeDocumentsError.rawValue, "Unable to decode document CBOR", nil)
        return
      }
      resolve(json);
    }catch{
      reject(ModuleErrorCodes.decodeDocumentsError.rawValue, error.localizedDescription, error)
    }
  }
  
  @objc func decodeIssuerSigned(
    _ issuerSigned: String,
    resolver resolve: RCTPromiseResolveBlock,
    rejecter reject: RCTPromiseRejectBlock
  ) {
    do{
      let data = try Base64Utils.decodeBase64OrBase64URL(base: issuerSigned)
      guard let json = CborCose.issuerSignedCborToJson(data: data) else {
        // We don't have the exact error here as this method returns nil upon failure
        reject(ModuleErrorCodes.decodeIssuerSignedError.rawValue, "Unable to decode Issuer Signed CBOR", nil)
        return
      }
      resolve(json);
    }catch{
      reject(ModuleErrorCodes.decodeIssuerSignedError.rawValue, error.localizedDescription, error)
    }
  }
  
  @objc func sign(
    _ payloadData: String,
    keyTag: String,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
      DispatchQueue.global().async { [weak self] in
        guard self != nil else {
          reject(ModuleErrorCodes.threadingError.rawValue, "Failed to perform background operation, self was deallocated", nil)
          return
        }
        
        do{
          let data = try Base64Utils.decodeBase64OrBase64URL(base: payloadData)
          guard let coseKey = CoseKeyPrivate(crv: .p256, keyTag: keyTag) else {
            // We don't have the exact error here as this method returns nil upon failure
            reject(ModuleErrorCodes.signError.rawValue, "Unable to create a private key with the given keytag: \(keyTag)", nil)
            return
          }
          let signedPayload = CborCose.sign(data: data, privateKey: coseKey)
          resolve(signedPayload.base64EncodedString())
      }
      catch{
        reject(ModuleErrorCodes.signError.rawValue, error.localizedDescription, error)
      }
    }
  }
  
  @objc func verify(
    _ sign1Data: String,
    jwk: NSDictionary,
    resolver resolve: RCTPromiseResolveBlock,
    rejecter reject: RCTPromiseRejectBlock
  ) {
    do {
      let data = try Base64Utils.decodeBase64OrBase64URL(base: sign1Data)
      let publicKeyJson = try JSONSerialization.data(withJSONObject: jwk, options:[] )
      let publicKeyString = String(data: publicKeyJson, encoding: .utf8)!
      let publicKey = CoseKey(jwk: publicKeyString)!
      let verified = CborCose.verify(data: data, publicKey: publicKey)
      resolve(verified)
    } catch {
      reject(ModuleErrorCodes.verifyError.rawValue, error.localizedDescription, error)
    }
  }
  
  // Errors which this module uses to reject a promise
  private enum ModuleErrorCodes: String, CaseIterable {
    case decodeError = "DECODE_ERROR"
    case decodeDocumentsError = "DECODE_DOCUMENTS_ERROR"
    case decodeIssuerSignedError = "DECODE_ISSUER_SIGNED_ERROR"
    case signError = "SIGN_ERROR"
    case verifyError = "VERIFY_ERROR"
    case threadingError = "THREADING_ERROR"
  }
}
