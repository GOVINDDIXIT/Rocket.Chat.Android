package chat.rocket.android.userdetails.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import chat.rocket.android.R
import chat.rocket.android.chatroom.ui.chatRoomIntent
import chat.rocket.android.emoji.internal.GlideApp
import chat.rocket.android.userdetails.presentation.UserDetailsPresenter
import chat.rocket.android.userdetails.presentation.UserDetailsView
import chat.rocket.android.util.extension.orFalse
import chat.rocket.android.util.extensions.showToast
import chat.rocket.common.model.roomTypeOf
import chat.rocket.core.model.ChatRoom
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_user_details.*
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToLong

fun Context.userDetailsIntent(userId: String): Intent {
    return Intent(this, UserDetailsActivity::class.java).apply {
        putExtra(EXTRA_USER_ID, userId)
    }
}

const val EXTRA_USER_ID = "EXTRA_USER_ID"

class UserDetailsActivity : AppCompatActivity(), UserDetailsView, HasSupportFragmentInjector {

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>
    @Inject
    lateinit var presenter: UserDetailsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_details)
        setupToolbar()

        val userId = intent.getStringExtra(EXTRA_USER_ID)
        presenter.loadUserDetails(userId = userId)
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentDispatchingAndroidInjector

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun showUserDetails(
        avatarUrl: String?,
        username: String?,
        name: String?,
        utcOffset: Float?,
        status: String,
        chatRoom: ChatRoom?
    ) {
        text_view_name.text = name
        text_view_username.text = username
        text_view_status.text = status.capitalize()
        GlideApp.with(this)
            .load(avatarUrl)
            .centerCrop()
            .apply(RequestOptions().transform(RoundedCorners(25)))
            .into(image_view_avatar)

        utcOffset?.let {
            val offsetLong = it.roundToLong()
            val offset = if (it > 0) "+$offsetLong" else offsetLong.toString()
            val formatter = DateTimeFormatter.ofPattern("'(GMT$offset)' hh:mm a")
            val zoneId = ZoneId.systemDefault()
            val timeNow = OffsetDateTime.now(ZoneOffset.UTC).plusHours(offsetLong).toLocalDateTime()
            text_view_tz.text = formatter.format(ZonedDateTime.of(timeNow, zoneId))
        }

        text_view_message.setOnClickListener {
            toDirectMessage(chatRoom = chatRoom)
        }

        image_view_message.setOnClickListener {
            toDirectMessage(chatRoom = chatRoom)
        }
    }

    private fun toDirectMessage(chatRoom: ChatRoom?) {
        chatRoom?.let { c ->
            startActivity(
                chatRoomIntent(
                    chatRoomId = c.id,
                    chatRoomName = c.name,
                    chatRoomType = c.type.toString(),
                    isReadOnly = c.readonly.orFalse(),
                    chatRoomLastSeen = c.lastSeen ?: 0,
                    isSubscribed = c.open,
                    isCreator = false,
                    isFavorite = c.favorite
                )
            )
        }
    }
}
