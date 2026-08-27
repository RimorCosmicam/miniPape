import Foundation

enum MediaKind: String, Codable, Sendable {
    case image
    case animatedImage
    case video
}

struct FlipCanvas: Codable, Equatable, Sendable {
    static let zFlip7 = Self(width: 1048, height: 948)
    let width: Int
    let height: Int
    var aspectRatio: Double { Double(width) / Double(height) }
}

struct CropRecipe: Codable, Equatable, Sendable {
    var scale = 1.0
    var offsetX = 0.0
    var offsetY = 0.0
    var rotation = 0.0
    var trimStart = 0.0
    var trimEnd: Double?
    var muted = true
    var loop = true
}

struct PreviewState: Codable, Sendable {
    let recipe: CropRecipe
    let playhead: Double
    let playing: Bool
    let canvas: FlipCanvas
}

struct PhoneStatus: Codable, Sendable {
    let receiverName: String
    let deviceModel: String
    let protocolVersion: Int
    let canvasWidth: Int
    let canvasHeight: Int
}

