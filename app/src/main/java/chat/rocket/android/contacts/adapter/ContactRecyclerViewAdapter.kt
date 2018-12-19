package chat.rocket.android.contacts.adapter

import android.content.Intent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import chat.rocket.android.R
import chat.rocket.android.chatrooms.adapter.*
import chat.rocket.android.main.ui.MainActivity
import chat.rocket.android.util.extensions.inflate
import kotlin.collections.HashMap


class ContactRecyclerViewAdapter(
        private val context: MainActivity,
        private val contactArrayList: List<ItemHolder<*>>,
        private val contactHashMap: HashMap<String, String>


) : RecyclerView.Adapter<ViewHolder<*>>() {

    init {
        setHasStableIds(true)
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<*> {
        return when (viewType) {
            VIEW_TYPE_CONTACT -> {
                val view = parent.inflate(R.layout.item_contact)
                ContactViewHolder(view)
            }
            VIEW_TYPE_HEADER -> {
                val view = parent.inflate(R.layout.item_heading)
                ContactHeaderViewHolder(view)
            }
            VIEW_TYPE_LOADING -> {
                val view = parent.inflate(R.layout.item_loading)
                LoadingViewHolder(view)
            }
            VIEW_TYPE_INVITE -> {
                val view = parent.inflate(R.layout.item_invite)
                InviteViewHolder(view)
            }
            else -> throw IllegalStateException("View type must be either Room, Header or Loading")
        }
    }

    override fun getItemCount() = contactArrayList.size

    override fun getItemId(position: Int): Long {
        val item = contactArrayList[position]
        return when (item) {
            is ContactItemHolder -> item.data.hashCode().toLong()
            is ContactHeaderItemHolder -> item.data.hashCode().toLong()
            is LoadingItemHolder -> "loading".hashCode().toLong()
            is inviteItemHolder -> item.data.hashCode().toLong()
            else -> throw IllegalStateException("View type must be either Room, Header or Loading")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (contactArrayList[position]) {
            is ContactItemHolder -> VIEW_TYPE_CONTACT
            is ContactHeaderItemHolder -> VIEW_TYPE_HEADER
            is LoadingItemHolder -> VIEW_TYPE_LOADING
            is inviteItemHolder -> VIEW_TYPE_INVITE
            else -> throw IllegalStateException("View type must be either Room, Header or Loading")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder<*>, position: Int) {
        if (holder is ContactViewHolder) {
            holder.bind(contactArrayList[position] as ContactItemHolder)
            holder.itemView.setOnClickListener {
               // it.context.startActivity(Intent.createChooser(this, context.getString(R.string.msg_share_using)))

            }

        } else if (holder is ContactHeaderViewHolder) {
            holder.bind(contactArrayList[position] as ContactHeaderItemHolder)
        }else if (holder is InviteViewHolder) {
            holder.bind(contactArrayList[position] as inviteItemHolder)
            holder.itemView.setOnClickListener {
                shareApp();
            }

        }
    }


    private fun shareApp() {
       with(Intent(Intent.ACTION_SEND)) {
           type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.msg_check_this_out))
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.play_store_link))
           context.startActivity(Intent.createChooser(this, context.getString(R.string.msg_share_using)))
       }
   }

    companion object {
        const val VIEW_TYPE_CONTACT = 1
        const val VIEW_TYPE_HEADER = 2
        const val VIEW_TYPE_LOADING = 3
        const val VIEW_TYPE_INVITE = 4

    }

}
//    override fun getItemCount(): Int {
//        return contactArrayList.size
//    }

