import SwiftUI
import SharedLogic

struct AbfuhrAppViewWrapper: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return AbfuhrAppKt.createAbfuhrAppViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

struct ContentView: View {
    var body: some View {
        AbfuhrAppViewWrapper()
            .background(Color(UIColor.systemBackground))
            .ignoresSafeArea()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
