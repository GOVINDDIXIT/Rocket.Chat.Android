package chat.rocket.android.main.ui

import DrawableHelper
import android.Manifest
import android.app.Activity
import androidx.appcompat.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import chat.rocket.android.BuildConfig
import chat.rocket.android.R
import chat.rocket.android.authentication.domain.model.DeepLinkInfo
import chat.rocket.android.chatrooms.ui.ChatRoomsFragment
import chat.rocket.android.chatrooms.ui.TAG_CHAT_ROOMS_FRAGMENT
import chat.rocket.android.contacts.worker.ContactsSyncWorker
import chat.rocket.android.main.adapter.AccountsAdapter
import chat.rocket.android.main.adapter.Selector
import chat.rocket.android.main.presentation.MainPresenter
import chat.rocket.android.main.presentation.MainView
import chat.rocket.android.main.uimodel.NavHeaderUiModel
import chat.rocket.android.push.refreshPushToken
import chat.rocket.android.server.domain.PermissionsInteractor
import chat.rocket.android.server.domain.model.Account
import chat.rocket.android.server.ui.INTENT_CHAT_ROOM_ID
import chat.rocket.android.util.extensions.fadeIn
import chat.rocket.android.util.extensions.fadeOut
import chat.rocket.android.util.extensions.rotateBy
import chat.rocket.android.util.extensions.showToast
import chat.rocket.android.util.invalidateFirebaseToken
import chat.rocket.common.model.UserStatus
import chat.rocket.common.util.ifNull
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.nav_header.view.*
import javax.inject.Inject

// WIDECHAT
import chat.rocket.android.helper.Constants
import timber.log.Timber
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import chat.rocket.android.chatrooms.viewmodel.LoadingState

private const val CURRENT_STATE = "current_state"

