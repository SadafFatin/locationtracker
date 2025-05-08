// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "Location_tracker",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "Location_tracker",
            targets: ["LocationTrackerPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "LocationTrackerPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/LocationTrackerPlugin"),
        .testTarget(
            name: "LocationTrackerPluginTests",
            dependencies: ["LocationTrackerPlugin"],
            path: "ios/Tests/LocationTrackerPluginTests")
    ]
)