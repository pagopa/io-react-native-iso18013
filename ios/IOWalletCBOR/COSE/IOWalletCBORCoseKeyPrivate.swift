//
//  IOWalletCBORCoseKeyPrivate.swift
//  cbor
//
//  Created by Antonio Caparello on 13/02/25.
//

internal import SwiftCBOR

import Foundation

public class IOWalletCBORCoseKeyPrivate {
    
    internal let coseKeyPrivate: CoseKeyPrivateImpl
    
    internal init(_ coseKeyPrivate: CoseKeyPrivateImpl) {
        self.coseKeyPrivate = coseKeyPrivate
    }
    
}

extension IOWalletCBORCoseKeyPrivate {
    public convenience init(privateKeyx963Data: Data, crv: IOWalletCBORECCurveName = .p256) {
        self.init(CoseKeyPrivateImpl(privateKeyx963Data: privateKeyx963Data, crv: crv))
    }
    
    public convenience init(crv: IOWalletCBORECCurveName) {
        self.init(CoseKeyPrivateImpl(crv: crv))
    }
   
    public convenience init?(crv: IOWalletCBORECCurveName, keyTag: String) {
        let getQuery: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: keyTag,
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef as String: true
        ]
        
        var item: CFTypeRef?
        let status = SecItemCopyMatching(getQuery as CFDictionary, &item)
        
        guard status == errSecSuccess else {
            return nil
        }
        
        self.init(crv: crv, secKey: (item as! SecKey))
      
    }
    
    public convenience init?(crv: IOWalletCBORECCurveName, secKey: SecKey) {
        guard let coseKeyPrivate = CoseKeyPrivateImpl(crv: crv, secKey: secKey) else {
            return nil
        }
        self.init(coseKeyPrivate)
    }
    
    public convenience init(publicKeyx963Data: Data, secureEnclaveKeyID: Data) {
        self.init(CoseKeyPrivateImpl(publicKeyx963Data: publicKeyx963Data, secureEnclaveKeyID: secureEnclaveKeyID))
    }
    
    public convenience init(x: [UInt8], y: [UInt8], d: [UInt8], crv: IOWalletCBORECCurveName = .p256) {
        self.init(CoseKeyPrivateImpl(x: x, y: y, d: d, crv: crv))
    }
    
    public convenience init?(data: [UInt8]) {
        guard let coseKeyPrivate = CoseKeyPrivateImpl(data: data) else {
            return nil
        }
        self.init(coseKeyPrivate)
    }
    
    public convenience init?(base64: String) {
        guard let coseKeyPrivate = CoseKeyPrivateImpl(base64: base64) else {
            return nil
        }
        self.init(coseKeyPrivate)
    }
    
    public func getx963Representation() -> Data {
        self.coseKeyPrivate.getx963Representation()
    }
    
    public func base64Encoded() -> String {
        return self.coseKeyPrivate.base64Encoded(options: CBOROptions())
    }
    
    public var key: IOWalletCBORCoseKey {
        return IOWalletCBORCoseKey(self.coseKeyPrivate.key)
    }
    
    public var secureEnclaveKeyID: Data? {
        return self.coseKeyPrivate.secureEnclaveKeyID
    }
    
    public func encode() -> [UInt8] {
        return self.coseKeyPrivate.encode(options: CBOROptions())
    }
    
    
    
    
}
