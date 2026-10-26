require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "IoReactNativeIso18013"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => min_ios_version_supported }
  s.source       = { :git => "https://github.com/pagopa/io-react-native-iso18013.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift}"

  # Module dependencies
  s.dependency "IOWalletCBOR", "0.2.1"

  # The IOWalletProximity library (https://github.com/pagopa/iso18013-ios) is vendored as source code in ios/IOWalletProximity.
  # Its Swift Package Manager dependencies are added with the same requirements used by the upstream Xcode project.
  if respond_to?(:spm_dependency, true)
    spm_dependency(s,
      url: "https://github.com/niscy-eudiw/SwiftCBOR.git",
      requirement: { kind: "exactVersion", version: "0.5.7" },
      products: ["SwiftCBOR"]
    )
    spm_dependency(s,
      url: "https://github.com/apple/swift-collections.git",
      requirement: { kind: "upToNextMajorVersion", minimumVersion: "1.0.6" },
      products: ["OrderedCollections"]
    )
    spm_dependency(s,
      url: "https://github.com/kishikawakatsumi/KeychainAccess.git",
      requirement: { kind: "upToNextMajorVersion", minimumVersion: "4.2.0" },
      products: ["KeychainAccess"]
    )
    spm_dependency(s,
      url: "https://github.com/apple/swift-asn1.git",
      requirement: { kind: "upToNextMajorVersion", minimumVersion: "1.3.1" },
      products: ["SwiftASN1"]
    )
    spm_dependency(s,
      url: "https://github.com/apple/swift-crypto.git",
      requirement: { kind: "exactVersion", version: "3.10.0" },
      products: ["Crypto"]
    )
    spm_dependency(s,
      url: "https://github.com/apple/swift-certificates.git",
      requirement: { kind: "upToNextMajorVersion", minimumVersion: "1.6.1" },
      products: ["X509"]
    )
  else
    raise "IoReactNativeIso18013 requires React Native >= 0.75 to resolve the Swift Package Manager dependencies of IOWalletProximity"
  end

  # Use install_modules_dependencies helper to install the dependencies if React Native version >=0.71.0.
  # See https://github.com/facebook/react-native/blob/febf6b7f33fdb4904669f99d795eba4c0f95d7bf/scripts/cocoapods/new_architecture.rb#L79.
  if respond_to?(:install_modules_dependencies, true)
    install_modules_dependencies(s)
  else
    s.dependency "React-Core"
  end
end
