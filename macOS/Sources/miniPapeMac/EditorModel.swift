import AVFoundation
import AppKit
import CoreImage
import Foundation
import Observation

@MainActor
@Observable
final class EditorModel {
    var mediaURL: URL?
    var mediaKind: MediaKind?
    var recipe = CropRecipe()
    var player: AVPlayer?
    private(set) var filterPreviewSource: CIImage?
    var isPlaying = false
    var errorMessage: String?
    let phone = PhoneClient()

    func open(_ url: URL) {
        recipe = CropRecipe()
        mediaURL = url
        let extensionName = url.pathExtension.lowercased()
        if ["mp4", "mov", "m4v", "webm"].contains(extensionName) {
            mediaKind = .video
            let item = AVPlayerItem(url: url)
            player = AVPlayer(playerItem: item)
            player?.isMuted = recipe.muted
            rebuildVideoComposition()
            prepareVideoFilterPreview(url)
        } else if ["gif", "apng"].contains(extensionName) {
            mediaKind = .animatedImage
            player = nil
        } else {
            mediaKind = .image
            player = nil
            filterPreviewSource = CIImage(contentsOf: url).map { FilterPipeline.previewSource(from: $0) }
        }
        if mediaKind == .animatedImage {
            filterPreviewSource = CIImage(contentsOf: url).map { FilterPipeline.previewSource(from: $0) }
        }
    }

    var filteredImage: NSImage? {
        guard let mediaURL else { return nil }
        return FilterPipeline.renderImage(at: mediaURL, filters: recipe.filters)
    }

    func addFilter(_ filter: ThemeFilter) {
        recipe.filters.append(filter)
        filtersDidChange()
    }

    func toggleFilter(_ filter: ThemeFilter) {
        if recipe.filters.contains(filter) {
            recipe.filters.removeAll { $0 == filter }
        } else {
            recipe.filters.append(filter)
        }
        filtersDidChange()
    }

    func removeFilter(at index: Int) {
        guard recipe.filters.indices.contains(index) else { return }
        recipe.filters.remove(at: index)
        filtersDidChange()
    }

    func moveFilter(from index: Int, by distance: Int) {
        let destination = index + distance
        guard recipe.filters.indices.contains(index), recipe.filters.indices.contains(destination) else { return }
        recipe.filters.swapAt(index, destination)
        filtersDidChange()
    }

    private func filtersDidChange() {
        rebuildVideoComposition()
        pushPreviewState()
    }

    private func prepareVideoFilterPreview(_ url: URL) {
        filterPreviewSource = nil
        Task {
            let generator = AVAssetImageGenerator(asset: AVURLAsset(url: url))
            generator.appliesPreferredTrackTransform = true
            let time = CMTime(seconds: 0.2, preferredTimescale: 600)
            if let frame = try? generator.copyCGImage(at: time, actualTime: nil) {
                filterPreviewSource = FilterPipeline.previewSource(from: CIImage(cgImage: frame))
            }
        }
    }

    private func rebuildVideoComposition() {
        guard mediaKind == .video, let item = player?.currentItem else { return }
        let filters = recipe.filters
        guard !filters.isEmpty else {
            item.videoComposition = nil
            return
        }
        item.videoComposition = AVVideoComposition(asset: item.asset) { request in
            let seconds = request.compositionTime.seconds
            let output = FilterPipeline.apply(filters, to: request.sourceImage.clampedToExtent(), time: seconds)
                .cropped(to: request.sourceImage.extent)
            request.finish(with: output, context: nil)
        }
    }

    func togglePlayback() {
        guard let player else { return }
        if isPlaying { player.pause() } else { player.play() }
        isPlaying.toggle()
        pushPreviewState()
    }

    func beginPhonePreview() {
        guard let mediaURL, let mediaKind else { return }
        Task {
            do {
                try await phone.startPreview(fileURL: mediaURL, kind: mediaKind)
                try await phone.updatePreview(previewState)
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    func pushPreviewState() {
        guard phone.status != nil else { return }
        let state = previewState
        Task {
            do { try await phone.updatePreview(state) }
            catch { errorMessage = error.localizedDescription }
        }
    }

    func sendWallpaper() {
        guard let mediaURL, let mediaKind else { return }
        Task {
            do { try await phone.sendWallpaper(fileURL: mediaURL, kind: mediaKind, recipe: recipe) }
            catch { errorMessage = error.localizedDescription }
        }
    }

    var previewState: PreviewState {
        let seconds = player?.currentTime().seconds ?? 0
        return PreviewState(recipe: recipe, playhead: seconds.isFinite ? seconds : 0, playing: isPlaying, canvas: .zFlip7)
    }
}
