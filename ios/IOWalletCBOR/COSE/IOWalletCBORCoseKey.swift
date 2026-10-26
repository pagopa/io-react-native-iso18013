//
//  IOWalletCBORCoseKey.swift
//  cbor
//
//  Created by Antonio Caparello on 13/02/25.
//

import Foundation

public class IOWalletCBORCoseKey {
    
    internal let coseKey: CoseKeyImpl
    
    internal init(_ coseKey: CoseKeyImpl) {
        self.coseKey = coseKey
    }
    
    public var crv: IOWalletCBORECCurveName {
        return coseKey.crv
    }
    
    public var kty: IOWalletCBORECCurveType {
        return coseKey.kty
    }
    
    public var x: [UInt8] {
        return coseKey.x
    }
    
    public var y: [UInt8] {
        return coseKey.y
    }
    
    
}

extension IOWalletCBORCoseKey {
    public convenience init(x: [UInt8], y: [UInt8], crv: IOWalletCBORECCurveName = .p256) {
        self.init(CoseKeyImpl(x: x, y: y, crv: crv))
    }
    
    public convenience init(crv: IOWalletCBORECCurveName, x963Representation: Data) {
        self.init(CoseKeyImpl(crv: crv, x963Representation: x963Representation))
    }
    
    public convenience init?(jwk: String) {
        guard let coseKey = CoseKeyImpl(jwk: jwk) else {
            return nil
        }
        self.init(coseKey)
    }
    
    public func getx963Representation() -> Data {
        return coseKey.getx963Representation()
    }
    
    public func toJWK() -> String? {
        return coseKey.toJWK()
    }
    
    public convenience init?(data: [UInt8]) {
        guard let coseKey = CoseKeyImpl(data: data) else {
            return nil
        }
        self.init(coseKey)
    }
}
