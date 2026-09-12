@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import dev.imanuel.abfuhr.api.client.AbfuhrClient
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.geo.checkIfLocationInHildesheim
import dev.imanuel.abfuhr.helper.notifyDone
import dev.imanuel.abfuhr.helper.resizeToFit
import dev.imanuel.abfuhr.helper.toJpegByteArray
import dev.imanuel.abfuhr.uikit.dsl.scrollableColumn
import dev.imanuel.abfuhr.uikit.dsl.showAlert
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import org.koin.mp.KoinPlatformTools
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSSelectorFromString
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithName
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalForeignApi::class)
fun isNetworkAvailable(): Boolean = memScoped {
    val reachability = SCNetworkReachabilityCreateWithName(
        null,
        "captive.apple.com"
    ) ?: return@memScoped false

    val flags = alloc<UIntVar>()

    if (!SCNetworkReachabilityGetFlags(reachability, flags.ptr)) {
        return@memScoped false
    }

    val value = flags.value

    (value and kSCNetworkReachabilityFlagsReachable) != 0u &&
            (value and kSCNetworkReachabilityFlagsConnectionRequired) == 0u
}

class ImagePickerLauncher(
    private val viewController: UIViewController,
    private val onImageCaptured: (UIImage?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    fun launchCamera() {
        if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
            onImageCaptured(null)
            return
        }

        val picker = UIImagePickerController().apply {
            sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            delegate = this@ImagePickerLauncher
            allowsEditing = false
        }

        viewController.presentViewController(picker, animated = true, completion = null)
    }

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = (didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage]
            ?: didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage]) as? UIImage

        picker.dismissViewControllerAnimated(true) {
            onImageCaptured(image)
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true) {
            onImageCaptured(null)
        }
    }
}

class ReportWasteManagerDelegateBridge(
    private val onLocationUpdated: (CLLocation) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    @ObjCSignatureOverride
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val firstLocation = didUpdateLocations.firstOrNull() as? CLLocation ?: return
        onLocationUpdated(firstLocation)
    }
}

class ReportWasteViewController : UIViewController(nibName = null, bundle = null) {

    init {
        tabBarItem = UITabBarItem(
            title = AbfuhrNavDestination.Locations.title,
            image = AbfuhrNavDestination.Locations.createIcon(),
            tag = AbfuhrNavDestination.Locations.ordinal.toLong()
        )
    }

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val locationManager: CLLocationManager = CLLocationManager()
    private val abfuhrClient: AbfuhrClient
        get() = KoinPlatformTools.defaultContext().get().get()
    private val database: AbfallDatabase
        get() = KoinPlatformTools.defaultContext().get().get()

    private lateinit var myLocationField: UITextField
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var commentView: UITextView
    private lateinit var locationManagerDelegateBridge: ReportWasteManagerDelegateBridge
    private lateinit var pictureAspectConstraint: NSLayoutConstraint
    private lateinit var sendButton: UIBarButtonItem
    private lateinit var captureButton: UIButton
    private var sendIconDefault: UIImage? = null
    private var sendingItem: UIBarButtonItem? = null
    private var pictureView = UIImageView()

    @ObjCAction
    fun locationFieldTapped(sender: UITapGestureRecognizer) {
        locate()
    }

    @ObjCAction
    fun onSendButtonTapped() {
        view.endEditing(true)
        setUploading(true)

        ioScope.launch {
            val result = abfuhrClient.reportWaste(
                latitude,
                longitude,
                myLocationField.text!!,
                commentView.text,
                pictureView.image
                    ?.resizeToFit(1920, 1920)
                    ?.toJpegByteArray()
            )

            mainScope.launch {
                if (result) {
                    notifyDone(true)

                    commentView.text = ""
                    myLocationField.text = ""
                    pictureView.image = null
                    latitude = 0.0
                    longitude = 0.0
                } else {
                    ioScope.launch {
                        val networkAvailable = isNetworkAvailable()
                        mainScope.launch {
                            if (networkAvailable) {
                                showAlert(
                                    "Müll konnte nicht gemeldet werden",
                                    "Es ist es nicht möglich, deine Meldung weiterzuleiten. Bitte versuche es nach einiger Zeit noch einmal."
                                ) {
                                    okAction("Schließen")
                                }
                            } else {
                                showAlert(
                                    "Keine Internetverbindung",
                                    "Dein ${UIDevice.currentDevice.localizedModel} ist nicht mit dem Internet verbunden. Um Müll zu melden brauchst du eine aktive Internetverbindung."
                                ) {
                                    okAction("Schließen")
                                }
                            }
                        }
                    }
                    notifyDone(false)
                }

                setUploading(false)
                if (result) {
                    showSendSuccessCheck()
                }
                sendButton.enabled = !result
            }
        }
    }

    private fun showSendingSpinner() {
        if (sendingItem == null) {
            val spinner = UIActivityIndicatorView().apply {
                activityIndicatorViewStyle = UIActivityIndicatorViewStyleMedium
                startAnimating()
                translatesAutoresizingMaskIntoConstraints = false
            }
            sendingItem = UIBarButtonItem(customView = spinner)
        }
        navigationItem.rightBarButtonItem = sendingItem
    }

