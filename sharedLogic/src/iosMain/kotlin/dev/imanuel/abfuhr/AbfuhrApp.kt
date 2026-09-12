@file:OptIn(ExperimentalForeignApi::class, ExperimentalObjCName::class)

package dev.imanuel.abfuhr

import dev.imanuel.abfuhr.api.client.apiModule
import dev.imanuel.abfuhr.database.databaseModule
import dev.imanuel.abfuhr.helper.resolveSystemSymbol
import dev.imanuel.abfuhr.preferences.firstSyncHappened
import dev.imanuel.abfuhr.preferences.markFirstSync
import dev.imanuel.abfuhr.screens.createPickupViewController
import dev.imanuel.abfuhr.screens.createReportWasteViewController
import dev.imanuel.abfuhr.screens.createStandorteMapViewController
import dev.imanuel.abfuhr.screens.createWasteAbcViewController
import dev.imanuel.abfuhr.search.searchModule
import dev.imanuel.abfuhr.sync.SyncClient
import dev.imanuel.abfuhr.sync.syncModule
import dev.imanuel.abfuhr.uikit.dsl.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSUserDefaults
import platform.UIKit.*
import kotlin.experimental.ExperimentalObjCName

/**
 * Initializes Koin for iOS if it has not already been started.
 * Exposes all required modules (API, Database, Search, Sync).
 */
@ObjCName(swiftName = "initKoin")
fun initKoin(): KoinApplication {
    return KoinPlatformTools.defaultContext().getOrNull()?.let {
        KoinApplication.init()
    } ?: startKoin {
        modules(apiModule, databaseModule, searchModule, syncModule)
    }
}

/**
 * Navigation destination items for the Abfuhr app shell.
 */
enum class AbfuhrNavDestination(
    val title: String,
    val primarySymbolName: String,
) {
    Pickup(
        title = "Abfuhrtermine",
        primarySymbolName = "calendar",
    ),
    WasteAbc(
        title = "Sortierhilfe",
        primarySymbolName = "arrow.3.trianglepath",
    ),
    Locations(
        title = "Standorte",
        primarySymbolName = "mappin.and.ellipse",
    ),
    ReportWaste(
        title = "Müll melden",
        primarySymbolName = "trash",
    );

    fun createIcon(pointSize: Double = 18.0): UIImage? {
        val config = UIImageSymbolConfiguration.configurationWithPointSize(pointSize)
        val image = UIImage.resolveSystemSymbol(primarySymbolName)
        return image?.imageWithConfiguration(config)
    }
}

class AbfuhrSyncLoadingView : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {

    val activityIndicator = activityIndicator {
        setTranslatesAutoresizingMaskIntoConstraints(false)
        hidesWhenStopped = true
        startAnimating()
    }

    val messageLabel = label {
        text =
            "Bitte warte einen Moment, die Müllabfuhrdaten werden gerade auf dein Gerät runtergeladen, danach kannst du die App benutzen. Das geht sogar ohne Internet."
        textAlignment = NSTextAlignmentCenter
        numberOfLines = 0
        textColor = UIColor.secondaryLabelColor()
        font = UIFont.preferredFontForTextStyle(UIFontTextStyleBody)
        lineBreakMode = NSLineBreakByWordWrapping
        setTranslatesAutoresizingMaskIntoConstraints(false)
    }

    private val stackView = column {
        setTranslatesAutoresizingMaskIntoConstraints(false)
        alignment = UIStackViewAlignmentCenter
        distribution = UIStackViewDistributionFill
        spacing = 24.0
    }

    init {
        backgroundColor = UIColor.systemBackgroundColor()
        setupView()
    }

    private fun setupView() {
        stackView.addArrangedSubview(activityIndicator)
        stackView.addArrangedSubview(messageLabel)
        addSubview(stackView)

        NSLayoutConstraint.activateConstraints(
            listOf(
                stackView.centerXAnchor.constraintEqualToAnchor(centerXAnchor),
                stackView.centerYAnchor.constraintEqualToAnchor(centerYAnchor),
                stackView.leadingAnchor.constraintGreaterThanOrEqualToAnchor(leadingAnchor, 32.0),
                stackView.trailingAnchor.constraintLessThanOrEqualToAnchor(trailingAnchor, -32.0),
                stackView.widthAnchor.constraintLessThanOrEqualToConstant(600.0)
            )
        )
    }

