import Foundation

enum MediaKind: String, Codable, Sendable {
    case image
    case animatedImage
    case video
}

enum ThemeFilter: String, Codable, CaseIterable, Identifiable, Sendable {
    case chromatic = "CHROMATIC"
    case crt = "CRT"
    case vhs = "VHS"
    case pixelate = "PIXELATE"
    case dreamBloom = "DREAM_BLOOM"
    case monoInk = "MONO_INK"
    case kaleidoscope = "KALEIDOSCOPE"
    case fisheye = "FISHEYE"
    case halftone = "HALFTONE"
    case thermal = "THERMAL"
    case negative = "NEGATIVE"
    case posterize = "POSTERIZE"
    case filmGrain = "FILM_GRAIN"
    case mirrorPrism = "MIRROR_PRISM"
    case liquidGlass = "LIQUID_GLASS"
    case nightVision = "NIGHT_VISION"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .chromatic: "Chromatic"
        case .crt: "CRT"
        case .vhs: "VHS"
        case .pixelate: "Pixelate"
        case .dreamBloom: "Dream Bloom"
        case .monoInk: "Mono Ink"
        case .kaleidoscope: "Kaleidoscope"
        case .fisheye: "Fisheye"
        case .halftone: "Halftone"
        case .thermal: "Thermal"
        case .negative: "Negative"
        case .posterize: "Posterize"
        case .filmGrain: "35mm Film"
        case .mirrorPrism: "Mirror Prism"
        case .liquidGlass: "Liquid Glass"
        case .nightVision: "Night Vision"
        }
    }

    var description: String {
        switch self {
        case .chromatic: "RGB lens separation and subtle edge distortion"
        case .crt: "Scanlines, phosphor shimmer, and curved-screen vignette"
        case .vhs: "Tape jitter, tracking lines, and soft color drift"
        case .pixelate: "Chunky display pixels"
        case .dreamBloom: "Soft luminous highlights and a hazy lens"
        case .monoInk: "High-contrast monochrome editorial treatment"
        case .kaleidoscope: "Mirrored radial glass sectors"
        case .fisheye: "Optical barrel curvature with edge compression"
        case .halftone: "Printed-dot screening driven by luminance"
        case .thermal: "False-color infrared mapping"
        case .negative: "Photographic color inversion"
        case .posterize: "Hard tonal screen-print bands"
        case .filmGrain: "Grain, vignette, and warm highlights"
        case .mirrorPrism: "Angular mirrored facets"
        case .liquidGlass: "Animated thick-glass refraction"
        case .nightVision: "Green phosphor, bloom, noise, and edge falloff"
        }
    }

    var symbol: String {
        switch self {
        case .chromatic: "circle.lefthalf.filled.inverse"
        case .crt: "tv"
        case .vhs: "videotape"
        case .pixelate: "squareshape.split.3x3"
        case .dreamBloom: "sparkles"
        case .monoInk: "circle.lefthalf.striped.horizontal"
        case .kaleidoscope: "camera.filters"
        case .fisheye: "circle.dotted.circle"
        case .halftone: "circle.grid.3x3.fill"
        case .thermal: "thermometer.high"
        case .negative: "circle.inset.filled"
        case .posterize: "square.3.layers.3d"
        case .filmGrain: "film.stack"
        case .mirrorPrism: "triangle"
        case .liquidGlass: "drop"
        case .nightVision: "eye"
        }
    }
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
    var filters: [ThemeFilter] = []

    init() {}

    private enum CodingKeys: String, CodingKey {
        case scale, offsetX, offsetY, rotation, trimStart, trimEnd, muted, loop, filters
    }

    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        scale = try values.decodeIfPresent(Double.self, forKey: .scale) ?? 1
        offsetX = try values.decodeIfPresent(Double.self, forKey: .offsetX) ?? 0
        offsetY = try values.decodeIfPresent(Double.self, forKey: .offsetY) ?? 0
        rotation = try values.decodeIfPresent(Double.self, forKey: .rotation) ?? 0
        trimStart = try values.decodeIfPresent(Double.self, forKey: .trimStart) ?? 0
        trimEnd = try values.decodeIfPresent(Double.self, forKey: .trimEnd)
        muted = try values.decodeIfPresent(Bool.self, forKey: .muted) ?? true
        loop = try values.decodeIfPresent(Bool.self, forKey: .loop) ?? true
        filters = try values.decodeIfPresent([ThemeFilter].self, forKey: .filters) ?? []
    }
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
