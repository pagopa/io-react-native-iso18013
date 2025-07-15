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
    print("here")
    if let data = dataFromBase64Url(base64url: base) {
      return data
    } else if let data = Data(base64Encoded: base) {
      return data
    } else {
      throw Base64DecodingError.invalidBase64("Input is not valid Base64 or Base64URL")
    }
  }
  
  static private func dataFromBase64Url(base64url: String) -> Data? {
    var base64 = base64url
        .replacingOccurrences(of: "-", with: "+")
        .replacingOccurrences(of: "_", with: "/")
    if base64.count % 4 != 0 {
        base64.append(String(repeating: "=", count: 4 - base64.count % 4))
    }
    print(base64)
    return Data(base64Encoded: base64)
  }
}
