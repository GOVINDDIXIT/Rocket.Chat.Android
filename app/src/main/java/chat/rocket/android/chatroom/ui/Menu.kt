package chat.rocket.android.chatroom.ui

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import chat.rocket.android.R
import chat.rocket.android.util.extension.onQueryTextListener

// WIDECHAT
import android.graphics.Color
import android.widget.*
import chat.rocket.android.helper.Constants

internal fun ChatRoomFragment.setupMenu(menu: Menu) {
    setupSearchMessageMenuItem(menu, requireContext())
}

private fun ChatRoomFragment.setupSearchMessageMenuItem(menu: Menu, context: Context) {
    var actionFlags: Int? = null
    actionFlags = MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW

    val searchItem = menu.add(
        Menu.NONE,
        Menu.NONE,
        Menu.NONE,
        R.string.title_search_message
    ).setActionView(SearchView(context))
        .setIcon(R.drawable.ic_chatroom_toolbar_magnifier_20dp)
        .setShowAsActionFlags(
                // WIDECHAT - all items in the overflow menu
            actionFlags
        )
        .setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                dismissEmojiKeyboard()
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                dismissEmojiKeyboard()
                return true
            }
        })

    (searchItem.actionView as? SearchView)?.let {
        // TODO: Check why we need to stylize the search text programmatically instead of by defining it in the styles.xml (ChatRoom.SearchView)
        if (Constants.WIDECHAT) {
            stylizeWidechatSearchView(it)
        } else {
            it.maxWidth = Integer.MAX_VALUE
            stylizeSearchView(it, context)
        }
        setupSearchViewTextListener(it)
        if (it.isIconified) {
            isSearchTermQueried = false
        }
    }
}

private fun stylizeWidechatSearchView(search: SearchView) {
    search.setBackgroundResource(R.drawable.widechat_search_white_background)

    val searchIcon: ImageView? = search.findViewById(R.id.search_mag_icon)
    searchIcon?.setImageResource(R.drawable.ic_search_gray_24px)

    val searchText: TextView? = search.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
    searchText?.setTextColor(Color.GRAY)
    searchText?.setHintTextColor(Color.GRAY)
    searchText?.setHint( R.string.title_search_message)

    val searchCloseButton: ImageView? = search.findViewById(R.id.search_close_btn)
    searchText?.setOnFocusChangeListener { v, hasFocus ->
        if (hasFocus)
            searchCloseButton?.setImageResource(R.drawable.ic_close_gray_24dp)
    }
    searchCloseButton?.setOnClickListener { v ->
        search.clearFocus()
        search.setQuery("", false)
        searchCloseButton?.setImageResource(0)
    }
}


private fun stylizeSearchView(searchView: SearchView, context: Context) {
    val searchText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
    searchText.setTextColor(ResourcesCompat.getColor(context.resources, R.color.color_white, null))
    searchText.setHintTextColor(
        ResourcesCompat.getColor(context.resources, R.color.color_white, null)
    )
}

private fun ChatRoomFragment.setupSearchViewTextListener(searchView: SearchView) {
    searchView.onQueryTextListener {
        // TODO: We use isSearchTermQueried to avoid querying when the search view is expanded but the user doesn't start typing. Check for a native solution.
        if (it.isEmpty() && isSearchTermQueried) {
            presenter.loadMessages(chatRoomId, chatRoomType, clearDataSet = true)
        } else if (it.isNotEmpty()) {
            presenter.searchMessages(chatRoomId, it)
            isSearchTermQueried = true
        }
    }
}
