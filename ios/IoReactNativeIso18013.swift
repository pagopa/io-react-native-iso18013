import Foundation
import IOWalletProximity
import React

@objc(IoReactNativeIso18013)
class IoReactNativeProximity: RCTEventEmitter {
  
  private typealias ME = ModuleException
  
  @objc
  override static func requiresMainQueueSetup() -> Bool {
    return true
  }
  
  override init() {
    super.init()
    setupProximityHandler()
  }
  
  /**
   Specifies supported events which will be emitted.
   - `onDeviceConnecting`: Emitted when the device is connecting to the verifier app.
   - `onDeviceConnected`: Emitted when the device is connected to the verifier app.
   - `onDocumentRequestReceived`: Emitted when a document request is received from the verifier app. Carries a payload containing the request data.
   - `onDeviceDisconnected`: Emitted when the device is disconnected from the verifier app.
   - `onError`: Emitted when an error occurs. Carries a payload containing the error data.
   */
  override func supportedEvents() -> [String]! {
    return ["onDeviceConnected", "onDeviceConnecting", "onDeviceDisconnected", "onDocumentRequestReceived", "onError", "unknown"]
  }
  
  /**
   Type alias for the accepted fields during the presentation. These are the fields which the user accepted to share.
   It can be fed to the ``IOWalletProximity.generateResponse`` function.
   An example might be:
   `["org.iso.18013.5.1.mDL": ["org.iso.18013.5.1": ["hair_colour": true, "given_name_national_character": true, "family_name_national_character": true, "given_name": true]]]`
   */
  typealias AcceptedFieldsDict = [String: [String: [String: Bool]]]
  
  /**
   Starts the proximity flow by allocating the necessary resources and initializing the Bluetooth stack.
   Resolves to true or rejects if an error occurs.
    
   - Parameters:
      - certificates: Array of base64 representing DER encoded X.509 certificate which are used to authenticate the verifier app
      - resolve: The promise to be resolved
      - reject: The promise to be rejected
  */
  @objc(start:withResolver:withRejecter:)
  func start(
    certificates: Array<Any>,
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ){
    do {
      let certsData = parseCertificates(certificates)
      try Proximity.shared.start(certsData.isEmpty ? nil : certsData)
      resolve(true)
    } catch let error {
      ME.startError.reject(reject: reject, ("error", error.localizedDescription))
    }
  }
  
  /**
   Utility function to parse an array coming from the React Native Bridge into an array of Data representing DER encoded X.509 certificates.
   
   - Parameters:
      - certificates:Array of base64 strings representing DER encoded X.509 certificate
   
    - Returns: An array of Data containing DER ecnoded X.509 certificates.
  */
  private func parseCertificates(_ certificates: [Any]) -> [Data] {
    return certificates.compactMap { item in
      guard let certString = item as? String,
            let data = Data(base64Encoded: certString) else {
        return nil
      }
      return data
    }
  }
  
  /**
   Creates a QR code to be scanned in order to initialize the presentation.
   Resolves with the QR code strings.

   - Parameters:
      - resolve: The promise to be resolved
      - reject: The promise to be rejected
  */
  @objc(getQrCodeString:withRejecter:)
  func getQrCodeString(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    do{
      let qrCodeString = try Proximity.shared.getQrCode()
      resolve(qrCodeString)
    } catch let error {
      ME.qrCodeError.reject(reject: reject, ("error", error.localizedDescription))
    }
  }
  
  /**
   Parses an array of documents from the React Native bridge which doesn't have any typing to an array of ``IOWalletProximity.ProximityDocument``.
   It checks if each element in the array has  `issuerSignedContent`, `alias` and `docType` properties in order to build a ``IOWalletProximity.ProximityDocument``, then it appens it to the array.
   The result can be fed to ``IOWalletProximity.generateResponse``.
   
   - Parameters:
      - documents: An array of any elements. In order to be added to the result array each element must be a dictionary with `issuerSignedContent` encoded in base64 or base64url, `alias` and `docType` as keys and strings as values.
   
   - Throws: `ModuleException.invalidDocRequested.error()` and `ModuleException.invalidDocRequested.error()` iif an error occurs while parsing the document
   
   - Returns: An array of `ProximityDocument` containg the documents to be presented
   */
  private func parseDocuments(documents: [Any]) throws -> [ProximityDocument] {
    return try documents.compactMap{ (element) -> ProximityDocument in
      guard let dict : [String: Any] = element as? [String: Any] else { throw ModuleException.unableToDecode.error() }
      guard
        let issuerSignedContent = dict["issuerSignedContent"] as? String,
        let alias = dict["alias"] as? String,
        let docType = dict["docType"] as? String,
        let decodedIssuerSignedContent = try? Base64Utils.decodeBase64OrBase64URL(base: issuerSignedContent)
      else { throw ModuleException.invalidDocRequested.error() }
      
      guard let document = ProximityDocument(
        docType: docType,
        issuerSigned: [UInt8](decodedIssuerSignedContent),
        deviceKeyTag: alias
      ) else {
        throw ModuleException.invalidDocRequested.error()
      }
      return document
    }
  }
  
