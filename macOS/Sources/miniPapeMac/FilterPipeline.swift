@preconcurrency import CoreImage
import AppKit

enum FilterPipeline {
    static func apply(_ stack: [ThemeFilter], to source: CIImage, time: Double = 0) -> CIImage {
        stack.reduce(source) { image, filter in
            apply(filter, to: image, time: time).cropped(to: source.extent)
        }
    }

    static func renderImage(at url: URL, filters: [ThemeFilter]) -> NSImage? {
        guard let source = CIImage(contentsOf: url) else { return NSImage(contentsOf: url) }
        let output = apply(filters, to: source)
        let representation = NSCIImageRep(ciImage: output)
        let image = NSImage(size: representation.size)
        image.addRepresentation(representation)
        return image
    }

    private static func apply(_ effect: ThemeFilter, to image: CIImage, time: Double) -> CIImage {
        let extent = image.extent
        let center = CIVector(x: extent.midX, y: extent.midY)

        switch effect {
        case .chromatic:
            let red = channel(image, red: 1, green: 0, blue: 0)
                .transformed(by: .init(translationX: 4, y: 0))
            let green = channel(image, red: 0, green: 1, blue: 0)
            let blue = channel(image, red: 0, green: 0, blue: 1)
                .transformed(by: .init(translationX: -4, y: 0))
            return add(add(red, green), blue)
        case .crt:
            let lines = filtered("CILineScreen", image, ["inputCenter": center, "inputAngle": 0, "inputWidth": 3.0, "inputSharpness": 0.42])
            return filtered("CIVignette", lines, ["inputIntensity": 1.15, "inputRadius": 1.7])
        case .vhs:
            let jitter = CGFloat(sin(time * 8) * 3)
            let shifted = image.transformed(by: .init(translationX: jitter, y: 0))
            return filtered("CIColorControls", shifted, ["inputSaturation": 0.82, "inputContrast": 0.92, "inputBrightness": 0.015])
        case .pixelate:
            return filtered("CIPixellate", image, ["inputCenter": center, "inputScale": 10.0])
        case .dreamBloom:
            return filtered("CIBloom", image, ["inputRadius": 18.0, "inputIntensity": 0.78])
        case .monoInk:
            return filtered("CIPhotoEffectNoir", image)
        case .kaleidoscope:
            return filtered("CIKaleidoscope", image, ["inputCount": 8.0, "inputCenter": center, "inputAngle": time * 0.08])
        case .fisheye:
            return filtered("CIBumpDistortion", image, ["inputCenter": center, "inputRadius": max(extent.width, extent.height) * 0.72, "inputScale": 0.48])
        case .halftone:
            return filtered("CIDotScreen", image, ["inputCenter": center, "inputAngle": 0.2, "inputWidth": 7.0, "inputSharpness": 0.7])
        case .thermal:
            return filtered("CIFalseColor", image, [
                "inputColor0": CIColor(red: 0.03, green: 0, blue: 0.28),
                "inputColor1": CIColor(red: 1, green: 0.88, blue: 0.04)
            ])
        case .negative:
            return filtered("CIColorInvert", image)
        case .posterize:
            return filtered("CIColorPosterize", image, ["inputLevels": 6.0])
        case .filmGrain:
            let warm = filtered("CISepiaTone", image, ["inputIntensity": 0.16])
            let vignette = filtered("CIVignette", warm, ["inputIntensity": 0.72, "inputRadius": 1.8])
            let noise = (CIFilter(name: "CIRandomGenerator")?.outputImage ?? image)
                .transformed(by: .init(translationX: CGFloat(time * 41).truncatingRemainder(dividingBy: 37), y: 0))
            let softNoise = filtered("CIColorMatrix", noise, [
                "inputRVector": CIVector(x: 0.08, y: 0, z: 0, w: 0),
                "inputGVector": CIVector(x: 0, y: 0.08, z: 0, w: 0),
                "inputBVector": CIVector(x: 0, y: 0, z: 0.08, w: 0),
                "inputAVector": CIVector(x: 0, y: 0, z: 0, w: 0.16)
            ])
            return filtered("CISoftLightBlendMode", softNoise, [kCIInputBackgroundImageKey: vignette])
        case .mirrorPrism:
            return filtered("CIKaleidoscope", image, ["inputCount": 6.0, "inputCenter": center, "inputAngle": .pi / 6])
        case .liquidGlass:
            let movingCenter = CIVector(
                x: extent.midX + CGFloat(sin(time * 0.7)) * extent.width * 0.12,
                y: extent.midY + CGFloat(cos(time * 0.6)) * extent.height * 0.12
            )
            let refracted = filtered("CIBumpDistortion", image, ["inputCenter": movingCenter, "inputRadius": min(extent.width, extent.height) * 0.58, "inputScale": 0.34])
            return filtered("CITwirlDistortion", refracted, ["inputCenter": center, "inputRadius": min(extent.width, extent.height) * 0.65, "inputAngle": sin(time * 0.45) * 0.08])
        case .nightVision:
            let mono = filtered("CIColorMonochrome", image, ["inputColor": CIColor(red: 0.08, green: 1, blue: 0.24), "inputIntensity": 1.0])
            let bloom = filtered("CIBloom", mono, ["inputRadius": 8.0, "inputIntensity": 0.45])
            return filtered("CIVignette", bloom, ["inputIntensity": 1.2, "inputRadius": 1.5])
        }
    }

    private static func filtered(_ name: String, _ image: CIImage, _ values: [String: Any] = [:]) -> CIImage {
        guard let filter = CIFilter(name: name) else { return image }
        filter.setValue(image, forKey: kCIInputImageKey)
        values.forEach { filter.setValue($1, forKey: $0) }
        return filter.outputImage ?? image
    }

    private static func channel(_ image: CIImage, red: CGFloat, green: CGFloat, blue: CGFloat) -> CIImage {
        filtered("CIColorMatrix", image, [
            "inputRVector": CIVector(x: red, y: 0, z: 0, w: 0),
            "inputGVector": CIVector(x: 0, y: green, z: 0, w: 0),
            "inputBVector": CIVector(x: 0, y: 0, z: blue, w: 0),
            "inputAVector": CIVector(x: 0, y: 0, z: 0, w: 1)
        ])
    }

    private static func add(_ foreground: CIImage, _ background: CIImage) -> CIImage {
        filtered("CIAdditionCompositing", foreground, [kCIInputBackgroundImageKey: background])
    }
}
