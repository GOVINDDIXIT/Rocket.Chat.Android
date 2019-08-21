package chat.rocket.android.dynamiclinks

import android.content.Context
import android.content.Intent
import android.net.Uri
import chat.rocket.android.util.TimberLogger
import chat.rocket.android.R
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ShortDynamicLink
import javax.inject.Inject
import timber.log.Timber

class DynamicLinksForFirebase @Inject constructor(private var context: Context) :
        DynamicLinks {

    private var deepLink: Uri? = null
    private var newDeepLink: String? = null

    override fun getDynamicLink(intent: Intent, deepLinkCallback: (Uri?) -> Unit?) {

        FirebaseDynamicLinks.getInstance()
            .getDynamicLink(intent)
            .addOnSuccessListener { pendingDynamicLinkData ->
                if (pendingDynamicLinkData != null) {
                    deepLink = pendingDynamicLinkData.link
                }
                deepLinkCallback(deepLink)
            }
            .addOnFailureListener { e -> TimberLogger.debug("getDynamicLink:onFailure : $e") }
    }

    override fun createDynamicLink(username: String, server: String, deepLinkCallback: (String?) -> Unit? ) {

        FirebaseDynamicLinks.getInstance().createDynamicLink()
            .setLink(Uri.parse("$server/direct/$username"))
            .setDomainUriPrefix("https://" + context.getString(R.string.widechat_deeplink_host))
            .setAndroidParameters(
                    DynamicLink.AndroidParameters.Builder(context.getString(R.string.widechat_package_name)).build())
            .setSocialMetaTagParameters(
                    DynamicLink.SocialMetaTagParameters.Builder()
                            .setTitle(username)
                            .setDescription(String.format(context.getString(R.string.chat_with), username, " ") + context.getString(R.string.app_name))
                            .build())
            .buildShortDynamicLink(ShortDynamicLink.Suffix.SHORT)
            .addOnSuccessListener { result ->
                newDeepLink = result.shortLink.toString()
                Timber.d("New deeplink created: ${newDeepLink}")

            }.addOnFailureListener {
                // Error
                Timber.d("Error creating dynamic link.")

            }.addOnCompleteListener {
                deepLinkCallback(newDeepLink)
            }
    }

}


