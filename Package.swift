// swift-tools-version: 6.2
import PackageDescription

let package = Package(
    name: "miniPapeMac",
    platforms: [.macOS("27.0")],
    products: [.executable(name: "miniPapeMac", targets: ["miniPapeMac"])],
    targets: [
        .executableTarget(
            name: "miniPapeMac",
            path: "macOS/Sources/miniPapeMac",
            swiftSettings: [.swiftLanguageMode(.v6)]
        ),
        .testTarget(
            name: "miniPapeMacTests",
            dependencies: ["miniPapeMac"],
            path: "macOS/Tests/miniPapeMacTests"
        )
    ]
)

