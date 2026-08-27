import AppKit
import AVKit
import SwiftUI
import UniformTypeIdentifiers

struct EditorView: View {
    @Bindable var model: EditorModel
    @State private var importing = false

    var body: some View {
        NavigationSplitView {
            List {
                Section("Project") {
                    Label("Canvas", systemImage: "rectangle.on.rectangle")
                    Label("Phone", systemImage: "iphone.gen3")
                }
            }
            .navigationTitle("miniPape")
            .navigationSplitViewColumnWidth(min: 170, ideal: 190)
        } content: {
            VStack(spacing: 0) {
                canvas
                Divider()
                transport
                    .padding(14)
            }
            .navigationTitle(model.mediaURL?.deletingPathExtension().lastPathComponent ?? "Untitled")
        } detail: {
            inspector
                .navigationTitle("Inspector")
        }
        .toolbar {
            ToolbarItemGroup {
                Button("Open", systemImage: "plus") { importing = true }
                Button("Preview on Phone", systemImage: "iphone.and.arrow.forward") { model.beginPhonePreview() }
                    .disabled(model.mediaURL == nil || model.phone.status == nil)
                Button("Send", systemImage: "paperplane.fill") { model.sendWallpaper() }
                    .disabled(model.mediaURL == nil || model.phone.status == nil)
            }
        }
        .fileImporter(isPresented: $importing, allowedContentTypes: [.image, .movie], allowsMultipleSelection: false) { result in
            if case let .success(urls) = result, let url = urls.first { model.open(url) }
        }
        .alert("miniPape", isPresented: Binding(
            get: { model.errorMessage != nil },
            set: { if !$0 { model.errorMessage = nil } }
        )) {
            Button("OK") { model.errorMessage = nil }
        } message: {
            Text(model.errorMessage ?? "")
        }
    }

    private var canvas: some View {
        GeometryReader { proxy in
            ZStack {
                Color.black
                if let url = model.mediaURL {
                    media(url)
                        .scaleEffect(model.recipe.scale)
                        .rotationEffect(.degrees(model.recipe.rotation))
                        .offset(
                            x: model.recipe.offsetX * proxy.size.width * 0.5,
                            y: model.recipe.offsetY * proxy.size.height * 0.5
                        )
                } else {
                    ContentUnavailableView(
                        "Open a video, GIF, or image",
                        systemImage: "photo.badge.plus",
                        description: Text("The crop canvas matches the Z Flip 7 cover screen.")
                    )
                    .foregroundStyle(.white)
                }
                GuideOverlay()
            }
            .aspectRatio(FlipCanvas.zFlip7.aspectRatio, contentMode: .fit)
            .clipShape(.rect(cornerRadius: 28))
            .shadow(color: .black.opacity(0.28), radius: 28, y: 12)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(34)
        }
    }

    @ViewBuilder
    private func media(_ url: URL) -> some View {
        if model.mediaKind == .video, let player = model.player {
            VideoPlayer(player: player)
                .aspectRatio(contentMode: .fill)
        } else if let image = NSImage(contentsOf: url) {
            Image(nsImage: image)
                .resizable()
                .scaledToFill()
        }
    }

    private var transport: some View {
        GlassEffectContainer(spacing: 12) {
            HStack(spacing: 12) {
                Button(model.isPlaying ? "Pause" : "Play", systemImage: model.isPlaying ? "pause.fill" : "play.fill") {
                    model.togglePlayback()
                }
                .labelStyle(.iconOnly)
                .buttonStyle(.glass)
                .disabled(model.player == nil)

                Text("1048 × 948")
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(.secondary)

                Spacer()

                Text(model.phone.message)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var inspector: some View {
        Form {
            Section("Crop") {
                cropSlider("Scale", value: $model.recipe.scale, range: 1...4)
                cropSlider("Horizontal", value: $model.recipe.offsetX, range: -1...1)
                cropSlider("Vertical", value: $model.recipe.offsetY, range: -1...1)
                cropSlider("Rotation", value: $model.recipe.rotation, range: -15...15)
                Toggle("Loop", isOn: $model.recipe.loop)
                Toggle("Mute", isOn: $model.recipe.muted)
            }

            Section("Phone") {
                TextField("Phone address", text: Binding(
                    get: { model.phone.address },
                    set: { model.phone.address = $0 }
                ), prompt: Text("192.168.1.20"))
                TextField("Pair code", text: Binding(
                    get: { model.phone.pairCode },
                    set: { model.phone.pairCode = $0 }
                ), prompt: Text("000000"))
                Button("Connect") {
                    Task {
                        do { try await model.phone.connect() }
                        catch { model.errorMessage = error.localizedDescription }
                    }
                }
                if let status = model.phone.status {
                    LabeledContent("Receiver", value: status.receiverName)
                    LabeledContent("Device", value: status.deviceModel)
                }
            }
        }
        .formStyle(.grouped)
        .frame(minWidth: 285, idealWidth: 310)
    }

    private func cropSlider(_ title: String, value: Binding<Double>, range: ClosedRange<Double>) -> some View {
        LabeledContent(title) {
            Slider(value: value, in: range, onEditingChanged: { editing in
                if !editing { model.pushPreviewState() }
            })
        }
    }
}

private struct GuideOverlay: View {
    var body: some View {
        ZStack {
            Rectangle()
                .stroke(.white.opacity(0.18), lineWidth: 1)
                .padding(20)
            HStack(spacing: 0) {
                Spacer()
                Rectangle().fill(.white.opacity(0.1)).frame(width: 1)
                Spacer()
                Rectangle().fill(.white.opacity(0.1)).frame(width: 1)
                Spacer()
            }
            VStack(spacing: 0) {
                Spacer()
                Rectangle().fill(.white.opacity(0.1)).frame(height: 1)
                Spacer()
                Rectangle().fill(.white.opacity(0.1)).frame(height: 1)
                Spacer()
            }
        }
        .allowsHitTesting(false)
    }
}
