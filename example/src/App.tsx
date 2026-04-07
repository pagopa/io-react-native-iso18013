import { NavigationContainer } from '@react-navigation/native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { BottomTabsNavigation } from './navigation';

const App = () => {
  return (
    <SafeAreaProvider>
      <NavigationContainer>
        <BottomTabsNavigation />
      </NavigationContainer>
    </SafeAreaProvider>
  );
};

export default App;