  /**
   Parses a dictionary of accepted fields for the presentation from the React Native bridge which doesn't have any typing to a ``AcceptedFieldsDict`` dictionary.
   It checks if each element in the array is a dictionary where the key is a string, and the value is another dictionary. This nested dictionary has a string as its key and a boolean as its value, then it appens it to the array.
   The result can be fed to ``IOWalletProximity.generateResponse``.
   
   - Parameters:
      - acceptedFields: A dictionary of any elements. In order to be added to the result dictionary each element must be shaped as ``AcceptedFieldsDict`` thus as [String: [String: [String: Bool]]]
   
   - Throws: `NSError` if a value doesn't has the ``AcceptedFieldsDict`` shape or the result dictionary is empty
   
   - Returns: An ``AcceptedFieldsDict`` containg the accepted fields to be presented
   
   */
  private func parseAcceptedFields(acceptedFields: [AnyHashable: Any]) throws -> AcceptedFieldsDict {
    var result: AcceptedFieldsDict = [:]
    
    for (key, value) in acceptedFields {
      guard let keyString = key as? String else {
        throw NSError(domain: "ParseAcceptedFields", code: -1, userInfo: [NSLocalizedDescriptionKey: "Key must be a String."])
      }
      guard let valueDict = value as? [String: [String: Bool]] else {
        throw NSError(domain: "ParseAcceptedFields", code: -1, userInfo: [NSLocalizedDescriptionKey: "Value must be of type [String: [String: Bool]]."])
      }
      result[keyString] = valueDict
    }
    
    if(result.isEmpty){
      throw NSError(domain: "ParseAcceptedFields", code: -1, userInfo: [NSLocalizedDescriptionKey: "Error parsing documents"])
    }
    
    return result
  }
  
  /**
     Generates a response containing the documents and the fields which the user decided to present.
     It parses the untyped ``documents`` and ``acceptedFields`` parameters and feeds them to the ``IOWalletProximity.generateDeviceResponse`` function.
     It resolves the promise with the response as a base64 encoded string.
     It rejects the promise if an error occurs during the parameters parsing or while generating the device response.
     
     - Parameters:
       - documents: An array of documents which should contain a dictionary with `issuerSignedContent`, `alias` and `docType` as keys and strings as values
       - acceptedFields: A dictionary of elements, where each element must adhere to the structure of AcceptedFieldsDict—specifically, a [String: [String: [String: Bool]]]. The outermost key represents the credentia doctypel. The inner dictionary contains namespaces, and for each namespace, there is another dictionary mapping requested claims to a boolean value, which indicates whether the user is willing to present the corresponding claim. Example:
        
         
             {
                "org.iso.18013.5.1.mDL": {
                  "org.iso.18013.5.1": {
                    "hair_colour": true,
                    "given_name_national_character": true,
                    "family_name_national_character": true,
                    "given_name": true,
                  }
                }
             }
    
       - resolve: The promise to be resolved
       - reject: The promise to be rejected
     
     */
  @objc(generateResponse:withAcceptedFields:withResolver:withRejecter:)
  func generateResponse(
    documents: Array<Any>,
    acceptedFields: [AnyHashable: Any],
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ){
    do {
      let parsedDocuments = try parseDocuments(documents: documents)
      let items = try parseAcceptedFields(acceptedFields: acceptedFields)
      let deviceResponse = try Proximity.shared.generateDeviceResponse(items: items, documents: parsedDocuments, sessionTranscript: nil)
      let strDeviceResponse = Data(deviceResponse).base64EncodedString()
      resolve(strDeviceResponse)
    }catch{
      ME.generateDeviceResponseError.reject(reject: reject, ("error", error.localizedDescription))
    }
  }
  
