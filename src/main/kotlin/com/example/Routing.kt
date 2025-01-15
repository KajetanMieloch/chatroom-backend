package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun Application.configureRouting() {
    routing {
        get("/") { call.respondText("Hello World!") }

        post("/createRoom") { handleCreateRoom(call) }
        get("/getRooms") { handleGetRooms(call) }
        post("/sendMessage") { handleSendMessage(call) }
        get("/getMessages") { handleGetMessages(call) }
        post("/changeUsername") { handleChangeUsername(call) }
    }
}

// Funkcje obsługi tras
suspend fun handleCreateRoom(call: ApplicationCall) {
    try {
        val request = call.receive<CreateRoomRequest>()
        val roomId = generateRoomId()
        val passwordHash = request.password?.let { hashPassword(it) }

        val rooms = loadRooms()
        val users = loadUsers()

        rooms[roomId] = Room(name = request.roomName, passwordHash = passwordHash)
        users.getOrPut(request.userId) { UserRooms() }.rooms.add(roomId)

        saveRooms(rooms)
        saveUsers(users)

        call.respond(CreateRoomResponse("success", request.roomName, "Room created successfully!"))
    } catch (e: Exception) {
        call.application.log.error("Error creating room: ${e.localizedMessage}", e)
        call.respond(HttpStatusCode.BadRequest, "Invalid Request")
    }
}

suspend fun handleGetRooms(call: ApplicationCall) {
    try {
        val userId = call.request.queryParameters["userId"] ?: return call.respond(
            HttpStatusCode.BadRequest, "Missing userId parameter"
        )

        val users = loadUsers()
        val userRooms = users[userId]?.rooms ?: emptyList()
        val rooms = loadRooms()

        val userRoomDetails = userRooms.mapNotNull { roomId ->
            rooms[roomId]?.let { room -> RoomResponse(id = roomId, name = room.name) }
        }

        call.respond(userRoomDetails)
    } catch (e: Exception) {
        call.application.log.error("Error fetching rooms: ${e.localizedMessage}", e)
        call.respond(HttpStatusCode.InternalServerError, "An error occurred")
    }
}

suspend fun handleSendMessage(call: ApplicationCall) {
    try {
        val request = call.receive<SendMessageRequest>()
        val rooms = loadRooms()
        val room = rooms[request.roomId]
            ?: return call.respond(HttpStatusCode.NotFound, "Room not found")

        room.messages.add(request.message)
        saveRooms(rooms)

        call.respond(SendMessageResponse("success", "Message sent successfully"))
    } catch (e: Exception) {
        call.application.log.error("Error sending message: ${e.localizedMessage}", e)
        call.respond(HttpStatusCode.BadRequest, "Invalid Request")
    }
}

suspend fun handleGetMessages(call: ApplicationCall) {
    try {
        val roomId = call.request.queryParameters["roomId"]
            ?: return call.respond(HttpStatusCode.BadRequest, "Missing roomId parameter")

        val rooms = loadRooms()
        val room = rooms[roomId]
            ?: return call.respond(HttpStatusCode.NotFound, "Room not found")

        val messagesWithDetails = room.messages.map { content ->
            Message(
                userId = "defaultUserId",
                nickname = "Anonim",
                content = content
            )
        }

        call.respond(messagesWithDetails)
    } catch (e: Exception) {
        call.application.log.error("Error fetching messages: ${e.localizedMessage}", e)
        call.respond(HttpStatusCode.InternalServerError, "An error occurred")
    }
}

suspend fun handleChangeUsername(call: ApplicationCall) {
    try {
        val request = call.receive<ChangeUsernameRequest>()
        val usernames = loadUsernames()
        usernames[request.userId] = request.newUsername
        saveUsernames(usernames)

        call.respond(ChangeUsernameResponse("success", "Username updated successfully!"))
    } catch (e: Exception) {
        call.application.log.error("Error changing username: ${e.localizedMessage}", e)
        call.respond(HttpStatusCode.BadRequest, "Invalid Request")
    }
}

// Funkcje pomocnicze
private const val ROOMS_FILE = "rooms.json"
private const val USERS_FILE = "users.json"
private const val USERNAMES_FILE = "usernames.json"

val fileMutex = Mutex()

fun generateRoomId(): String = java.util.UUID.randomUUID().toString()

fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
}

suspend fun loadRooms(): MutableMap<String, Room> = fileMutex.withLock {
    val file = File(ROOMS_FILE)
    if (file.exists()) {
        Json.decodeFromString(MapSerializer(String.serializer(), Room.serializer()), file.readText()).toMutableMap()
    } else mutableMapOf()
}

suspend fun saveRooms(rooms: Map<String, Room>) = fileMutex.withLock {
    val jsonString = Json.encodeToString(MapSerializer(String.serializer(), Room.serializer()), rooms)
    File(ROOMS_FILE).writeText(jsonString)
}

suspend fun loadUsers(): MutableMap<String, UserRooms> = fileMutex.withLock {
    val file = File(USERS_FILE)
    if (file.exists()) {
        Json.decodeFromString(MapSerializer(String.serializer(), UserRooms.serializer()), file.readText()).toMutableMap()
    } else mutableMapOf()
}

suspend fun saveUsers(users: Map<String, UserRooms>) = fileMutex.withLock {
    val jsonString = Json.encodeToString(MapSerializer(String.serializer(), UserRooms.serializer()), users)
    File(USERS_FILE).writeText(jsonString)
}

suspend fun loadUsernames(): MutableMap<String, String> = fileMutex.withLock {
    val file = File(USERNAMES_FILE)
    if (file.exists()) {
        Json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), file.readText()).toMutableMap()
    } else mutableMapOf()
}

suspend fun saveUsernames(usernames: Map<String, String>) = fileMutex.withLock {
    val jsonString = Json.encodeToString(MapSerializer(String.serializer(), String.serializer()), usernames)
    File(USERNAMES_FILE).writeText(jsonString)
}

// Modele danych
@Serializable
data class CreateRoomRequest(val roomName: String, val password: String? = null, val userId: String)

@Serializable
data class CreateRoomResponse(val status: String, val roomName: String, val message: String)

@Serializable
data class Room(val name: String, val passwordHash: String?, val messages: MutableList<String> = mutableListOf())

@Serializable
data class UserRooms(val rooms: MutableList<String> = mutableListOf())

@Serializable
data class RoomResponse(val id: String, val name: String)

@Serializable
data class SendMessageRequest(val roomId: String, val message: String)

@Serializable
data class SendMessageResponse(val status: String, val message: String)

@Serializable
data class ChangeUsernameRequest(
    val userId: String,
    val newUsername: String
)

@Serializable
data class ChangeUsernameResponse(
    val status: String,
    val message: String
)

@Serializable
data class Message(
    val userId: String,
    val nickname: String?,
    val content: String
)
