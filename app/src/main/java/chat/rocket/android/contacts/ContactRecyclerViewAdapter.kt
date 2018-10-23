package chat.rocket.android.contacts

import android.content.Context
import timber.log.Timber
import android.view.LayoutInflater
import android.view.View
import android.provider.ContactsContract
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import chat.rocket.android.R
import chat.rocket.android.contacts.models.Contact
import com.facebook.drawee.view.DraweeView
import java.util.*
import kotlin.collections.HashMap

class ContactRecyclerViewAdapter(
        private val context: Context,
        private val contactArrayList: ArrayList<Contact>,
        private val contactHashMap: HashMap<String, String>
) : RecyclerView.Adapter<ContactRecyclerViewAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return contactArrayList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.contact = contactArrayList[position]
        holder.status = contactHashMap.get(holder.contact!!.getPhoneNumber())
        try {
            holder.contactName.text = holder.contact!!.getName()
            holder.phoneNumber.text = holder.contact!!.getPhoneNumber()
        } catch (exception: NullPointerException) {
            Timber.e("Failed to send resolution. Exception is: $exception")
        }

    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        var contact: Contact? = null
        var status: String? = null

        var contactName: TextView
        var phoneNumber: TextView

        init {
            this.view.setOnClickListener(this)
            this.contactName = view.findViewById(R.id.contact_name) as TextView
            this.phoneNumber = view.findViewById(R.id.phone_number) as TextView
        }

        override fun onClick(view: View) {
                Toast.makeText(
                        context,
                        "Contact was clicked: ${this.contact!!.getName()!!}",
                        Toast.LENGTH_LONG
                ).show()
//            Handle the click on the contact

        }
    }
}
