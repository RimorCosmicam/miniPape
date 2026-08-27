import SwiftUI

@main
struct MiniPapeApp: App {
    @State private var model = EditorModel()

    var body: some Scene {
        WindowGroup("miniPape") {
            EditorView(model: model)
                .frame(minWidth: 860, minHeight: 620)
        }
        .defaultSize(width: 1280, height: 820)
        .windowResizability(.contentMinSize)
    }
}
