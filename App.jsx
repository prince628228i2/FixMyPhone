import React from 'react';
import {SafeAreaView, Text, StyleSheet} from 'react-native';

export default function App() {
  return (
    <SafeAreaView style={styles.root}>
      <Text style={styles.text}>Fix My Phone</Text>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: {flex: 1, backgroundColor: '#071018', alignItems: 'center', justifyContent: 'center'},
  text: {color: '#fff', fontSize: 28, fontWeight: 'bold'},
});
