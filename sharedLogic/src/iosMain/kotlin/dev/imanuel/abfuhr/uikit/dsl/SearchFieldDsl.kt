@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_setAssociatedObject

class SearchFieldDelegateBridge(
    var onReturnAction: (() -> Boolean)? = null,
    var onSearchAction: ((String) -> Unit)? = null,
    var onBeginEditingAction: (() -> Unit)? = null,
    var onEndEditingAction: (() -> Unit)? = null,
    var onClearAction: (() -> Unit)? = null
) : NSObject(), UISearchTextFieldDelegateProtocol {

    override fun textFieldShouldReturn(textField: UITextField): Boolean {
        val query = textField.text ?: ""
        onSearchAction?.invoke(query)
        return onReturnAction?.invoke() ?: run {
            textField.resignFirstResponder()
            true
        }
    }

    override fun textFieldDidBeginEditing(textField: UITextField) {
        onBeginEditingAction?.invoke()
    }

    override fun textFieldDidEndEditing(textField: UITextField) {
        onEndEditingAction?.invoke()
    }

    override fun textFieldShouldClear(textField: UITextField): Boolean {
        onClearAction?.invoke()
        return true
    }
}

private val searchFieldDelegateKey: COpaquePointer = nativeHeap.alloc<ByteVar>().ptr

@UIKitDsl
class SearchFieldBuilder {
    val searchField: UISearchTextField = UISearchTextField(frame = CGRectMake(0.0, 0.0, 0.0, 36.0))

    var text: String? = null
    var placeholder: String? = "Search"
    var textAlignment: NSTextAlignment = NSTextAlignmentNatural
    var borderStyle: UITextBorderStyle = UITextBorderStyle.UITextBorderStyleRoundedRect
    var clearButtonMode: UITextFieldViewMode = UITextFieldViewMode.UITextFieldViewModeWhileEditing
    var returnKeyType: UIReturnKeyType = UIReturnKeyType.UIReturnKeySearch
    var keyboardType: UIKeyboardType = UIKeyboardTypeDefault
    var autocapitalizationType: UITextAutocapitalizationType =
        UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
    var autocorrectionType: UITextAutocorrectionType = UITextAutocorrectionType.UITextAutocorrectionTypeNo
    var isEnabled: Boolean = true

    var leftPadding: Double? = null
    var rightPadding: Double? = null

    private val tokensList = mutableListOf<UISearchToken>()
    private var textChangedListener: ((String) -> Unit)? = null
    private var searchListener: ((String) -> Unit)? = null
    private var returnListener: (() -> Boolean)? = null
    private var beginEditingListener: (() -> Unit)? = null
    private var endEditingListener: (() -> Unit)? = null
    private var clearListener: (() -> Unit)? = null

    fun token(title: String, icon: UIImage? = null) {
        val token = UISearchToken.tokenWithIcon(icon = icon, text = title)
        tokensList.add(token)
    }

    fun token(title: String, systemImageName: String) {
        val img = UIImage.systemImageNamed(systemImageName)
        val token = UISearchToken.tokenWithIcon(icon = img, text = title)
        tokensList.add(token)
    }

    fun onTextChanged(action: (String) -> Unit) {
        this.textChangedListener = action
    }

    fun onSearch(action: (String) -> Unit) {
        this.searchListener = action
    }

    fun onReturn(action: () -> Boolean) {
        this.returnListener = action
    }

    fun onReturnDismissKeyboard() {
        this.returnListener = {
            searchField.resignFirstResponder()
            true
        }
    }

    fun onEditingBegan(action: () -> Unit) {
        this.beginEditingListener = action
    }

    fun onEditingEnded(action: () -> Unit) {
        this.endEditingListener = action
    }

    fun onClear(action: () -> Unit) {
        this.clearListener = action
    }

    fun build(): UISearchTextField {
        text?.let { searchField.setText(it) }
        placeholder?.let { searchField.setPlaceholder(it) }
        searchField.setTextAlignment(textAlignment)
        searchField.setBorderStyle(borderStyle)
        searchField.setClearButtonMode(clearButtonMode)
        searchField.setReturnKeyType(returnKeyType)
        searchField.setKeyboardType(keyboardType)
        searchField.setAutocapitalizationType(autocapitalizationType)
        searchField.setAutocorrectionType(autocorrectionType)
        searchField.setEnabled(isEnabled)

        if (tokensList.isNotEmpty()) {
            searchField.setTokens(tokensList)
        }

        if (leftPadding != null && leftPadding!! > 0.0) {
            val paddingView = UIView(frame = CGRectMake(0.0, 0.0, leftPadding!!, 1.0))
            searchField.setLeftView(paddingView)
            searchField.setLeftViewMode(UITextFieldViewMode.UITextFieldViewModeAlways)
        }

        if (rightPadding != null && rightPadding!! > 0.0) {
            val paddingView = UIView(frame = CGRectMake(0.0, 0.0, rightPadding!!, 1.0))
            searchField.setRightView(paddingView)
            searchField.setRightViewMode(UITextFieldViewMode.UITextFieldViewModeAlways)
        }

        val delegateBridge = SearchFieldDelegateBridge(
            onReturnAction = returnListener,
            onSearchAction = searchListener,
            onBeginEditingAction = beginEditingListener,
            onEndEditingAction = endEditingListener,
            onClearAction = clearListener
        )
        searchField.setDelegate(delegateBridge)
        objc_setAssociatedObject(searchField, searchFieldDelegateKey, delegateBridge, OBJC_ASSOCIATION_RETAIN_NONATOMIC)

        textChangedListener?.let { listener ->
            searchField.onEvent(UIControlEventEditingChanged) {
                listener(searchField.text ?: "")
            }
        }

        return searchField
    }
}

inline fun searchField(
    placeholder: String? = "Search",
    text: String? = null,
    builder: SearchFieldBuilder.() -> Unit = {}
): UISearchTextField {
    val b = SearchFieldBuilder()
    if (placeholder != null) b.placeholder = placeholder
    if (text != null) b.text = text
    b.builder()
    return b.build()
}
