import SwiftUI

@main
struct MiniPapeApp: App {
    @State private var model = EditorModel()

    var body: some Scene {
        WindowGroup("miniPape") {
            EditorView(model: model)
                .frame(minWidth: 980, minHeight: 680)
        }
        .defaultSize(width: 1180, height: 780)
        .windowResizability(.contentMinSize)
    }
}

