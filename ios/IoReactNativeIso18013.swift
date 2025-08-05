import Foundation
import IOWalletProximity
import React

@objc(IoReactNativeIso18013)
class IoReactNativeIso18013: RCTEventEmitter {
  
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
      - certificates: Two-dimensional array of base64 strings representing DER encoded X.509 certificate which are used to authenticate the verifier app
      - resolve: The promise to be resolved
      - reject: The promise to be rejected
  */
  @objc(start:withResolver:withRejecter:)
  func start(
    certificates: [Any],
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ){
    do {
      let certsData = try parseCertificates(certificates)
      try Proximity.shared.start(certsData)
      resolve(true)
    } catch let proximityError as ProximityError {
      reject(ModuleErrorCodes.generateResponseError.rawValue, proximityError.description, proximityError)
    } catch let parsingError as ParsingError{
      reject(ModuleErrorCodes.generateResponseError.rawValue, parsingError.description, parsingError)
    } catch {
      reject(ModuleErrorCodes.generateResponseError.rawValue, error.localizedDescription, error)
    }
  }
  
  /**
   Utility function to parse an array coming from the React Native Bridge into an array of Data representing DER encoded X.509 certificates.
   
   - Parameters:
      - certificates:Two-dimensional array of base64 strings representing DER encoded X.509 certificate
   
    - Returns: A two-dimensional array of Data containing DER encoded X.509 certificates.
  */
  private func parseCertificates(_ certificates: [Any]) throws -> [[Data]] {
    return try certificates.enumerated().compactMap { (chainIndex, item) in
      guard let certStrings = item as? [String] else {
        throw ParsingError.certificatesNotValid("Certificate chain at index \(chainIndex) is not an array of strings.")
      }
      return try certStrings.enumerated().map { (certIndex, certString) in
        guard let data = Data(base64Encoded: certString) else {
          throw ParsingError.certificatesNotValid("Cartificate at index \(certIndex) in the chain at index \(chainIndex) is not a valid base64 string.")
        }
        return data
      }
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
    } catch {
      reject(ModuleErrorCodes.getQrCodeError.rawValue, error.localizedDescription, error)
    }
  }
  
