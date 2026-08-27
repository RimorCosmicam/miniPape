import AppKit
import AVKit
import CoreImage
import SwiftUI
import UniformTypeIdentifiers

private enum InspectorPage: String, CaseIterable, Identifiable {
    case crop = "Crop"
    case filters = "Filters"
    case phone = "Phone"
    var id: String { rawValue }
}

struct EditorView: View {
    @Bindable var model: EditorModel
    @State private var importing = false
    @State private var inspectorPresented = true
    @State private var inspectorPage = InspectorPage.crop

    var body: some View {
        ZStack(alignment: .bottom) {
            stage
            transport.padding(.horizontal, 24).padding(.bottom, 20)
        }
        .navigationTitle(model.mediaURL?.deletingPathExtension().lastPathComponent ?? "miniPape")
        .inspector(isPresented: $inspectorPresented) {
            inspector.inspectorColumnWidth(min: 280, ideal: 320, max: 380)
        }
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                Button("Open", systemImage: "plus") { importing = true }
                Button("Preview on Phone", systemImage: "iphone.and.arrow.forward") { model.beginPhonePreview() }
                    .disabled(model.mediaURL == nil || model.phone.status == nil)
                Button("Send", systemImage: "paperplane.fill") { model.sendWallpaper() }
                    .disabled(model.mediaURL == nil || model.phone.status == nil)
                Divider()
                Button("Inspector", systemImage: "sidebar.right") { inspectorPresented.toggle() }
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

    private var stage: some View {
        GeometryReader { proxy in
            let horizontalInset: CGFloat = proxy.size.width < 900 ? 12 : 22
            let topInset: CGFloat = 16
            let bottomInset: CGFloat = 82
            let availableWidth = max(1, proxy.size.width - horizontalInset * 2)
            let availableHeight = max(1, proxy.size.height - topInset - bottomInset)
            let canvasWidth = min(availableWidth, availableHeight * FlipCanvas.zFlip7.aspectRatio)
            let canvasHeight = canvasWidth / FlipCanvas.zFlip7.aspectRatio

            ZStack {
                Color(nsColor: .windowBackgroundColor)
                canvas
                    .frame(width: canvasWidth, height: canvasHeight)
                    .clipShape(.rect(cornerRadius: 24))
                    .shadow(color: .black.opacity(0.24), radius: 24, y: 10)
                    .overlay {
                        RoundedRectangle(cornerRadius: 24)
                            .stroke(.white.opacity(0.12), lineWidth: 1)
                    }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private var canvas: some View {
        GeometryReader { proxy in
            ZStack {
                Color.black
                if model.mediaURL != nil {
                    media
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
                        description: Text("Designed at the Z Flip 7 cover-screen ratio.")
                    )
                    .foregroundStyle(.white)
                }
                GuideOverlay()
            }
            .clipped()
        }
    }

    @ViewBuilder
    private var media: some View {
        if model.mediaKind == .video, let player = model.player {
            PlayerSurface(player: player)
        } else if let image = model.filteredImage {
            Image(nsImage: image).resizable().scaledToFill()
        }
    }

    private var transport: some View {
        GlassEffectContainer(spacing: 10) {
            HStack(spacing: 10) {
                Button(model.isPlaying ? "Pause" : "Play", systemImage: model.isPlaying ? "pause.fill" : "play.fill") {
                    model.togglePlayback()
                }
                .labelStyle(.iconOnly)
                .buttonStyle(.glass)
                .disabled(model.player == nil)

                VStack(alignment: .leading, spacing: 1) {
                    Text("Z Flip 7 Cover").font(.caption.weight(.medium))
                    Text("1048 × 948").font(.caption2.monospacedDigit()).foregroundStyle(.secondary)
                }

                if !model.recipe.filters.isEmpty {
                    Divider().frame(height: 22)
                    Label("\(model.recipe.filters.count)", systemImage: "camera.filters")
                        .font(.caption.weight(.medium)).foregroundStyle(.secondary)
                }

                Spacer(minLength: 20)
                Text(model.phone.message).font(.caption).foregroundStyle(.secondary).lineLimit(1)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 9)
            .frame(maxWidth: 560)
            .glassEffect(.regular, in: .rect(cornerRadius: 22))
        }
        .frame(maxWidth: .infinity)
    }

    private var inspector: some View {
        VStack(spacing: 0) {
            Picker("Inspector", selection: $inspectorPage) {
                ForEach(InspectorPage.allCases) { page in Text(page.rawValue).tag(page) }
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            .padding(14)
            Divider()
            switch inspectorPage {
            case .crop: cropInspector
            case .filters: filterInspector
            case .phone: phoneInspector
            }
        }
    }

    private var cropInspector: some View {
        Form {
            Section("Composition") {
                cropSlider("Scale", value: $model.recipe.scale, range: 1...4, valueLabel: model.recipe.scale.formatted(.number.precision(.fractionLength(2))))
                cropSlider("Horizontal", value: $model.recipe.offsetX, range: -1...1, valueLabel: model.recipe.offsetX.formatted(.number.precision(.fractionLength(2))))
                cropSlider("Vertical", value: $model.recipe.offsetY, range: -1...1, valueLabel: model.recipe.offsetY.formatted(.number.precision(.fractionLength(2))))
                cropSlider("Rotation", value: $model.recipe.rotation, range: -15...15, valueLabel: "\(model.recipe.rotation.formatted(.number.precision(.fractionLength(1))))°")
            }
            Section("Playback") {
                Toggle("Loop", isOn: $model.recipe.loop)
                Toggle("Mute", isOn: $model.recipe.muted)
                    .onChange(of: model.recipe.muted) { _, muted in model.player?.isMuted = muted }
            }
        }
        .formStyle(.grouped)
    }

    private var filterInspector: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 10) {
                if !model.recipe.filters.isEmpty {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Active Stack").font(.headline)
                        Text("Effects run from top to bottom.").font(.caption).foregroundStyle(.secondary)
                    }
                    .padding(.bottom, 2)

                    ForEach(Array(model.recipe.filters.enumerated()), id: \.offset) { index, filter in
                        filterRow(filter, at: index)
                    }

                    Divider().padding(.vertical, 5)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text("Effects").font(.headline)
                    Text("Select several to build a stack.").font(.caption).foregroundStyle(.secondary)
                }
                .padding(.bottom, 2)

                ForEach(ThemeFilter.allCases) { filter in
                    filterToggleCard(filter)
                }
            }
            .padding(12)
        }
    }

