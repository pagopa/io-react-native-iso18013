import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import Iso180135Screen from './iso18013/Iso180135Screen';
import CborScreen from './cbor/CborScreen';
import Iso1801357Screen from './iso18013/Iso180137Screen';

const Tab = createBottomTabNavigator();

export const BottomTabsNavigation = () => {
  return (
    <Tab.Navigator>
      <Tab.Screen
        name="ISO18013-5"
        component={Iso180135Screen}
        options={{ tabBarIconStyle: { display: 'none' } }}
      />
      <Tab.Screen
        name="ISO18013-7"
        component={Iso1801357Screen}
        options={{ tabBarIconStyle: { display: 'none' } }}
      />
      <Tab.Screen
        name="CBOR & COSE"
        component={CborScreen}
        options={{ tabBarIconStyle: { display: 'none' } }}
      />
    </Tab.Navigator>
  );
};
