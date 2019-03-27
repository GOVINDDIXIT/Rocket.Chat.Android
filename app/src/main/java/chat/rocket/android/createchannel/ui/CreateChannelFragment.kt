package chat.rocket.android.createchannel.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chat.rocket.android.R
import chat.rocket.android.analytics.AnalyticsManager
import chat.rocket.android.analytics.event.ScreenViewEvent
import chat.rocket.android.createchannel.presentation.CreateChannelPresenter
import chat.rocket.android.createchannel.presentation.CreateChannelView
import chat.rocket.android.main.ui.MainActivity
import chat.rocket.android.members.adapter.MembersAdapter
import chat.rocket.android.members.uimodel.MemberUiModel
import chat.rocket.android.util.extension.asObservable
import chat.rocket.android.util.extensions.inflate
import chat.rocket.android.util.extensions.showToast
import chat.rocket.android.util.extensions.ui
import chat.rocket.common.model.RoomType
import chat.rocket.common.model.roomTypeOf
import com.google.android.material.chip.Chip
import dagger.android.support.AndroidSupportInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_create_channel.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// WIDECHAT
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import chat.rocket.android.contacts.adapter.SelectedContactsAdapter
import chat.rocket.android.contacts.models.Contact
import chat.rocket.android.helper.Constants
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.fragment_create_group.*

internal const val TAG_CREATE_CHANNEL_FRAGMENT = "CreateChannelFragment"

private const val BUNDLE_CREATE_CHANNEL_MEMBERS = "BUNDLE_CREATE_CHANNEL_MEMBERS"

class CreateChannelFragment : Fragment(), CreateChannelView, ActionMode.Callback {
    @Inject
    lateinit var createChannelPresenter: CreateChannelPresenter
    @Inject
    lateinit var analyticsManager: AnalyticsManager
    private var actionMode: ActionMode? = null
    private val adapter: MembersAdapter = MembersAdapter {
        if (it.username != null) {
            val member: Contact? = memberList.find { member ->
                member.getUsername() == it.username
            }
            processSelectedMember(member!!)
        }
    }
    private val compositeDisposable = CompositeDisposable()
    private var channelType: String = RoomType.CHANNEL
    private var isChannelReadOnly: Boolean = false
    private var memberList = arrayListOf<Contact>()

    // WIDECHAT
    private var widechatSearchView: SearchView? = null
    private lateinit var selectedContactsRecyclerView: RecyclerView
    private lateinit var selectedContactsAdapter: RecyclerView.Adapter<*>

