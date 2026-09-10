import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    init() {
        AbfuhrAppKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}