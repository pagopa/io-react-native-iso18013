module.exports = {
  overrides: [
    {
      exclude: /\/node_modules\//,
      presets: ['module:react-native-builder-bob/babel-preset'],
    },
    {
      include: /\/node_modules\//,
      presets: ['module:@react-native/babel-preset'],
      // Must run before @babel/plugin-transform-modules-commonjs to handle
      // Zod v4's ESM `export * as namespace from '...'` syntax correctly.
      plugins: ['@babel/plugin-transform-export-namespace-from'],
    },
  ],
};