    fun stopAnimating() {
        activityIndicator.stopAnimating()
    }
}

class AbfuhrAppViewController : UIViewController(nibName = null, bundle = null) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val syncLoadingView = AbfuhrSyncLoadingView()
    private var mainNavigationController: AdaptiveNavigationController? = null
    private var isSyncCompleted: Boolean = false

    private val syncClient: SyncClient
        get() = KoinPlatformTools.defaultContext().get().get()

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemBackgroundColor()

        setupMainNavigation()

        if (NSUserDefaults.standardUserDefaults.firstSyncHappened()) {
            syncLoadingView.hidden = true
            isSyncCompleted = true
            showMainView()
        } else {
            setupLoadingView()
            if (!NSUserDefaults.standardUserDefaults.firstSyncHappened()) {
                syncLoadingView.hidden = false
                scope.launch {
                    syncClient.sync()
                    onSyncFinished()
                }
            }
        }
    }

    private fun showMainView() {
        syncLoadingView.stopAnimating()
        val nav = mainNavigationController ?: return
        addChildViewController(nav)
        nav.view.setTranslatesAutoresizingMaskIntoConstraints(false)
        view.addSubview(nav.view)

        NSLayoutConstraint.activateConstraints(
            listOf(
                nav.view.topAnchor.constraintEqualToAnchor(view.topAnchor),
                nav.view.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor),
                nav.view.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor),
                nav.view.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
            )
        )
        nav.selectItemByTag(0L)
        nav.didMoveToParentViewController(this)
    }

    private fun onSyncFinished() {
        if (syncClient.isSuccess.value) {
            NSUserDefaults.standardUserDefaults.markFirstSync()
        }

        isSyncCompleted = true
        showMainView()

        // Smoothly fade out loading view
        UIView.animateWithDuration(
            duration = 0.25,
            animations = {
                syncLoadingView.alpha = 0.0
            },
            completion = { _ ->
                syncLoadingView.hidden = true
                syncLoadingView.removeFromSuperview()
            }
        )
    }

    private fun setupLoadingView() {
        syncLoadingView.setTranslatesAutoresizingMaskIntoConstraints(false)
        view.addSubview(syncLoadingView)

        NSLayoutConstraint.activateConstraints(
            listOf(
                syncLoadingView.topAnchor.constraintEqualToAnchor(view.topAnchor),
                syncLoadingView.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor),
                syncLoadingView.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor),
                syncLoadingView.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
            )
        )
    }

    private fun setupMainNavigation() {
        mainNavigationController = adaptiveNavigation {
            mode = AdaptiveNavigationMode.Auto
            headerTitle = "Müllabfuhr Hildesheim"

            item(
                title = AbfuhrNavDestination.Pickup.title,
                image = AbfuhrNavDestination.Pickup.createIcon() ?: UIImage(),
                tag = 0L
            ) {
                viewController = createPickupViewController()
            }

            item(
                title = AbfuhrNavDestination.WasteAbc.title,
                image = AbfuhrNavDestination.WasteAbc.createIcon() ?: UIImage(),
                tag = 1L
            ) {
                viewController = createWasteAbcViewController()
            }

            item(
                title = AbfuhrNavDestination.Locations.title,
                image = AbfuhrNavDestination.Locations.createIcon() ?: UIImage(),
                tag = 2L
            ) {
                viewController = createStandorteMapViewController()
            }

            item(
                title = AbfuhrNavDestination.ReportWaste.title,
                image = AbfuhrNavDestination.ReportWaste.createIcon() ?: UIImage(),
                tag = 3L
            ) {
                viewController = createReportWasteViewController()
            }
        }
    }
}

fun createAbfuhrAppViewController(): UIViewController {
    return AbfuhrAppViewController()
}