//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        return when (viewType) {
//            Contact.CARD_TYPE.VIEW_HEADING-> HeadingViewHolder(parent.inflate(R.layout.item_heading))
//            Contact.CARD_TYPE.VIEW_CONTACT -> ContactViewHolder(parent.inflate(R.layout.item_contact))
//            Contact.CARD_TYPE.VIEW_INVITE_OTHER_APP -> inviteViewHolder(parent.inflate(R.layout.item_invite))
//            else -> ContactViewHolder(parent.inflate(R.layout.item_contact))
//        }
//    }
//
//        override fun getItemViewType(position: Int): Int {
//            return contactArrayList.get(position)!!.getType()!!;
//        }
//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        val viewType = getItemViewType(position)
//        when (viewType) {
//            Contact.CARD_TYPE.VIEW_CONTACT  -> contactData(holder,position)
//            Contact.CARD_TYPE.VIEW_INVITE_OTHER_APP -> inviteData(holder, position)
//            Contact.CARD_TYPE.VIEW_HEADING -> HeadingData(holder,position)
//            else -> contactData(holder,position)
//        }
//    }
//
//    private fun inviteData(holder: RecyclerView.ViewHolder, position: Int) {
//    }
//
//
//    private fun contactData(holder:RecyclerView.ViewHolder,  position: Int) {
//        val contactCardViewHolder = holder as ContactViewHolder
//        contactCardViewHolder.contact = contactArrayList[position]
//        try {
//            if(contactCardViewHolder.contact!!.getUsername()==null){
//                contactCardViewHolder.inviteButton.visibility = View.VISIBLE
//                contactCardViewHolder.emailDetail.visibility=View.GONE
//            }else{
//                contactCardViewHolder.inviteButton.visibility = View.GONE
//                contactCardViewHolder.emailDetail.visibility=View.VISIBLE
//                contactCardViewHolder.emailDetail.text= "@"+holder.contact!!.getUsername()
//            }
//            contactCardViewHolder.contactName.text = holder.contact!!.getName()
//            if (contactCardViewHolder.contact!!.isPhone()) {
//                contactCardViewHolder.phoneNumber.text = holder.contact!!.getPhoneNumber()
//            } else {
//                contactCardViewHolder.phoneNumber.text = holder.contact!!.getEmailAddress()
//            }
//            contactCardViewHolder.inviteButton.setOnClickListener { view ->
//                run {
//                    // Make API call using context.presenter
//
//
      //            context.startActivity(Intent.createChooser(this, context.getString(R.string.msg_share_using)))//                    if(contactCardViewHolder.contact!!.isPhone()){
////                        context.presenter.inviteViaSMS(contactCardViewHolder.contact!!.getPhoneNumber()!!);
////                    }else{
////                        context.presenter.inviteViaEmail(contactCardViewHolder.contact!!.getEmailAddress()!!);
////                    }
//                }
//            }
//            contactCardViewHolder.imageAvtar.setImageURI(holder.contact!!.getAvatarUrl())
//        } catch (exception: NullPointerException) {
//            Timber.e("Failed to send resolution. Exception is: $exception")
//        }
//    }
//
//    fun toChatRoom(
//            chatRoomId: String,
//            chatRoomName: String,
//            chatRoomType: String,
//            isReadOnly: Boolean,
//            chatRoomLastSeen: Long,
//            isSubscribed: Boolean,
//            isCreator: Boolean,
//            isFavorite: Boolean
//    ) {
//        activity.startActivity(
//                activity.chatRoomIntent(
//                        chatRoomId,
//                        chatRoomName,
//                        chatRoomType,
//                        isReadOnly,
//                        chatRoomLastSeen,
//                        isSubscribed,
//                        isCreator,
//                        isFavorite
//                )
//        )
//        activity.overridePendingTransition(R.anim.open_enter, R.anim.open_exit)
//    }
//
//    private fun HeadingData(holder:RecyclerView.ViewHolder,  position: Int) {
//        val headingViewHolder = holder as HeadingViewHolder
//        headingViewHolder.contact = contactArrayList[position]
//        headingViewHolder.contactHeading.text=headingViewHolder.contact!!.getUsername()
//    }
//
//
//
//    inner class ContactViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
//        var contact: Contact? = null
//        var status: String? = null
//        var contactName: TextView
//        var phoneNumber: TextView
//        var inviteButton: Button
//        var emailDetail:TextView
//        var imageAvtar: SimpleDraweeView
//
//        init {
//            this.contactName = view.findViewById(R.id.contact_name) as TextView
//            this.phoneNumber = view.findViewById(R.id.contact_detail) as TextView
//            this.inviteButton = view.findViewById(R.id.invite_contact) as Button
//            this.emailDetail = view.findViewById(R.id.text_email) as TextView
//            this.imageAvtar = view.findViewById(R.id.image_avatar) as SimpleDraweeView
//        }
//    }
//
//    inner class inviteViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
//        var contact: Contact? = null
//        var layout: LinearLayout
//        init {
//            this.layout=view.findViewById(R.id.ll_invite)as LinearLayout
//            this.layout.setOnClickListener { view ->
//                run {
//                    shareApp();
//                }
//            }
//        }
//    }
//
//    inner class HeadingViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
//        var contact: Contact? = null
//        var contactHeading: TextView
//        init {
//            this.contactHeading = view.findViewById(R.id.contacts_heading) as TextView
//        }
//    }
//
//    private fun shareApp() {
//        with(Intent(Intent.ACTION_SEND)) {
//            type = "text/plain"
//            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.msg_check_this_out))
//            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.play_store_link))
//           context.startActivity(Intent.createChooser(this, context.getString(R.string.msg_share_using)))
//        }
//    }
//}
