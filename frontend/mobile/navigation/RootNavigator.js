import React from 'react'
import { NavigationContainer } from '@react-navigation/native'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import LoginScreen from '../screens/LoginScreen'
import RoomListScreen from '../screens/RoomListScreen'
import ChatScreen from '../screens/ChatScreen'
import ProfileScreen from '../screens/ProfileScreen'

const Stack = createNativeStackNavigator()

export default function RootNavigator({ user, onLogin, onLogout }) {
  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {user ? (
          <>
            <Stack.Screen name="Rooms">
              {props => <RoomListScreen {...props} onLogout={onLogout} />}
            </Stack.Screen>
            <Stack.Screen name="Chat" component={ChatScreen} />
            <Stack.Screen name="Profile">
              {props => <ProfileScreen {...props} user={user} onLogout={onLogout} />}
            </Stack.Screen>
          </>
        ) : (
          <Stack.Screen name="Login">
            {props => <LoginScreen {...props} onLogin={onLogin} />}
          </Stack.Screen>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  )
}
