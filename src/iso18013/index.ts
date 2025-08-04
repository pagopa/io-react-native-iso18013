import { NativeModules, Platform } from 'react-native';
export {
  type EventError,
  type VerifierRequest,
  parseEventError,
  parseVerifierRequest,
} from './iso18013-5/schema';

const LINKING_ERROR =
  `The package '@pagopa/io-react-native-iso18013' (IoReactNativeIso18013) doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

export const IoReactNativeIso18013 = NativeModules.IoReactNativeIso18013
  ? NativeModules.IoReactNativeIso18013
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );
