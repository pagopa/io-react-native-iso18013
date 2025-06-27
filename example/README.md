# Getting started

The following sections provide instructions to build and run the example app for development purposes.

## Prerequisites

### NodeJS and Ruby

To run the project you need to install the correct version of NodeJS and Ruby.
We recommend the use of a virtual environment of your choice. For ease of use, this guide adopts [nodenv](https://github.com/nodenv/nodenv) for NodeJS, [rbenv](https://github.com/rbenv/rbenv) for Ruby.

The node version used in this project is stored in [.node-version](.node-version),
while the version of Ruby is stored in [.ruby-version](.ruby-version).

### React Native

Follow the [official tutorial](https://reactnative.dev/docs/getting-started-without-a-framework) for installing the `React Native CLI` for your operating system.

If you have a macOS system, you can follow both the tutorial for iOS and for Android. If you have a Linux or Windows system, you need only to install the development environment for Android.

## Build the app

In order to build the app, we use [yarn](https://yarnpkg.com/) for managing javascript dependencies.
As stated [previously](#nodejs-and-ruby), we also use `nodenv` and `rbenv` for managing the environment:

```bash
# Clone the repository
$ git clone https://github.com/pagopa/io-react-native-iso18013

# CD into the repository
$ cd io-react-native-iso18013

# Install NodeJS with nodenv, the returned version should match the one in the .node-version file
$ nodenv install && nodenv version

# Install yarn and rehash to install shims
$ npm install -g yarn && nodenv rehash

# Install dependencies
# Run this only during the first setup and when JS dependencies change
$ yarn

# CD into the example app
$ cd example

# Install Ruby with rbenv, the returned version should match the one in the .ruby-version file
$ rbenv install && rbenv version

# Install bundle
$ gem install bundle

# Install the required Gems from the Gemfile
# Run this only during the first setup and when Gems dependencies change
$ bundle install

# Install podfiles when targeting iOS (ignore this step for Android)
# Run this only during the first setup and when Pods dependencies change
$ cd iOS && bundle exec pod install && cd ..
```

## Run the app

The app must be tested on a real device as it requires Bluetooth capabilities. It can be run on both iOS and Android devices by executing the following command:

```bash
# Run the app on iOS
$ yarn ios

# Run the app on Android
$ yarn android
```
