package chat.rocket.android.analytics

import chat.rocket.android.analytics.event.AuthenticationEvent
import chat.rocket.android.analytics.event.ScreenViewEvent
import chat.rocket.android.analytics.event.SubscriptionTypeEvent
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.crashlytics.android.answers.LoginEvent
import com.crashlytics.android.answers.SignUpEvent

class AnswersAnalytics : Analytics {

    override fun logLogin(event: AuthenticationEvent, loginSucceeded: Boolean) =
        Answers.getInstance()
            .logLogin(
                LoginEvent()
                    .putMethod(event.methodName)
                    .putSuccess(loginSucceeded)
            )

    override fun logSignUp(event: AuthenticationEvent, signUpSucceeded: Boolean) =
        Answers.getInstance()
            .logSignUp(
                SignUpEvent()
                    .putMethod(event.methodName)
                    .putSuccess(signUpSucceeded)
            )


    override fun logScreenView(event: ScreenViewEvent) =
        Answers.getInstance()
            .logCustom(CustomEvent("screen_view").putCustomAttribute("screen", event.screenName))


    override fun logMessageSent(messageType: String, serverUrl: String) =
        Answers.getInstance()
            .logCustom(
                CustomEvent("message_actionsent")
                    .putCustomAttribute("message_type", messageType)
                    .putCustomAttribute("server", serverUrl)
            )

    override fun logSendMessageException(countToSend: Int, exceptionDescription: String, serverUrl: String) =
        Answers.getInstance()
            .logCustom(
                    CustomEvent("send_message_exception")
                            .putCustomAttribute("description", exceptionDescription)
                            .putCustomAttribute("count_to_send", countToSend)
                            .putCustomAttribute("server", serverUrl)
            )

    override fun logConnectionStateChange(previousState: String, newState: String, serverUrl: String) {

        Answers.getInstance()
            .logCustom(
                    CustomEvent("connection_state_change")
                            .putCustomAttribute("previous_state", previousState)
                            .putCustomAttribute("new_state", newState)
                            .putCustomAttribute("server", serverUrl)
            )
    }

    override fun logMediaUploaded(event: SubscriptionTypeEvent, mimeType: String) =
        Answers.getInstance()
            .logCustom(
                CustomEvent("media_upload")
                    .putCustomAttribute("subscription_type", event.subscriptionTypeName)
                    .putCustomAttribute("media_type", mimeType)
            )


    override fun logReaction(event: SubscriptionTypeEvent) =
        Answers.getInstance()
            .logCustom(
                CustomEvent("reaction")
                    .putCustomAttribute("subscription_type", event.subscriptionTypeName)
            )


    override fun logServerSwitch(serverUrl: String, serverCount: Int) =
        Answers.getInstance()
            .logCustom(
                CustomEvent("server_switch")
                    .putCustomAttribute("server_url", serverUrl)
                    .putCustomAttribute("server_count", serverCount)
            )

    override fun logOpenAdmin() = Answers.getInstance().logCustom(CustomEvent("open_admin"))

    override fun logResetPassword(resetPasswordSucceeded: Boolean) =
        Answers.getInstance()
            .logCustom(
                CustomEvent("reset_password")
                    .putCustomAttribute("resetPasswordSucceeded", resetPasswordSucceeded.toString())
            )

    override fun logVideoConference(event: SubscriptionTypeEvent, serverUrl: String) =
        Answers.getInstance()
            .logCustom(
                CustomEvent("video_conference")
                    .putCustomAttribute("subscription_type", event.subscriptionTypeName)
                    .putCustomAttribute("server", serverUrl)
            )

    override fun logStorage(freeBytes: String, totalBytes: String) =
        Answers.getInstance()
            .logCustom(
                CustomEvent("log_storage")
                    .putCustomAttribute("free_bytes", freeBytes)
                    .putCustomAttribute("total_bytes", totalBytes)
            )

    override fun logMessageActionAddReaction() = Answers.getInstance().logCustom(CustomEvent("message_action_add_reaction"))

    override fun logMessageActionReply() = Answers.getInstance().logCustom(CustomEvent("message_action_reply"))

    override fun logMessageActionQuote() = Answers.getInstance().logCustom(CustomEvent("message_action_quote"))

    override fun logMessageActionPermalink() = Answers.getInstance().logCustom(CustomEvent("message_action_permalink"))

    override fun logMessageActionCopy() = Answers.getInstance().logCustom(CustomEvent("message_action_copy"))

    override fun logMessageActionEdit() = Answers.getInstance().logCustom(CustomEvent("message_action_edit"))

    override fun logMessageActionInfo() = Answers.getInstance().logCustom(CustomEvent("message_action_info"))

    override fun logMessageActionStar() = Answers.getInstance().logCustom(CustomEvent("message_action_star"))

    override fun logMessageActionPin() = Answers.getInstance().logCustom(CustomEvent("message_action_pin"))

    override fun logMessageActionReport() = Answers.getInstance().logCustom(CustomEvent("message_action_report"))

    override fun logMessageActionDelete() = Answers.getInstance().logCustom(CustomEvent("message_action_delete"))
}