    private func filterToggleCard(_ filter: ThemeFilter) -> some View {
        let selected = model.recipe.filters.contains(filter)
        return Button {
            model.toggleFilter(filter)
        } label: {
            ZStack(alignment: .bottomLeading) {
                FilterLivePreview(source: model.filterPreviewSource, filter: filter)

                LinearGradient(
                    colors: [.clear, .black.opacity(0.82)],
                    startPoint: .center,
                    endPoint: .bottom
                )

                HStack(alignment: .bottom, spacing: 10) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(filter.label)
                            .font(.headline)
                            .foregroundStyle(.white)
                        Text(filter.description)
                            .font(.caption2)
                            .foregroundStyle(.white.opacity(0.78))
                            .lineLimit(2)
                    }
                    Spacer(minLength: 6)
                    Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                        .font(.title2)
                        .foregroundStyle(selected ? Color.accentColor : .white.opacity(0.8))
                        .symbolRenderingMode(.hierarchical)
                }
                .padding(12)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 132)
            .clipShape(.rect(cornerRadius: 16))
            .overlay {
                RoundedRectangle(cornerRadius: 16)
                    .stroke(selected ? Color.accentColor : .white.opacity(0.12), lineWidth: selected ? 3 : 1)
            }
            .contentShape(.rect(cornerRadius: 16))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(filter.label), \(selected ? "selected" : "not selected")")
    }

    private func filterRow(_ filter: ThemeFilter, at index: Int) -> some View {
        HStack(spacing: 10) {
            Image(systemName: filter.symbol).frame(width: 22).foregroundStyle(.secondary)
            VStack(alignment: .leading, spacing: 2) {
                Text(filter.label).font(.callout.weight(.medium))
                Text(filter.description).font(.caption2).foregroundStyle(.secondary).lineLimit(2)
            }
            Spacer(minLength: 4)
            ControlGroup {
                Button("Move Up", systemImage: "chevron.up") { model.moveFilter(from: index, by: -1) }
                    .disabled(index == 0)
                Button("Move Down", systemImage: "chevron.down") { model.moveFilter(from: index, by: 1) }
                    .disabled(index == model.recipe.filters.count - 1)
                Button("Remove", systemImage: "xmark") { model.removeFilter(at: index) }
            }
            .labelStyle(.iconOnly)
            .controlSize(.small)
        }
        .padding(10)
        .background(.quaternary.opacity(0.45), in: .rect(cornerRadius: 12))
    }

    private var phoneInspector: some View {
        Form {
            Section("Receiver") {
                TextField("Phone address", text: Binding(
                    get: { model.phone.address }, set: { model.phone.address = $0 }
                ), prompt: Text("192.168.1.20"))
                TextField("Pair code", text: Binding(
                    get: { model.phone.pairCode }, set: { model.phone.pairCode = $0 }
                ), prompt: Text("000000"))
                Button("Connect", systemImage: "antenna.radiowaves.left.and.right") {
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
            Section {
                Text("Live preview sends crop, playback, and the complete ordered filter stack without flattening the source.")
                    .font(.caption).foregroundStyle(.secondary)
            }
        }
        .formStyle(.grouped)
    }

    private func cropSlider(_ title: String, value: Binding<Double>, range: ClosedRange<Double>, valueLabel: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title)
                Spacer()
                Text(valueLabel).foregroundStyle(.secondary).monospacedDigit()
            }
            Slider(value: value, in: range, onEditingChanged: { editing in
                if !editing { model.pushPreviewState() }
            })
        }
    }
}

