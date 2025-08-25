import IOWalletCBOR
import IOWalletProximity
import CryptoKit

@objc(IoReactNativeCbor)
class IoReactNativeCbor: NSObject {
  private let keyConfig: KeyConfig = .ec
  
  /**
   Decode base64 or base64url encoded CBOR data to JSON object.
   Resolves with a string containing the parsed data or rejects with an error code defined in ``ModuleErrorCodes``.
   This method does not handle nested CBOR data, which will need additional parsing.
   - Parameters:
      - cbor: The base64 or base64url encoded CBOR string.
      - resolve: The promise to be resolved.
      - reject: The promise to be rejected.
   */
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
  
  /**
   Decode base64 or base64url encoded mDOC-CBOR data to a JSON object.
   Resolves with a string containing the parsed data or rejects with an error code defined in ``ModuleErrorCodes``.
   - Parameters:
      - mdoc: The base64 or base64url encoded mDOC-CBOR string.
      - resolve: The promise to be resolved.
      - reject: The promise to be rejected.
   */
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
  
  /**
   Decode base64 or base64url encoded issuerSigned attribute part of an mDOC-CBOR.
   Resolves with a string containing the parsed data or rejects with an error code defined in ``ModuleErrorCodes``.
   - Parameters:
      - issuerSigned: The base64 or base64url encoded mDOC-CBOR containing the issuerSigned data string.
      - resolve: The promise to be resolved.
      - reject: The promise to be rejected.
   */
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
  
  /**
   Sign base64 encoded data with COSE and return the COSE-Sign1 object in base64 encoding.
   Resolves with a string containing the COSE-Sign1 object in base64 encoding or rejects with an error code defined in ``ModuleErrorCodes``.
   - Parameters:
     - payloadData: The base64 or base64url encoded payload to sign.
     - keyTag: The alias of the key to use for signing.
     - resolve: The promise to be resolved.
     - reject: The promise to be rejected.
   */
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
  
  /**
   Verifies a COSE-Sign1 object with the provided public key.
   Resolves with boolean indicating whether or not the verification succeeded or not or rejects with an error code defined in ``ModuleErrorCodes``.
   - Parameters:
      - sign1Data: The COSE-Sign1 object in base64 or base64url encoding.
      - publicKey: The public key in JWK format.
      - resolve: The promise to be resolved
      - reject: The promise to be rejected..
   */
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
    
  private func keyExists(keyTag: String) -> (key: SecKey?, status: OSStatus) {
    let getQuery = privateKeyKeychainQuery(keyTag: keyTag)
    var item: CFTypeRef?
    let status = SecItemCopyMatching(getQuery as CFDictionary, &item)
    return (status == errSecSuccess ? (item as! SecKey) : nil, status)
  }
  
  private func privateKeyKeychainQuery(
    keyTag: String
  ) -> [String : Any] {
    return [
      kSecClass as String: kSecClassKey,
      kSecAttrApplicationTag as String: keyTag,
      kSecAttrKeyType as String: keyConfig.keyType(),
      kSecReturnRef as String: true
    ]
  }
  
  private enum KeyConfig: Int, CaseIterable {
    case ec
    
    func keyType() -> CFString {
      switch self {
      case .ec:
        return kSecAttrKeyTypeECSECPrimeRandom
      }
    }
    
    func keySizeInBits() -> Int {
      switch self {
      case .ec:
        return 256
      }
    }
    
    func keySignAlgorithm() -> SecKeyAlgorithm {
      switch self {
      case .ec:
        return .ecdsaSignatureMessageX962SHA256
      }
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