    companion object {
        fun newInstance(members: ArrayList<Contact>? = null): CreateChannelFragment {
            return CreateChannelFragment().apply {
                arguments = Bundle(1).apply {
                    putParcelableArrayList(BUNDLE_CREATE_CHANNEL_MEMBERS, members)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(
            if (Constants.WIDECHAT)
                R.layout.fragment_create_group
            else
                R.layout.fragment_create_channel
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolBar()
        if (!Constants.WIDECHAT) {
            setupViewListeners()
            setupRecyclerView()
            subscribeEditTexts()
        } else {
            // WIDECHAT - remove options for public rooms, read only, invite members and member chips
            setupWidechatView()
            selectedContactsAdapter = SelectedContactsAdapter(memberList, false) {}
            selectedContactsRecyclerView = selected_contacts_recycler_view.apply {
                setHasFixedSize(true)
                val displayMetrics = context.resources.displayMetrics
                val dpWidth = displayMetrics.widthPixels / displayMetrics.density
                val nCols = ((dpWidth - 40) / 60).toInt()
                layoutManager = GridLayoutManager(context, nCols)
                adapter = selectedContactsAdapter
            }
        }
        val bundle = arguments
        if (bundle != null) {
            val members = bundle.getParcelableArrayList<Contact>(BUNDLE_CREATE_CHANNEL_MEMBERS)
            members?.forEach {
                processSelectedMember(it)
            }
            selectedContactsAdapter.notifyDataSetChanged()
        }
        analyticsManager.logScreenView(ScreenViewEvent.CreateChannel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unsubscribeEditTexts()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.create_channel, menu)
        mode.title = getString(R.string.title_create_channel)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_create_channel -> {
                createChannelPresenter.createChannel(
                    roomTypeOf(channelType),
                    text_channel_name.text.toString(),
                    memberList.map { it.getUsername()!! },
                    isChannelReadOnly
                )
                mode.finish()
                true
            }
            else -> {
                false
            }
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
    }

    override fun showLoading() {
        ui {
            if (Constants.WIDECHAT)
                widechat_view_loading.isVisible = true
            else
                view_loading.isVisible = true
        }
    }

    override fun hideLoading() {
        ui {
            if (Constants.WIDECHAT)
                widechat_view_loading.isVisible = false
            else
                view_loading.isVisible = false
        }
    }

    override fun showMessage(resId: Int) {
        ui {
            showToast(resId)
        }
    }

    override fun showMessage(message: String) {
        ui {
            showToast(message)
        }
    }

    override fun showGenericErrorMessage() {
        showMessage(getString(R.string.msg_generic_error))
    }

    override fun showUserSuggestion(dataSet: List<MemberUiModel>) {
        adapter.clearData()
        adapter.prependData(dataSet)
        text_member_not_found.isVisible = false
        recycler_view.isVisible = true
        view_member_suggestion.isVisible = true
    }

    override fun showNoUserSuggestion() {
        recycler_view.isVisible = false
        text_member_not_found.isVisible = true
        view_member_suggestion.isVisible = true
    }

    override fun showSuggestionViewInProgress() {
        recycler_view.isVisible = false
        text_member_not_found.isVisible = false
        view_member_suggestion_loading.isVisible = true
        view_member_suggestion.isVisible = true
    }

    override fun hideSuggestionViewInProgress() {
        view_member_suggestion_loading.isVisible = false
    }

    override fun prepareToShowChatList() {
        with(activity as MainActivity) {
            // WIDECHAT - no nav drawer in widechat client
            if (Constants.WIDECHAT) {
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                createChannelPresenter.toChatList()
            } else {
                setCheckedNavDrawerItem(R.id.menu_action_chats)
                openDrawer()
                getDrawerLayout().postDelayed(1000) {
                    closeDrawer()
                    createChannelPresenter.toChatList()
                }
            }
        }
    }

    override fun showChannelCreatedSuccessfullyMessage() {
        showMessage(getString(R.string.msg_channel_created_successfully))
    }

    override fun enableUserInput() {
        if (Constants.WIDECHAT) {
            text_group_name.isEnabled = true
        } else {
            text_channel_name.isEnabled = true
            text_invite_members.isEnabled = true
        }
    }

    override fun disableUserInput() {
        if (Constants.WIDECHAT) {
            text_group_name.isEnabled = false
        } else {
            text_channel_name.isEnabled = false
            text_invite_members.isEnabled = false
        }
    }

    private fun setupToolBar() {
        if (Constants.WIDECHAT) {
            with((activity as AppCompatActivity?)?.supportActionBar) {
                widechatSearchView = this?.getCustomView()?.findViewById(R.id.action_widechat_search)
                widechatSearchView?.visibility = View.GONE
                this?.setDisplayShowTitleEnabled(true)
                this?.title = getString(R.string.title_create_group)
            }
            (activity as MainActivity).toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
            (activity as MainActivity).toolbar.setNavigationOnClickListener {
                activity?.onBackPressed()
            }
        } else {
            with((activity as AppCompatActivity?)?.supportActionBar) {
                this?.setDisplayShowTitleEnabled(true)
                this?.title = getString(R.string.title_create_channel)
            }
        }
    }

    private fun setupWidechatView() {
        channelType = RoomType.PRIVATE_GROUP
        val channelNameDisposable = text_group_name.asObservable()
                .subscribe {
                    create_group_fab.isVisible = it.isNotBlank()
                }
        compositeDisposable.add(channelNameDisposable)
        create_group_fab.setOnClickListener {
            createChannelPresenter.createChannel(
                    roomTypeOf(channelType),
                    text_group_name.text.toString(),
                    memberList.map { it.getUsername()!! },
                    isChannelReadOnly
            )
        }
    }

    private fun setupViewListeners() {
        switch_channel_type.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                text_channel_type.text = getString(R.string.msg_private_channel)
                text_channel_type_description.text =
                    getString(R.string.msg_private_channel_description)
                image_channel_icon.setImageDrawable(
                    context?.getDrawable(R.drawable.ic_lock_black_12_dp)
                )
                channelType = RoomType.PRIVATE_GROUP
            } else {
                text_channel_type.text = getString(R.string.msg_public_channel)
                text_channel_type_description.text =
                    getString(R.string.msg_public_channel_description)
                image_channel_icon.setImageDrawable(
                    context?.getDrawable(R.drawable.ic_hashtag_black_12dp)
                )
                channelType = RoomType.CHANNEL
            }
        }

        switch_read_only.setOnCheckedChangeListener { _, isChecked ->
            isChannelReadOnly = isChecked
        }
    }

    private fun setupRecyclerView() {
        ui {
            recycler_view.layoutManager =
                    LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            recycler_view.addItemDecoration(
                DividerItemDecoration(it, DividerItemDecoration.HORIZONTAL)
            )
            recycler_view.adapter = adapter
        }
    }

    private fun subscribeEditTexts() {
        val channelNameDisposable = text_channel_name.asObservable()
            .subscribe {
                if (it.isNotBlank()) {
                    startActionMode()
                } else {
                    finishActionMode()
                }
            }

        val inviteMembersDisposable = text_invite_members.asObservable()
            .debounce(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .filter { t -> t.isNotBlank() }
            .subscribe {
                if (it.length >= 3) {
                    createChannelPresenter.searchUser(it.toString())
                } else {
                    view_member_suggestion.isVisible = false
                }
            }

        compositeDisposable.addAll(channelNameDisposable, inviteMembersDisposable)
    }

    private fun unsubscribeEditTexts() {
        compositeDisposable.dispose()
    }

    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(this)
        }
    }

    private fun finishActionMode() {
        actionMode?.finish()
    }

    private fun processSelectedMember(member: Contact) {
        val username = member.getUsername()!!
        if (memberList.map { it.getUsername() }.contains(username)) {
            showMessage(getString(R.string.msg_member_already_added))
        } else {
            if (!Constants.WIDECHAT) {
                view_member_suggestion.isVisible = false
                text_invite_members.setText("")
                addChip(username)
                chip_group_member.isVisible = true
            }
            addMember(member)
        }
    }

    private fun addMember(member: Contact) {
        memberList.add(member)
    }

    private fun removeMember(member: Contact) {
        memberList.remove(member)
    }

    private fun addChip(chipText: String) {
        val chip = Chip(context)
        chip.chipText = chipText
        chip.isCloseIconEnabled = true
        chip.setChipBackgroundColorResource(R.color.icon_grey)
        setupChipOnCloseIconClickListener(chip)
        chip_group_member.addView(chip)
    }

    private fun setupChipOnCloseIconClickListener(chip: Chip) {
        chip.setOnCloseIconClickListener {
            removeChip(it)
            val username = (it as Chip).chipText.toString()
            val member: Contact? = memberList.find { it.getUsername() == username }
            removeMember(member!!)
            // whenever we remove a chip we should process the chip group visibility.
            processChipGroupVisibility()
        }
    }

    private fun removeChip(chip: View) {
        chip_group_member.removeView(chip)
    }

    private fun processChipGroupVisibility() {
        chip_group_member.isVisible = memberList.isNotEmpty()
    }
}