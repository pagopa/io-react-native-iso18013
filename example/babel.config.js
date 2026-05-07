const path = require('path');
const { getConfig } = require('react-native-builder-bob/babel-config');
const pkg = require('../package.json');

const root = path.resolve(__dirname, '..');

module.exports = getConfig(
  {
    presets: ['module:@react-native/babel-preset'],
    // Must run before @babel/plugin-transform-modules-commonjs to handle
    // Zod v4's ESM `export * as namespace from '...'` syntax correctly.
    plugins: ['@babel/plugin-transform-export-namespace-from'],
  },
  { root, pkg }
);
