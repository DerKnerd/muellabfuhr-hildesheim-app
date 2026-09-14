import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    init() {
        AbfuhrAppKt.doInitKoin()
        SyncDatabaseKt.initializeBackgroundTasks()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}