  /**
   Parses an array of documents from the React Native bridge which doesn't have any typing to an array of ``IOWalletProximity.ProximityDocument``.
   It checks if each element in the array has  `issuerSignedContent`, `alias` and `docType` properties in order to build a ``IOWalletProximity.ProximityDocument``, then it appens it to the array.
   The result can be fed to ``IOWalletProximity.generateResponse``.
   
   - Parameters:
      - documents: An array containing documents. Each document is defined as a map containing:
         - issuerSignedContent which is a base64 or base64url encoded string representing the credential;
         - alias which is the alias of the key used to sign the credential;
         - docType which is the document type.
   
   - Throws: `ParsingError`:
      - If the provided documents array do not contain a dictionary;
      - If the provided dictionary doesn't adhere to the structure we expect;
      - If the issuerSignedContent is not a valid base64 or base64url encoded string;
      - If the creation of a ProximityDdocument fails.
   
   - Returns: An array of `ProximityDocument` containg the documents to be presented
   */
  private func parseDocuments(documents: [Any]) throws -> [ProximityDocument] {
    return try documents.map{ (element) -> ProximityDocument in
      guard let dict : [String: Any] = element as? [String: Any] else { throw ParsingError.documentsNotValid("The provided documents array element can't be parsed as a dictionary") }
      guard
        let issuerSignedContent = dict["issuerSignedContent"] as? String,
        let alias = dict["alias"] as? String,
        let docType = dict["docType"] as? String,
        let decodedIssuerSignedContent = try? Base64Utils.decodeBase64OrBase64URL(base: issuerSignedContent)
      else {
        throw ParsingError.documentsNotValid ("The provided issuerSignedContent is not a valid base64 or base64url or don't contain the required fields alas, doctype and issuerSignedContent")
      }
      guard let document = ProximityDocument(
        docType: docType,
        issuerSigned: [UInt8](decodedIssuerSignedContent),
        deviceKeyTag: alias
      ) else {
        throw ParsingError.documentsNotValid("The parsing for the document with docType \(docType) failed")
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
   
   - Throws: `ParsingError` if a value doesn't has the ``AcceptedFieldsDict`` shape or the result dictionary is empty
   
   - Returns: An ``AcceptedFieldsDict`` containg the accepted fields to be presented
   
   */
  private func parseAcceptedFields(acceptedFields: [AnyHashable: Any]) throws(ParsingError) -> AcceptedFieldsDict {
    var result: AcceptedFieldsDict = [:]
    
    for (key, value) in acceptedFields {
      guard let keyString = key as? String else {
        throw ParsingError.acceptedFieldsNotValid("The accepted fields keys must be a String")
      }
      guard let valueDict = value as? [String: [String: Bool]] else {
        throw ParsingError.acceptedFieldsNotValid("The accepted fields values must be of type [String: [String: Bool]]")
      }
      result[keyString] = valueDict
    }
    
    return result
  }
  
  /**
     Generates a response containing the documents and the fields which the user decided to present.
     It parses the untyped ``documents`` and ``acceptedFields`` parameters and feeds them to the ``IOWalletProximity.generateDeviceResponse`` function.
     It resolves the promise with the response as a base64 encoded string.
     It rejects the promise if an error occurs during the parameters parsing or while generating the device response.
     
     - Parameters:
       - documents: An array containing documents. Each document is defined as a map containing:
           - issuerSignedContent which is a base64 or base64url encoded string representing the credential;
           - alias which is the alias of the key used to sign the credential;
           - docType which is the document type.
       - issuerSignedContent which is a base64 or base64url encoded string representing the credential;
       - alias which is the alias of the key used to sign the credential;
       - docType which is the document type.
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
    }
    catch let proximityError as ProximityError {
      reject(ModuleErrorCodes.generateResponseError.rawValue, proximityError.description, proximityError)
    }
    catch let parsingError as ParsingError {
      reject(ModuleErrorCodes.generateResponseError.rawValue, parsingError.description, parsingError)
    } catch {
      reject(ModuleErrorCodes.generateResponseError.rawValue, error.localizedDescription, error)
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
      reject(ModuleErrorCodes.sendResponseError.rawValue, error.localizedDescription, error)
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
        reject(ModuleErrorCodes.sendErrorResponseError.rawValue, "Invalid status code provided: \(status)", nil)
      }
      resolve(true)
    }catch let error{
      reject(ModuleErrorCodes.sendErrorResponseError.rawValue, error.localizedDescription, error)
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
    fieldRequestedAndAccepted: [AnyHashable: Any],
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
      let items = try parseAcceptedFields(acceptedFields: fieldRequestedAndAccepted)
      let response = try Proximity.shared.generateDeviceResponse(items: items, documents: documentsAsProximityDocument, sessionTranscript: sessionTranscript)
      resolve(Data(response).base64EncodedString())
    } catch let parsingError as ParsingError{
      reject(ModuleErrorCodes.generateOID4VPResponseError.rawValue, parsingError.description, parsingError)
    } catch let proximityError as ProximityError{
      reject(ModuleErrorCodes.generateOID4VPResponseError.rawValue, proximityError.description, proximityError)
    }
    catch {
      reject(ModuleErrorCodes.generateOID4VPResponseError.rawValue, error.localizedDescription, error)
    }
  }
  
  private class DocRequested {
    var issuerSignedContent : [UInt8]
    var alias : String
    var docType : String
    
    public init(issuerSignedContent: [UInt8], alias: String, docType: String) {
      self.issuerSignedContent = issuerSignedContent
      self.alias = alias
      self.docType = docType
    }
  }
  
  /**
   Custom Error which is thrown when a parsing error occurs in our utility functions which converts data from the bridge to what
   our underlying functions expect.
   This is needed in order to provide a customized description which can include more debug information.
   */
  enum ParsingError : Error, CustomStringConvertible {
      case documentsNotValid(String)
      case certificatesNotValid(String)
      case acceptedFieldsNotValid(String)
      
      public var description: String {
          switch(self) {
            case .documentsNotValid(let message):
                return message
            case .certificatesNotValid(let message):
              return message
            case .acceptedFieldsNotValid(let message):
              return message
          }
      }
  }
  
  // Errors which this module uses to reject a promise
  private enum ModuleErrorCodes: String, CaseIterable {
    // ISO18013-5 related errors
    case startError = "START_ERROR"
    case getQrCodeError = "GET_QR_CODE_ERROR"
    case sendResponseError = "SEND_RESPONSE_ERROR"
    case sendErrorResponseError = "SEND_ERROR_RESPONSE_ERROR"
    case generateResponseError = "GENERATE_RESPONSE_ERROR"
    
    // ISO18013-7 related errors
    case generateOID4VPResponseError = "GENERATE_OID4VP_RESPONSE_ERROR"
  }
}
