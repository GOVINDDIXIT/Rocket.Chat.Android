package chat.rocket.android.members.presentation

import chat.rocket.android.chatroom.ui.ChatRoomActivity
import chat.rocket.android.userdetails.ui.userDetailsIntent

class MembersNavigator(internal val activity: ChatRoomActivity) {

    fun toMemberDetails(userId: String, avatarUri: String, realName: String, username: String, email: String, utcOffset: String) {
        activity.apply {
            startActivity(this.userDetailsIntent(userId, ""))
        }
    }
}
