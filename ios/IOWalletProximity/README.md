# IOWalletProximity

Source code of the [iso18013-ios](https://github.com/pagopa/iso18013-ios) library, embedded in this React Native module instead of being fetched from CocoaPods.

- Upstream: `https://github.com/pagopa/iso18013-ios`, branch `main`, commit `3a3c80c5f87e58f4005efee2621738556b66350c` (`feat: [SIW-000] Enhance NFC emulation handling (#38)`).
- Content: the `IOWalletProximity/IOWalletProximity` folder of the upstream repository with the same structure, without the framework umbrella header and the DocC catalog.
- License: MIT, see [LICENSE](LICENSE).

The sources are compiled in the `IoReactNativeIso18013` module, so `import IOWalletProximity` is not needed.
Since types such as `CoseKey` and `CoseKeyPrivate` are also defined by `IOWalletCBOR`, the latter must be referenced with the module prefix (e.g. `IOWalletCBOR.CoseKey`).

## Dependencies

The Swift Package Manager dependencies are declared in [IoReactNativeIso18013.podspec](../../IoReactNativeIso18013.podspec) via `spm_dependency`, mirroring the requirements of the upstream Xcode project:

| Package                                                                   | Product              | Requirement      |
| ------------------------------------------------------------------------- | -------------------- | ---------------- |
| [SwiftCBOR](https://github.com/niscy-eudiw/SwiftCBOR)                     | `SwiftCBOR`          | exactly `0.5.7`  |
| [swift-collections](https://github.com/apple/swift-collections)           | `OrderedCollections` | from `1.0.6`     |
| [KeychainAccess](https://github.com/kishikawakatsumi/KeychainAccess)      | `KeychainAccess`     | from `4.2.0`     |
| [swift-asn1](https://github.com/apple/swift-asn1)                         | `SwiftASN1`          | from `1.3.1`     |
| [swift-crypto](https://github.com/apple/swift-crypto)                     | `Crypto`             | exactly `3.10.0` |
| [swift-certificates](https://github.com/apple/swift-certificates)         | `X509`               | from `1.6.1`     |

## Updating

1. Replace the content of this folder with the Swift sources of `IOWalletProximity/IOWalletProximity` from the desired upstream revision, preserving this `README.md` and `LICENSE`.
2. Align the dependencies in the podspec with the upstream `IOWalletProximity.xcodeproj` package references.
3. Update the commit reference above, run `pod install` in `example/ios` and build the example app.
