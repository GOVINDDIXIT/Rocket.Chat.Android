package chat.rocket.android.userdetails.presentation

import chat.rocket.android.chatroom.presentation.ChatRoomNavigator
import chat.rocket.android.chatrooms.domain.FetchChatRoomsInteractor
import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.db.DatabaseManager
import chat.rocket.android.db.model.ChatRoomEntity
import chat.rocket.android.db.model.UserEntity
import chat.rocket.android.server.domain.*
import chat.rocket.android.server.infraestructure.ConnectionManagerFactory
import chat.rocket.android.util.extension.launchUI
import chat.rocket.android.util.extensions.avatarUrl
import chat.rocket.android.util.retryIO
import chat.rocket.common.model.RoomType
import chat.rocket.common.model.roomTypeOf
import chat.rocket.common.util.ifNull
import chat.rocket.core.internal.rest.chatRoomRoles
import chat.rocket.core.internal.rest.createDirectMessage
import chat.rocket.core.internal.rest.kick
import chat.rocket.core.model.ChatRoomRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class UserDetailsPresenter @Inject constructor(
    private val view: UserDetailsView,
    private val dbManager: DatabaseManager,
    private val strategy: CancelStrategy,
    private val navigator: ChatRoomNavigator,
	private val permissionsInteractor: PermissionsInteractor,
    tokenRepository: TokenRepository,
    settingsInteractor: GetSettingsInteractor,
    serverInteractor: CurrentServerRepository,
    factory: ConnectionManagerFactory
) {
    private var currentServer = serverInteractor.get()!!
    private val manager = factory.create(currentServer)
    private val client = manager.client
    private val interactor = FetchChatRoomsInteractor(client, dbManager)
    private val settings = settingsInteractor.get(currentServer)
    private lateinit var userEntity: UserEntity

    fun loadUserDetails(userId: String) {
        launchUI(strategy) {
            try {
                view.showLoading()
                dbManager.getUser(userId)?.let {
                    userEntity = it
                    val avatarUrl =
                        userEntity.username?.let { username -> currentServer.avatarUrl(avatar = username) }
                    val username = userEntity.username
                    val name = userEntity.name
                    val utcOffset =
                        userEntity.utcOffset // TODO Convert UTC and display like the mockup

                    if (avatarUrl != null && username != null && name != null && utcOffset != null) {
                        view.showUserDetailsAndActions(
                            avatarUrl = avatarUrl,
                            name = name,
                            username = username,
                            status = userEntity.status,
                            utcOffset = utcOffset.toString(),
                            isVideoCallAllowed = settings.isJitsiEnabled()
                        )
                    } else {
                        throw Exception()
                    }
                }
            } catch (ex: Exception) {
                Timber.e(ex)
                ex.message?.let {
                    view.showMessage(it)
                }.ifNull {
                    view.showGenericErrorMessage()
                }
            } finally {
                view.hideLoading()
            }
        }
    }

    fun createDirectMessage(username: String) {
        launchUI(strategy) {
            try {
                view.showLoading()

                withContext(Dispatchers.Default) {
                    val directMessage = retryIO("createDirectMessage($username") {
                        client.createDirectMessage(username)
                    }

                    val chatRoomEntity = ChatRoomEntity(
                        id = directMessage.id,
                        name = userEntity.username ?: userEntity.name.orEmpty(),
                        description = null,
                        type = RoomType.DIRECT_MESSAGE,
                        fullname = userEntity.name,
                        subscriptionId = "",
                        updatedAt = directMessage.updatedAt
                    )

                    dbManager.insertOrReplaceRoom(chatRoomEntity)

                    interactor.refreshChatRooms()

                    navigator.toChatRoom(
                        chatRoomId = chatRoomEntity.id,
                        chatRoomName = chatRoomEntity.name,
                        chatRoomType = chatRoomEntity.type,
                        isReadOnly = false,
                        chatRoomLastSeen = -1,
                        isSubscribed = chatRoomEntity.open,
                        isCreator = true,
                        isFavorite = false
                    )
                }
            } catch (ex: Exception) {
                Timber.e(ex)
                ex.message?.let {
                    view.showMessage(it)
                }.ifNull {
                    view.showGenericErrorMessage()
                }
            } finally {
                view.hideLoading()
            }
        }
    }

    fun toVideoConference(username: String) {
        launchUI(strategy) {
            try {
                withContext(Dispatchers.Default) {
                    val directMessage = retryIO("createDirectMessage($username") {
                        client.createDirectMessage(username)
                    }
                    navigator.toVideoConference(directMessage.id, RoomType.DIRECT_MESSAGE)
                }
            } catch (ex: Exception) {
                Timber.e(ex)
                ex.message?.let {
                    view.showMessage(it)
                }.ifNull {
                    view.showGenericErrorMessage()
                }
            }
        }
    }

	fun removeUser(userId: String, chatRoomId: String) {
		launchUI(strategy) {
			try {
				dbManager.getRoom(chatRoomId)?.let {
					val result = retryIO("kick($chatRoomId,${roomTypeOf(it.chatRoom.type)},$userId)") {
						client.kick(chatRoomId, roomTypeOf(it.chatRoom.type), userId)
					}
					if (result) {
						view.showUserRemovedMessage()
					}
				}.ifNull {
					Timber.e("Couldn't find a room with id: $chatRoomId at current server.")
				}
			} catch (exception: Exception) {
				Timber.e(exception)
				exception.message?.let {
					view.showMessage(it)
				}.ifNull {
					view.showGenericErrorMessage()
				}
			}
		}
	}

	fun checkRemoveUserPermission(chatRoomId: String) {
		launchUI(strategy) {
			if (hasRemoveUserPermission(chatRoomId)) {
				view.showRemoveUserButton()
			} else {
				view.hideRemoveUserButton()
			}
		}
	}

	private suspend fun hasRemoveUserPermission(chatRoomId: String): Boolean {
		return permissionsInteractor.hasPermission(REMOVE_USER, chatRoomId)
	}
}

