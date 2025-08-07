# @pagopa/io-react-native-iso18013

This library provides a React Native module based on [iso18013-android](https://github.com/pagopa/iso18013-android) and [iso18013-ios](https://github.com/pagopa/iso18013-ios) which allows proximity presentation according to the ISO 18013-5 standard, remote presentation according to the ISO 18013-7 standard, decoding of CBOR data and management of COSE verify and sign operations.

## Installation

```sh
npm install @pagopa/io-react-native-iso18013
```

## Usage

This library includes two native modules for both iOS and Android:

- [IoReactNativeIso18013](src/iso18013/iso18013-5/proximity.ts) for ISO 18013-5 and ISO 18013-7 implementations;
- [IoReactNativeCbor](src/cbor/cbor.ts) for CBOR and COSE implementations.

On the javascript side, the library exposes the following modules:

- [ISO18013-5](src/iso18013/iso18013-5/README.md): Proximity presentation according to the ISO 18013-5 standard which is part of the `IoReactNativeIso18013` module;
- [ISO18013-7](src/iso18013/iso18013-7/README.md): Remote presentation according to the ISO 18013-7 standard which is part of the `IoReactNativeIso18013` module;
- [CBOR](src/cbor/cbor/README.md): Decoding of CBOR data into readable objects which is part of the `IoReactNativeCbor` module;
- [COSE](src/cbor/cose/README.md): Management of COSE verify and sign operations which is part of the `IoReactNativeCbor` module.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
