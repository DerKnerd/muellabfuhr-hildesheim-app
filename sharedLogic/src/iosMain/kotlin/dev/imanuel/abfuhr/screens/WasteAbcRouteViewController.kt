package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import dev.imanuel.abfuhr.api.client.AbfuhrClient
import dev.imanuel.abfuhr.database.AbfallAbcDisposalRoute
import dev.imanuel.abfuhr.uikit.dsl.scrollableColumn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.downloadTaskWithURL
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.rename
import platform.posix.unlink

fun UIViewController.downloadAndShareFile(fileName: String, urlString: String) {
    val url = NSURL.URLWithString(urlString) ?: return

    val task = NSURLSession.sharedSession.downloadTaskWithURL(url) { tempUrl, response, error ->
        if (error != null || tempUrl == null) {
            // show error alert
            return@downloadTaskWithURL
        }

        val safeFileName = fileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .ifBlank { "download" }
        val targetPath = "${NSTemporaryDirectory().trimEnd('/')}/$safeFileName"
        val tempPath = tempUrl.path
        val shareUrl = if (tempPath != null) {
            unlink(targetPath)
            if (rename(tempPath, targetPath) == 0) NSURL(fileURLWithPath = targetPath) else tempUrl
        } else {
            tempUrl
        }

        dispatch_async(dispatch_get_main_queue()) {
            val shareController = UIActivityViewController(
                activityItems = listOf(shareUrl),
                applicationActivities = null
            )

            presentViewController(
                shareController,
                animated = true,
                completion = null
            )
        }
    }

    task.resume()
}

fun openInDefaultBrowser(urlString: String) {
    val url = NSURL.URLWithString(urlString) ?: return

    dispatch_async(dispatch_get_main_queue()) {
        UIApplication.sharedApplication.openURL(
            url = url,
            options = emptyMap<Any?, Any>(),
            completionHandler = null
        )
    }
}

class WasteAbcRouteViewController(
    private val route: AbfallAbcDisposalRoute,
    private val language: String
) : UIViewController(nibName = null, bundle = null) {
    init {
        tabBarItem = UITabBarItem(
            title = AbfuhrNavDestination.WasteAbc.title,
            image = AbfuhrNavDestination.WasteAbc.createIcon(),
            tag = AbfuhrNavDestination.WasteAbc.ordinal.toLong()
        )
        title = route.title
    }

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val abfuhrClient: AbfuhrClient
        get() = KoinPlatformTools.defaultContext().get().get()

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemBackgroundColor()

        val detailsView = scrollableColumn {
            textView(route.description) {
                padding(16.0, 16.0, 0.0, 16.0)
                isSelectable = true
                font = UIFont.systemFontOfSize(UIFont.systemFontSize)
            }
            if (route.openingHours.isNotBlank()) {
                textView("Öffnungszeiten") {
                    padding(16.0, 16.0, 0.0, 16.0)
                    font = UIFont.boldSystemFontOfSize(UIFont.labelFontSize)
                }
                textView(route.openingHours.trim()) {
                    padding(16.0, 0.0)
                    font = UIFont.systemFontOfSize(UIFont.systemFontSize)
                }
            }
            if (route.street.isNotBlank() || route.city.isNotBlank() || route.zipcode.isNotBlank()) {
                textView("Adresse") {
                    padding(16.0, 16.0, 0.0, 16.0)
                    font = UIFont.boldSystemFontOfSize(UIFont.labelFontSize)
                }
                textView(buildString {
                    if (route.street.isNotBlank()) {
                        append(route.street.trim())
                        if (route.city.isNotBlank() || route.zipcode.isNotBlank()) {
                            append(", ")
                        }
                    }
                    val zipCity = listOf(route.zipcode.trim(), route.city.trim()).filter { it.isNotBlank() }
                    append(zipCity.joinToString(" "))
                }) {
                    padding(16.0, 0.0)
                    font = UIFont.systemFontOfSize(UIFont.systemFontSize)
                }
            }
            if (route.fees.isNotBlank()) {
                textView("Gebühren") {
                    padding(16.0, 16.0, 0.0, 16.0)
                    font = UIFont.boldSystemFontOfSize(UIFont.labelFontSize)
                }
                textView(route.fees.trim()) {
                    padding(16.0, 0.0)
                    font = UIFont.systemFontOfSize(UIFont.systemFontSize)
                }
            }

            val files = listOfNotNull(
                route.file1,
                route.file2,
                route.file3
            ).filter { it.isNotBlank() }
            val fileDescriptions = listOfNotNull(
                route.descriptionFile1,
                route.descriptionFile2,
                route.descriptionFile3
            ).filter { it.isNotBlank() }
            if (files.isNotEmpty() && files.size == fileDescriptions.size) {
                textView("Downloads") {
                    padding(16.0, 16.0, 0.0, 16.0)
                    font = UIFont.boldSystemFontOfSize(UIFont.labelFontSize)
                }
                listView {
                    isScrollEnabled = false
                    rowHeight = 44.0

                    files.forEachIndexed { index, file ->
                        item(fileDescriptions[index].trim()) {
                            systemIcon("arrow.down.circle", pointSize = 22.0)
                            tintColor = UIColor.systemBlueColor

                            onSelect {
                                mainScope.launch {
                                    val basePart = abfuhrClient.getContentFilesUrl(language).trimEnd('/')
                                    val filePart = file.trimStart('/')
                                    val url = "$basePart/$filePart"
                                    downloadAndShareFile(file, url)
                                }
                            }
                        }
                    }
                }
            }

            val links = listOfNotNull(
                route.link1,
                route.link2,
                route.link3
            ).filter { it.isNotBlank() }
            val linkDescriptions = listOfNotNull(
                route.descriptionLink1,
                route.descriptionLink2,
                route.descriptionLink3
            ).filter { it.isNotBlank() }
            if (links.isNotEmpty() && links.size == linkDescriptions.size) {
                textView("Links") {
                    padding(16.0, 16.0, 0.0, 16.0)
                    font = UIFont.boldSystemFontOfSize(UIFont.labelFontSize)
                }
                listView {
                    isScrollEnabled = false
                    rowHeight = 44.0

                    links.forEachIndexed { index, link ->
                        item(linkDescriptions[index].trim()) {
                            systemIcon("arrow.up.right.square", pointSize = 22.0)
                            tintColor = UIColor.systemBlueColor

                            onSelect {
                                openInDefaultBrowser(link)
                            }
                        }
                    }
                }
            }
        }

        view.addSubview(detailsView)

        NSLayoutConstraint.activateConstraints(
            listOf(
                detailsView.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor),
                detailsView.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor),
                detailsView.leadingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.leadingAnchor),
                detailsView.trailingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.trailingAnchor),
            )
        )
    }
}

fun createWasteAbcRouteViewController(route: AbfallAbcDisposalRoute, language: String) =
    WasteAbcRouteViewController(route, language)