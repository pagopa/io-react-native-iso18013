# @pagopa/io-react-native-iso18013

This library provides a React Native module based on [iso18013-android](https://github.com/pagopa/iso18013-android) and [iso18013-ios](https://github.com/pagopa/iso18013-ios) which allows proximity presentation according to the ISO 18013-5 standard, remote presentation according to the ISO 18013-7 standard, decoding of CBOR data and management of COSE verify and sign operations.

## Installation

```sh
npm install @pagopa/io-react-native-iso18013
```

## Usage

Each module is documented in its own README file:

- [ISO18013-5](src/iso18013/iso18013-5/README.md): Proximity presentation according to the ISO 18013-5 standard;
- [ISO18013-7](src/iso18013/iso18013-7/README.md): Remote presentation according to the ISO 18013-7 standard;
- [CBOR](src/cbor/cbor/README.md): Decoding of CBOR data into readable objects;
- [COSE](src/cbor/cose/README.md): Management of COSE verify and sign operations.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