class MainActivity : AppCompatActivity(), MainView, HasActivityInjector,
    HasSupportFragmentInjector {
    @Inject
    lateinit var activityDispatchingAndroidInjector: DispatchingAndroidInjector<Activity>
    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>
    @Inject
    lateinit var presenter: MainPresenter
    @Inject
    lateinit var permissions: PermissionsInteractor
    private var isFragmentAdded: Boolean = false
    private var expanded = false
    private val headerLayout by lazy { view_navigation.getHeaderView(0) }
    private var chatRoomId: String? = null
    private var deepLinkInfo: DeepLinkInfo? = null
    private var progressDialog: ProgressDialog? = null
    private val PERMISSIONS_REQUEST_RW_CONTACTS = 0

    // WIDECHAT
    val contactsLoadingState = MutableLiveData<LoadingState>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        if (Constants.WIDECHAT) {
            setContentView(R.layout.widechat_activity_main)
        } else {
            setContentView(R.layout.activity_main)
        }
        refreshPushToken()

        contactsLoadingState.postValue(LoadingState.Loading(0))
        syncContacts()

        chatRoomId = intent.getStringExtra(INTENT_CHAT_ROOM_ID)
        deepLinkInfo = intent.getParcelableExtra(Constants.DEEP_LINK_INFO)
        presenter.clearNotificationsForChatroom(chatRoomId)

        presenter.connect()
        if (!Constants.WIDECHAT) {
            presenter.loadServerAccounts()
        }
        presenter.loadCurrentInfo()
        presenter.loadEmojis()
        setupToolbar()

        // WIDECHAT - no nav drawer
        if (!Constants.WIDECHAT) {
            setupNavigationView()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            var deepLinkInfo = it.getParcelableExtra<DeepLinkInfo>(Constants.DEEP_LINK_INFO)
            if (deepLinkInfo != null) {
                val chatRoomsFragment = supportFragmentManager.findFragmentByTag(TAG_CHAT_ROOMS_FRAGMENT) as ChatRoomsFragment
                chatRoomsFragment?.let {
                    it.processDeepLink(deepLinkInfo)
                } .ifNull {
                    isFragmentAdded = false
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean(CURRENT_STATE, isFragmentAdded)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        isFragmentAdded = savedInstanceState?.getBoolean(CURRENT_STATE) ?: false
    }

    override fun onResume() {
        supportFragmentManager.popBackStackImmediate("contactsFragment", 1)
        super.onResume()

        if (intent?.data == "connect://profile.update".toUri()) {
            presenter.logout()
            return
        }

        if (!isFragmentAdded) {
            presenter.toChatList(chatRoomId, deepLinkInfo)
            deepLinkInfo = null
            isFragmentAdded = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            presenter.disconnect()
        }
    }

    override fun onBackPressed() {
        if (Constants.WIDECHAT) {
            super.onBackPressed()
        } else {
            if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                closeDrawer()
            } else {
                supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
                    if (it !is ChatRoomsFragment && supportFragmentManager.backStackEntryCount == 0) {
                        presenter.toChatList(chatRoomId)
                        setCheckedNavDrawerItem(R.id.menu_action_chats)
                    } else {
                        super.onBackPressed()
                    }
                }
            }
        }
    }

    override fun activityInjector(): AndroidInjector<Activity> = activityDispatchingAndroidInjector

    override fun supportFragmentInjector(): AndroidInjector<Fragment> =
        fragmentDispatchingAndroidInjector


    override fun showUserStatus(userStatus: UserStatus) {
        headerLayout.apply {
            image_user_status.setImageDrawable(
                DrawableHelper.getUserStatusDrawable(userStatus, this.context)
            )
        }
    }

    override fun setupUserAccountInfo(uiModel: NavHeaderUiModel) {
        if (Constants.WIDECHAT) {
            return
        }
        with(headerLayout) {
            with(uiModel) {
                if (userStatus != null) {
                    image_user_status.setImageDrawable(
                        DrawableHelper.getUserStatusDrawable(userStatus, context)
                    )
                }
                if (userDisplayName != null) {
                    text_user_name.text = userDisplayName
                }
                if (userAvatar != null) {
                    setAvatar(userAvatar)
                }
                if (serverLogo != null) {
                    server_logo.setImageURI(serverLogo)
                }
                text_server_url.text = uiModel.serverUrl
            }
        }
    }

    override fun setupServerAccountList(serverAccountList: List<Account>) {
        accounts_list.layoutManager = LinearLayoutManager(this)
        accounts_list.adapter = AccountsAdapter(serverAccountList, object : Selector {
            override fun onStatusSelected(userStatus: UserStatus) {
                presenter.changeDefaultStatus(userStatus)
            }

            override fun onAccountSelected(serverUrl: String) {
                presenter.changeServer(serverUrl)
            }

            override fun onAddedAccountSelected() {
                presenter.addNewServer()
            }
        })

        headerLayout.account_container.setOnClickListener {
            it.image_account_expand.rotateBy(180f)
            if (expanded) {
                accounts_list.fadeOut()
            } else {
                accounts_list.fadeIn()
            }
            expanded = !expanded
        }

        headerLayout.image_avatar.setOnClickListener {
            view_navigation.menu.findItem(R.id.menu_action_profile).isChecked = true
            presenter.toUserProfile()
            drawer_layout.closeDrawer(GravityCompat.START)
        }
    }

    override fun closeServerSelection() {
        view_navigation.getHeaderView(0).account_container.performClick()
    }

    override fun alertNotRecommendedVersion() {
        AlertDialog.Builder(this)
            .setMessage(
                getString(
                    R.string.msg_ver_not_recommended,
                    BuildConfig.RECOMMENDED_SERVER_VERSION
                )
            )
            .setPositiveButton(android.R.string.ok, null)
            .create()
            .show()
    }

    override fun blockAndAlertNotRequiredVersion() {
        AlertDialog.Builder(this)
            .setMessage(
                getString(
                    R.string.msg_ver_not_minimum,
                    BuildConfig.REQUIRED_SERVER_VERSION
                )
            )
            .setOnDismissListener { presenter.logout() }
            .setPositiveButton(android.R.string.ok, null)
            .create()
            .show()
    }

    override fun invalidateToken(token: String) = invalidateFirebaseToken(token)

    override fun showMessage(resId: Int) = showToast(resId)

    override fun showMessage(message: String) = showToast(message)

    override fun showGenericErrorMessage() = showMessage(getString(R.string.msg_generic_error))

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    fun setupNavigationView() {
        with (view_navigation.menu) {
            clear()
            setupMenu(this)
        }

        view_navigation.setNavigationItemSelectedListener {
            it.isChecked = true
            closeDrawer()
            onNavDrawerItemSelected(it)
            true
        }

        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp)
        toolbar.setNavigationOnClickListener { openDrawer() }
    }

    fun showLogoutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.title_are_you_sure)
            .setPositiveButton(R.string.action_logout) { _, _ -> presenter.logout()}
            .setNegativeButton(android.R.string.no) { dialog, _ -> dialog.cancel() }
            .create()
            .show()
    }

    fun setAvatar(avatarUrl: String) {
        headerLayout.image_avatar.setImageURI(avatarUrl)
    }

    fun getDrawerLayout(): DrawerLayout = drawer_layout

    fun openDrawer() = drawer_layout.openDrawer(GravityCompat.START)

    fun closeDrawer() = drawer_layout.closeDrawer(GravityCompat.START)

    fun setCheckedNavDrawerItem(@IdRes item: Int) = view_navigation.setCheckedItem(item)

    override fun showProgress() {
        progressDialog = ProgressDialog.show(this, getString(R.string.app_name), getString(R.string.msg_log_out), true, false)
    }

    override fun hideProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun syncContacts() {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS),
                    PERMISSIONS_REQUEST_RW_CONTACTS)
        } else {
            // Permission has already been granted
            val contactsSyncWork = OneTimeWorkRequestBuilder<ContactsSyncWorker>().build()
            WorkManager.getInstance().enqueue(contactsSyncWork)
            WorkManager.getInstance().getStatusById(contactsSyncWork.getId()).observe(this, Observer { info ->
                if (info !=null) {
                    if (info.state.name == "RUNNING") {
                        contactsLoadingState.postValue(LoadingState.Loading(0))
                        Timber.d("Contact sync running")
                    } else if (info.state.isFinished || info.state.name == "FAILED") {
                        contactsLoadingState.postValue(LoadingState.Loaded(0))
                        Timber.d("Contact sync ended")
                    }
                }
            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_RW_CONTACTS -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    syncContacts()
                }
                return
            }
            else -> {
                // Ignore
            }
        }
    }
}
