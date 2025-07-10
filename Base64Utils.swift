import Foundation

/**
 * This class contains a set of utilities for encoding and decoding base64 and base64url strings.
 */
class Base64Utils {
  enum Base64DecodingError: Error {
    case invalidBase64(String)
  }
  
  /**
   Parses a base64 or base64url `String` into `Data` buffer.
   
   - Parameters:
      - documents: An array of any elements. In order to be added to the result array each element must be a dictionary with `issuerSignedContent`, `alias` and `docType` as keys and strings as values.
   
   - Throws: `Base64DecodingError.invalidBase64` if `base` is not a valid base64 or base64url
   
   - Returns: A `Data` buffer.
   */
  static func decodeBase64OrBase64URL(base: String) throws -> Data {
    if let data = Data(base64UrlEncoded: base) {
      return data
    } else if let data = Data(base64Encoded: base) {
      return data
    } else {
      throw Base64DecodingError.invalidBase64("Input is not valid Base64 or Base64URL")
    }
  }
  
}