  /**
   Sends a response containing the documents and the fields which the user decided to present generated by ``generateResponse``.
   It resolves to true after sending the response, otherwise it rejects if an error occurs while decoding the response.
   Currently there's not evidence of the verifier app responding to this request, thus we don't handle the response.
   
   - Parameters:
     - response: A base64 encoded string containing the response generated by ``generateResponse``
     - resolve: The promise to be resolved
     - reject: The promise to be rejected
   */
  @objc(sendResponse:withResolver:withRejecter:)
  func sendResponse(
    response: String,
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ){
    do{
      if let responseData = Data(base64Encoded: response) {
        let decodedResponse = [UInt8](responseData)
        try Proximity.shared.dataPresentation(decodedResponse)
        resolve(true)
      }
    }catch let error {
      ME.sendResponseError.reject(reject: reject, ("error", error.localizedDescription))
    }
  }
  
  /**
   Sends an error response during the presentation according to the SessionData status codes defined in table 20 of the ISO18013-5 standard.
   - Parameters:
     - status: The status error to be sent is an integer of type ``SessionDataStatus``:
       ```
         10 -> Error: session encryption
         11 -> Error: CBOR decoding
         20 -> Session termination
       ```
     - resolve: The promise to be resolved
     - reject: The promise to be rejected
   */
  @objc(sendErrorResponse:withResolver:withRejecter:)
  func sendErrorResponse(status: UInt64, _ resolve: @escaping RCTPromiseResolveBlock,
                               reject: @escaping RCTPromiseRejectBlock){
    do{
      if let statusEnum = SessionDataStatus(rawValue: status) {
        try Proximity.shared.errorPresentation(statusEnum)
      } else {
        ME.sendResponseError.reject(reject: reject, ("error", "Invalid status code"))
      }
      resolve(true)
    }catch let error{
      ME.sendResponseError.reject(reject: reject, ("error", error.localizedDescription))
    }
  }
  
  
  /**
   Closes the bluetooth connection and clears any resource.
   It resolves to true after closing the connection.
   
   - Parameters:
     - resolve: The promise to be resolved
     - reject:  The promise to be rejected
   
   */
  @objc(close:withRejecter:)
  func close(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Proximity.shared.stop()
    resolve(true)
  }
  
  /**
     Converts a device requested from the `onDocumentRequestReceived` callback into a serializable JSON.
     
     - Parameters:
        - request: The request returned from `onDocumentRequestReceived` which contains an array of tuples consists of a doctype, namespaces and the requested claims with a boolean value indicating wether or not the device which is making the request has an intent to retain the dataß
     
     - Returns: A JSON string representing the device request or nil if an error occurs
    */
  private func deviceRequestToJson(request: [(docType: String, nameSpaces: [String: [String: Bool]], isAuthenticated: Bool)]?) -> String? {
    var jsonRequest : [String: AnyHashable] = [:]
    request?.forEach({
      item in
      var subReq: [String: AnyHashable] = [:]
      item.nameSpaces.keys.forEach({
        nameSpace in
        subReq[nameSpace] = item.nameSpaces[nameSpace]
      })
      
      subReq["isAuthenticated"] = item.isAuthenticated
      
      jsonRequest[item.docType] = subReq
    })
    
    let json: [String: AnyHashable] = [
      "request": jsonRequest
    ]
    
    if let jsonData = try? JSONSerialization.data(withJSONObject: json, options: .prettyPrinted),
       let jsonString = String(data: jsonData, encoding: .utf8) {
      return jsonString
    } else {
      return nil
    }
  }
  
  /**
   Sets the proximity handler along with the possible dispatched events and their callbacks.
   The events are then sent to React Native via `RCTEventEmitter`.
   */
  private func setupProximityHandler() {
    Proximity.shared.proximityHandler = { [weak self] event in
      guard let self = self else { return }
      var eventName: String
      var eventBody: [String: Any] = [:]
      
      switch event {
      case .onDeviceConnecting:
        eventName = "onDeviceConnecting"
      case .onDeviceConnected:
        eventName = "onDeviceConnected"
      case .onDocumentRequestReceived(let request):
        eventName = "onDocumentRequestReceived"
        if let request = request {
          /**
           The outermost key represents the credential doctype, the inner key represents the namespace and the innermost key represents the requested fields with a boolean value. Example:
           {
             "org.iso.18013.5.1.mDL": {
               "isAuthenticated": true,
               "org.iso.18013.5.1": {
                 "hair_colour": true,
                 "given_name_national_character": true,
                 "family_name_national_character": true,
                 "given_name": true,
               }
             }
           }
           */
          let jsonString = deviceRequestToJson(request: request)
          // Here we either send the request or an empty string which signals that something went wrong.
          eventBody = ["data": jsonString ?? ""]
        }
      case .onDeviceDisconnected:
        eventName = "onDeviceDisconnected"
      case .onError(let error):
        eventName = "onError"
        eventBody = ["error": error.localizedDescription]
      default:
        eventName = "unknown"
        eventBody = ["error": "Received an unknown event"]
      }
      
      self.sendEvent(withName: eventName, body: eventBody)
    }
  }
  
