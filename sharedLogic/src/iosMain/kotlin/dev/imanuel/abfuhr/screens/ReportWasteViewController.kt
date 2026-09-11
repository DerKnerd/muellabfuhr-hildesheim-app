@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import dev.imanuel.abfuhr.api.client.AbfuhrClient
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.helper.resizeToFit
import dev.imanuel.abfuhr.helper.showToast
import dev.imanuel.abfuhr.helper.toJpegByteArray
import dev.imanuel.abfuhr.uikit.dsl.activityIndicator
import dev.imanuel.abfuhr.uikit.dsl.scrollableColumn
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import org.koin.mp.KoinPlatformTools
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSSelectorFromString
import platform.UIKit.*
import platform.darwin.NSObject

class ImagePickerLauncher(
    private val viewController: UIViewController,
    private val onImageCaptured: (UIImage?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    fun launchCamera() {
        // Verify camera hardware is available on the device/simulator
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
    private lateinit var uploadOverlay: UIView
    private lateinit var uploadIndicator: UIActivityIndicatorView
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
                    showToast(
                        message = "Der Müll wurde gemeldet",
                        isSuccess = true
                    )

                    commentView.text = ""
                    myLocationField.text = ""
                    pictureView.image = null
                    latitude = 0.0
                    longitude = 0.0
                } else {
                    showToast(
                        message = "Leider konnte der Müll nicht gemeldet werden",
                        isSuccess = false
                    )
                }

                setUploading(false)
                sendButton.enabled = !result
            }
        }
    }

    private fun setUploading(uploading: Boolean) {
        uploadOverlay.hidden = !uploading
        sendButton.enabled = !uploading

        if (uploading) {
            uploadIndicator.startAnimating()
        } else {
            uploadIndicator.stopAnimating()
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
                this@ReportWasteViewController.latitude = latitude
                this@ReportWasteViewController.longitude = longitude
                val loc = database.abfuhrQueries.searchByGeolocation(latitude, longitude).executeAsList().firstOrNull()
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
        setupForm()
        val sendIcon = UIImage.systemImageNamed("arrow.up")
        sendButton = UIBarButtonItem(
            image = sendIcon,
            style = UIBarButtonItemStyle.UIBarButtonItemStyleDone,
            target = this,
            action = NSSelectorFromString("onSendButtonTapped"),
        ).apply {
            enabled = false
        }
        navigationItem.rightBarButtonItem = sendButton

        setupUploadIndicator()
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)

        val hasThemeChanged =
            this.traitCollection.hasDifferentColorAppearanceComparedToTraitCollection(previousTraitCollection)
        if (hasThemeChanged) {
            commentView.layer.borderColor = UIColor.systemGray4Color.CGColor
        }
    }

    private fun setupUploadIndicator() {
        uploadOverlay = UIView().apply {
            translatesAutoresizingMaskIntoConstraints = false
            backgroundColor = UIColor.systemBackgroundColor
                .colorWithAlphaComponent(0.75)
            hidden = true
        }

        uploadIndicator = activityIndicator {
            style = UIActivityIndicatorViewStyleMedium
            indicatorView.translatesAutoresizingMaskIntoConstraints = false
        }

        view.addSubview(uploadOverlay)
        uploadOverlay.addSubview(uploadIndicator)

        NSLayoutConstraint.activateConstraints(
            listOf(
                uploadOverlay.topAnchor.constraintEqualToAnchor(view.topAnchor),
                uploadOverlay.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor),
                uploadOverlay.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor),
                uploadOverlay.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor),

                uploadIndicator.centerXAnchor.constraintEqualToAnchor(
                    uploadOverlay.centerXAnchor
                ),
                uploadIndicator.centerYAnchor.constraintEqualToAnchor(
                    uploadOverlay.centerYAnchor
                )
            )
        )
    }

    private fun setupForm() {
        val form = scrollableColumn {
            alignment = UIStackViewAlignmentFill

            padding(16.0)
            label("Meine Position")
            myLocationField = singleLineTextField {
                singleLineTextField.delegate = object : NSObject(), UITextFieldDelegateProtocol {
                    override fun textFieldShouldBeginEditing(textField: UITextField): Boolean = false
                }

                val tapGesture = UITapGestureRecognizer(
                    target = this@ReportWasteViewController,
                    action = NSSelectorFromString("locationFieldTapped:")
                )
                singleLineTextField.addGestureRecognizer(tapGesture)

                rightViewMode = UITextFieldViewMode.UITextFieldViewModeAlways
                rightView = dev.imanuel.abfuhr.uikit.dsl.iconButton("location.circle.fill") {
                    isCircular = true
                    onClick {
                        locate()
                    }
                }
            }
            label("Kommentar")
            commentView = multiLineTextField("Dein Kommentar")

            val captureRow = UIStackView().apply {
                axis = UILayoutConstraintAxisHorizontal
                distribution = UIStackViewDistributionFill
                spacing = 8.0
                translatesAutoresizingMaskIntoConstraints = false
                alignment = UIStackViewAlignmentTop

            }
            captureButton = button("Foto aufnehmen") {
                button.configuration = UIButtonConfiguration.tintedButtonConfiguration()

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

            pictureView.apply {
                translatesAutoresizingMaskIntoConstraints = false
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFill
                clipsToBounds = true

                pictureAspectConstraint = heightAnchor.constraintEqualToAnchor(
                    widthAnchor,
                    multiplier = 9.0 / 16.0
                ).apply {
                    active = true
                }
            }

            captureRow.addArrangedSubview(pictureView)
            captureRow.addArrangedSubview(captureButton)

            pictureView.widthAnchor.constraintEqualToAnchor(
                captureButton.widthAnchor
            ).active = true

            add(captureRow)
        }

        pictureView.widthAnchor.constraintEqualToAnchor(
            captureButton.widthAnchor
        ).active = true
        view.addSubview(form)
        NSLayoutConstraint.activateConstraints(
            listOf(
                form.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor),
                form.bottomAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.bottomAnchor),
                form.leadingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.leadingAnchor),
                form.trailingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.trailingAnchor),
            )
        )
    }
}

fun createReportWasteViewController() = ReportWasteViewController()