import AVFoundation
import Foundation
import Observation

@MainActor
@Observable
final class EditorModel {
    var mediaURL: URL?
    var mediaKind: MediaKind?
    var recipe = CropRecipe()
    var player: AVPlayer?
    var isPlaying = false
    var errorMessage: String?
    let phone = PhoneClient()

    func open(_ url: URL) {
        mediaURL = url
        let extensionName = url.pathExtension.lowercased()
        if ["mp4", "mov", "m4v", "webm"].contains(extensionName) {
            mediaKind = .video
            player = AVPlayer(url: url)
            player?.isMuted = recipe.muted
        } else if ["gif", "apng"].contains(extensionName) {
            mediaKind = .animatedImage
            player = nil
        } else {
            mediaKind = .image
            player = nil
        }
        recipe = CropRecipe()
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