  @objc func generateOID4VPDeviceResponse(
    _ clientId: String,
    responseUri: String,
    authorizationRequestNonce: String,
    mdocGeneratedNonce: String,
    documents: [Any],
    fieldRequestedAndAccepted: String,
    resolver resolve: RCTPromiseResolveBlock,
    rejecter reject: RCTPromiseRejectBlock
  ) {
    
    do {
      let sessionTranscript = Proximity.shared.generateOID4VPSessionTranscriptCBOR(
          clientId: clientId,
          responseUri: responseUri,
          authorizationRequestNonce: authorizationRequestNonce,
          mdocGeneratedNonce: mdocGeneratedNonce
      )

      let documentsAsProximityDocument = try parseDocuments(documents: documents)
      
      let items = try JSONDecoder().decode([String : [String : [String : Bool]]].self, from: Data(fieldRequestedAndAccepted.utf8))
      
      do {
        let response = try Proximity.shared.generateDeviceResponse(items: items, documents: documentsAsProximityDocument, sessionTranscript: sessionTranscript)
        resolve(Data(response).base64EncodedString())
      } catch {
        ME.unableToGenerateResponse.reject(reject: reject)
        return
      }
    } catch let error as NSError where error.domain == ME.invalidDocRequested.rawValue {
      ME.invalidDocRequested.reject(reject: reject)
    } catch _ as DecodingError {
      ME.invalidItems.reject(reject: reject)
    } catch {
      ME.unexpected.reject(reject: reject)
    }
  }
  
  /**
   Wrapper for rejecting with an error.
   Add a new case in order to extend the possible errors.
   */
  private enum ModuleException: String, CaseIterable {
    case startError = "START_ERROR"
    case qrCodeError = "QR_CODE_ERROR"
    case generateDeviceResponseError = "GENERATE_DEVICE_RESPONSE_ERROR"
    case sendResponseError = "SEND_RESPONSE_ERROR"
    case sendErrorResponse = "SEND_ERROR_RESPONSE_ERROR"
    case invalidDocRequested = "DOC_REQUESTED_PARSING_ERROR"
    case unableToGenerateResponse = "UNABLE_TO_GENERATE_RESPONSE_ERROR"
    case invalidItems = "REQUESTED_ITEMS_PARSING_ERROR"
    case unexpected = "UNEXPECTED_ERROR"
    case unableToDecode = "UNABLE_TO_DECODE"
    
    func error(
      userInfo: [String : Any]? = nil
    ) -> NSError {
      switch self {
      case .startError:
        return NSError(domain: self.rawValue, code: -1, userInfo: userInfo)
      case .qrCodeError:
        return NSError(domain: self.rawValue, code: -1, userInfo: userInfo)
      case .generateDeviceResponseError:
        return NSError(domain: self.rawValue, code: -1, userInfo: userInfo)
      case .sendResponseError:
        return NSError(domain: self.rawValue, code: -1, userInfo: userInfo)
      case .sendErrorResponse:
        return NSError(domain: self.rawValue, code: -1, userInfo: userInfo)
      case .invalidDocRequested:
        return NSError(domain: self.rawValue, code: -1, userInfo: userInfo)
      case .unableToGenerateResponse:
        return NSError(domain: self.rawValue, code: -1, userInfo: userInfo)
      case .invalidItems:
        return NSError(domain: self.rawValue, code: -1, userInfo: userInfo)
      case .unexpected:
        return NSError(domain: self.rawValue, code: -1, userInfo: userInfo)
      case .unableToDecode:
        return NSError(domain: self.rawValue, code: -1, userInfo: userInfo)
      }
    }
    
     /**
      Rejects the provided promise with the appropriate error message and additional data.
      
      - Parameters:
        - reject:  the promise to be rejected.
        - moreUserInfo: additional key-value pairs of data to be passed along with the error.
      
      */
    func reject(
      reject: RCTPromiseRejectBlock,
      _ moreUserInfo: (String, Any)...
    ) {
      let userInfo = [String: Any](uniqueKeysWithValues: moreUserInfo)
      let error = error(userInfo: userInfo)
      reject("\(error.code)", error.domain, error)
    }
  }
}
