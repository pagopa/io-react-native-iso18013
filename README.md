# @pagopa/io-react-native-iso18013

This library provides a React Native module which allows proximity presentation according to the ISO 18013-5 standard, remote presentation according to the ISO 18013-7 standard, decoding of CBOR data and management of COSE verify and sign operations.

## Installation

```sh
npm install @pagopa/io-react-native-iso18013
```

## Usage

This library includes two native modules for both iOS and Android:

- [IoReactNativeIso18013](src/iso18013/iso18013-5/proximity.ts) for ISO 18013-5 and ISO 18013-7 implementations;
- [IoReactNativeCbor](src/cbor/index.ts) for CBOR and COSE implementations.

On the javascript side, the library exposes the following modules:

- [ISO18013-5](src/iso18013/iso18013-5/README.md): Proximity presentation according to the ISO 18013-5 standard which is part of the `IoReactNativeIso18013` module;
- [ISO18013-7](src/iso18013/iso18013-7/README.md): Remote presentation according to the ISO 18013-7 standard which is part of the `IoReactNativeIso18013` module;
- [CBOR](src/cbor/cbor/README.md): Decoding of CBOR data into readable objects which is part of the `IoReactNativeCbor` module;
- [COSE](src/cbor/cose/README.md): Management of COSE verify and sign operations which is part of the `IoReactNativeCbor` module.

## Tests

### JavaScript

```sh
yarn test
```

### Android

```sh
cd example/android
./gradlew test
```

### iOS

The iOS unit tests are located in [ios/Tests](ios/Tests) and cover the `IOWalletCBOR` and `IOWalletProximity` libraries. They are exposed as the `Tests` test spec of the podspec, which is enabled in the example app [Podfile](example/ios/Podfile) and generates the `IoReactNativeIso18013-Unit-Tests` scheme.

Install the pods first:

```sh
cd example/ios
pod install
```

Then run the tests from Xcode by opening `example/ios/IoReactNativeIso18013Example.xcworkspace`, selecting the `IoReactNativeIso18013-Unit-Tests` scheme and an iOS simulator, and pressing `⌘U`.

Alternatively, run them from the command line:

```sh
cd example/ios
xcodebuild test \
  -workspace IoReactNativeIso18013Example.xcworkspace \
  -scheme IoReactNativeIso18013-Unit-Tests \
  -destination 'platform=iOS Simulator,name=iPhone 16'
```

To run a single test, add `-only-testing:IoReactNativeIso18013-Unit-Tests/<TestClass>/<testMethod>`.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