private struct PlayerSurface: NSViewRepresentable {
    let player: AVPlayer
    func makeNSView(context: Context) -> AVPlayerView {
        let view = AVPlayerView()
        view.controlsStyle = .none
        view.videoGravity = .resizeAspectFill
        view.player = player
        return view
    }
    func updateNSView(_ view: AVPlayerView, context: Context) { view.player = player }
}

private struct FilterLivePreview: View {
    let source: CIImage?
    let filter: ThemeFilter

    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.16)) { timeline in
            if let source {
                Image(nsImage: FilterPipeline.render(
                    source,
                    filters: [filter],
                    time: timeline.date.timeIntervalSinceReferenceDate
                ))
                .resizable()
                .scaledToFill()
            } else {
                ZStack {
                    Color.black.opacity(0.72)
                    Image(systemName: filter.symbol)
                        .font(.system(size: 34))
                        .foregroundStyle(.white.opacity(0.45))
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }
}

private struct GuideOverlay: View {
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 18).stroke(.white.opacity(0.16), lineWidth: 1).padding(18)
            HStack(spacing: 0) {
                Spacer(); Rectangle().fill(.white.opacity(0.09)).frame(width: 1)
                Spacer(); Rectangle().fill(.white.opacity(0.09)).frame(width: 1)
                Spacer()
            }
            VStack(spacing: 0) {
                Spacer(); Rectangle().fill(.white.opacity(0.09)).frame(height: 1)
                Spacer(); Rectangle().fill(.white.opacity(0.09)).frame(height: 1)
                Spacer()
            }
        }
        .allowsHitTesting(false)
    }
}