    private fun restoreSendButton() {
        navigationItem.rightBarButtonItem = sendButton
    }

    private fun setUploading(uploading: Boolean) {
        sendButton.enabled = !uploading

        if (uploading) {
            showSendingSpinner()
        } else {
            restoreSendButton()
        }
    }

    private fun showSendSuccessCheck() {
        val checkIcon = UIImage.systemImageNamed("checkmark")
        sendButton.image = checkIcon
        mainScope.launch {
            delay(2000.milliseconds)
            sendButton.image = sendIconDefault
        }
    }

    private fun locate() {
        locationManager.run {
            setDelegate(this@ReportWasteViewController.locationManagerDelegateBridge)
            setDesiredAccuracy(kCLLocationAccuracyBest)
            requestWhenInUseAuthorization()
            startUpdatingLocation()
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        locationManagerDelegateBridge = ReportWasteManagerDelegateBridge { location ->
            locationManager.stopUpdatingLocation()
            location.coordinate.useContents {
                if (!checkIfLocationInHildesheim(latitude, longitude)) {
                    showAlert(
                        "Nicht in Hildesheim",
                        "Du scheinst nicht im Landkreis Hildesheim zu sein. Es können nur Meldungen von dort weitergeleitet werden."
                    ) {
                        okAction("Schließen")
                    }
                    sendButton.enabled = false
                } else {
                    this@ReportWasteViewController.latitude = latitude
                    this@ReportWasteViewController.longitude = longitude
                    val loc =
                        database.abfuhrQueries.searchByGeolocation(latitude, longitude).executeAsList().firstOrNull()
                    if (loc != null) {
                        if (loc.locality == "Hildesheim") {
                            myLocationField.text = "${loc.street} Hildesheim"
                        } else {
                            myLocationField.text = "${loc.street} ${loc.district}"
                        }
                        sendButton.enabled = true
                    }
                }
            }
        }
        setupForm()
        val sendIcon = UIImage.systemImageNamed("arrow.up")
        sendIconDefault = sendIcon
        sendButton = UIBarButtonItem(
            image = sendIcon,
            style = UIBarButtonItemStyle.UIBarButtonItemStyleDone,
            target = this,
            action = NSSelectorFromString("onSendButtonTapped"),
        ).apply {
            enabled = false
        }
        navigationItem.rightBarButtonItem = sendButton
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)

        val hasThemeChanged =
            this.traitCollection.hasDifferentColorAppearanceComparedToTraitCollection(previousTraitCollection)
        if (hasThemeChanged) {
            commentView.layer.borderColor = UIColor.systemGray4Color.CGColor
        }
    }

    private fun setupForm() {
        val form = scrollableColumn {
            alignment = UIStackViewAlignmentFill

            padding(16.0)
            myLocationField = singleLineTextField(placeholder = "Position") {
                singleLineTextField.delegate = object : NSObject(), UITextFieldDelegateProtocol {
                    override fun textFieldShouldBeginEditing(textField: UITextField): Boolean = false
                }

                val tapGesture = UITapGestureRecognizer(
                    target = this@ReportWasteViewController,
                    action = NSSelectorFromString("locationFieldTapped:")
                )
                singleLineTextField.addGestureRecognizer(tapGesture)

                rightViewMode = UITextFieldViewMode.UITextFieldViewModeAlways
                rightPadding = 4.0
                rightView = dev.imanuel.abfuhr.uikit.dsl.iconButton("location.circle.fill") {
                    isCircular = true
                    onClick {
                        locate()
                    }
                }
            }
            label("Kommentar")
            commentView = multiLineTextField("Dein Kommentar")

            captureButton = button("Foto hinzufügen") {
                systemImage("camera")
                button.configuration = UIButtonConfiguration.borderedButtonConfiguration()

                onClick {
                    val launcher = ImagePickerLauncher(this@ReportWasteViewController) {
                        pictureView.image = it

                        if (it != null) {
                            pictureAspectConstraint.active = false

                            pictureAspectConstraint =
                                pictureView.heightAnchor.constraintEqualToAnchor(
                                    pictureView.widthAnchor,
                                    multiplier = it.size().useContents {
                                        height / width
                                    }
                                ).apply {
                                    active = true
                                }
                            pictureView.layoutIfNeeded()
                        }
                    }
                    launcher.launchCamera()
                }
            }

            add(pictureView.apply {
                translatesAutoresizingMaskIntoConstraints = false
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFill
                clipsToBounds = true

                pictureAspectConstraint = heightAnchor.constraintEqualToAnchor(
                    widthAnchor,
                    multiplier = 9.0 / 16.0
                ).apply {
                    active = true
                }
            })
            pictureView.widthAnchor.constraintEqualToAnchor(
                captureButton.widthAnchor
            ).active = true
        }

        pictureView.widthAnchor.constraintEqualToAnchor(
            captureButton.widthAnchor
        ).active = true
        view.addSubview(form)
        NSLayoutConstraint.activateConstraints(
            listOf(
                form.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor),
                form.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor),
                form.leadingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.leadingAnchor),
                form.trailingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.trailingAnchor),
            )
        )
    }
}

fun createReportWasteViewController() = ReportWasteViewController()