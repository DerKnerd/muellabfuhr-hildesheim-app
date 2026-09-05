package dev.imanuel.abfuhr.ui.dsl

import platform.UIKit.UIColor
import platform.UIKit.UIViewController

/**
 * Example demonstrating how to use the UIKit DSL to build a complete screen with:
 * - Layout controls (vstack, hstack, scrollView, container, spacer, divider)
 * - Buttons (button with primary/outline styles and click handlers)
 * - Textfields (textField with placeholder, secure text, change listeners)
 * - Text display (label/text with presets, alignments, colors)
 * - Navigation bar (navigationBar / setupNavigationBar with bar buttons)
 * - Title bar (titleBar with title, subtitle, back button, action buttons)
 */
class SampleUiViewController : UIViewController(nibName = null, bundle = null) {

    override fun viewDidLoad() {
        super.viewDidLoad()

        // 1. Navigation bar setup via UIViewController extension
        setupNavigationBar {
            title = "Waste Management"
            rightButton("Settings") {
                println("Settings tapped!")
            }
        }

        // 2. Screen layout using the DSL
        setContent {
            vstack(spacing = 0.0) {
                // Layout modifier: fill root view safe area
                fillSuperview()

                // A. Title Bar Control
                titleBar(
                    title = "Pickup Schedule",
                    subtitle = "Hildesheim Central"
                ) {
                    showBackButton = true
                    onBackClick {
                        println("Back clicked!")
                    }
                    action("Filter") {
                        println("Filter clicked!")
                    }
                }

                divider()

                // B. Scrollable Content Layout
                scrollView {
                    contentVStack(spacing = 16.0) {
                        padding(all = 16.0)

                        // C. Text Display Controls
                        text("Search Pickups") {
                            title2()
                        }

                        label("Enter street name or waste type to search upcoming pickups.") {
                            footnote()
                        }

                        // D. Textfield Controls
                        val searchField = textField(placeholder = "Search street...") {
                            onTextChanged { query ->
                                println("Search query updated: $query")
                            }
                            onReturnDismiss()
                        }

                        val passwordField = textField(placeholder = "Admin passcode (optional)") {
                            passwordField()
                            onReturnDismiss()
                        }

                        // E. Layout Controls: Horizontal Stack (HStack) with Buttons
                        hstack(spacing = 12.0) {
                            button("Search") {
                                primaryStyle()
                                onClick {
                                    println("Search triggered for: ${searchField.text}")
                                }
                            }

                            button("Clear") {
                                outlineStyle()
                                onClick {
                                    searchField.text = ""
                                    passwordField.text = ""
                                }
                            }

                            spacer()
                        }

                        divider()

                        // Text display showcase
                        text("Upcoming Events") {
                            headline()
                        }

                        container {
                            backgroundColor = DslColors.secondaryBackground
                            cornerRadius = 12.0

                            vstack(spacing = 8.0) {
                                padding(all = 12.0)

                                text("Restmüll (Residual Waste)") {
                                    title3()
                                    textColor = DslColors.primary
                                }

                                text("Next pickup: Monday, 07:00 AM") {
                                    body()
                                }

                                label("Please place bins at the curb by 06:30 AM.") {
                                    caption()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
