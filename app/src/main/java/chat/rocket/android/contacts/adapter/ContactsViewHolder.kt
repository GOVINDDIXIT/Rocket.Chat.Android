package chat.rocket.android.contacts.adapter

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.selection.ItemDetailsLookup
import chat.rocket.android.R
import chat.rocket.android.chatrooms.adapter.ViewHolder
import chat.rocket.android.contacts.models.Contact
import kotlinx.android.synthetic.main.item_contact.view.*

class ContactsViewHolder(itemView: View) : ViewHolder<ContactsItemHolder>(itemView) {

    private val resources: Resources = itemView.resources
    private val online: Drawable = resources.getDrawable(R.drawable.ic_status_online_12dp)
    private val away: Drawable = resources.getDrawable(R.drawable.ic_status_away_12dp)
    private val busy: Drawable = resources.getDrawable(R.drawable.ic_status_busy_12dp)
    private val offline: Drawable = resources.getDrawable(R.drawable.ic_status_invisible_12dp)

    override fun bindViews(data: ContactsItemHolder) {
        val contact: Contact = data.data
        with(itemView) {
            contact_image_avatar.setImageURI(contact.getAvatarUrl())
            contact_name.text = contact.getName()

            isActivated = false
            contact_status.isGone = true
            contact_checkbox.isGone = true

            if (contact.getUsername() == null) {
                contact_detail.isVisible = true
                invite_contact.isVisible = true
                chat_username.isGone = true

                if (contact.isPhone()) {
                    contact_detail.text = contact.getPhoneNumber()
                } else {
                    contact_detail.text = contact.getEmailAddress()
                }

            } else {
                contact_detail.isGone = true
                invite_contact.isGone = true
                chat_username.isVisible = true
                chat_username.text = "@${contact.getUsername()}"
            }

            if(contact.getIsSpotlightResult()) {
                chat_username.isVisible = false
                chat_username.text = ""
                invite_contact.isVisible = false
            }
        }
    }

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int = adapterPosition
                override fun getSelectionKey(): Long? = itemId
            }

    fun bindSelection(isActivated: Boolean = false) {
        with(itemView) {
            this.isActivated = isActivated
            contact_status.isVisible = !isActivated
            contact_checkbox.isVisible = isActivated
        }
    }

    fun setContactStatus(contact: Contact?) {
        contact?.getStatus()?.let {
            itemView.contact_status.isVisible = true
            itemView.contact_status.setImageDrawable(getStatusDrawable(it))
        }
    }

    private fun getStatusDrawable(status: String?): Drawable {
        if (status == null) {
            return offline
        }
        return when(status) {
            "online" -> online
            "away" -> away
            "busy" -> busy
            else -> offline
        }
    }
}
