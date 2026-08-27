package com.rimor.minipape

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.util.Locale
import kotlin.random.Random

class ReceiverServer(
    private val context: Context,
    private val repository: WallpaperRepository,
) {
    val pairCode: String = Random.nextInt(0, 1_000_000).toString().padStart(6, '0')
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (serverSocket != null) return
        scope.launch {
            val server = ServerSocket(PORT)
            serverSocket = server
            while (!server.isClosed) {
                runCatching { server.accept() }.getOrNull()?.let { socket ->
                    launch { socket.use(::handle) }
                }
            }
        }
    }

    private fun handle(socket: Socket) {
        socket.soTimeout = 20_000
        val input = BufferedInputStream(socket.getInputStream())
        val output = BufferedOutputStream(socket.getOutputStream())
        val requestLine = readLine(input)?.split(' ') ?: return
        if (requestLine.size < 2) return
        val method = requestLine[0]
        val path = requestLine[1]
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readLine(input) ?: return
            if (line.isEmpty()) break
            val separator = line.indexOf(':')
            if (separator > 0) headers[line.substring(0, separator).lowercase(Locale.US)] = line.substring(separator + 1).trim()
        }

        if (path != "/v1/status" && headers["x-minipape-code"] != pairCode) {
            respond(output, 401, "text/plain", "Pair code required".toByteArray())
            return
        }

        val length = headers["content-length"]?.toLongOrNull() ?: 0
        if (length > MAX_UPLOAD_BYTES) {
            respond(output, 413, "text/plain", "Upload too large".toByteArray())
            return
        }

        when {
            method == "GET" && path == "/v1/status" -> status(output)
            method == "POST" && path == "/v1/preview/source" -> {
                val name = headers["x-minipape-name"] ?: "preview.bin"
                val extension = name.substringAfterLast('.', "bin")
                val file = repository.previewFile(extension)
                receiveFile(input, file, length)
                repository.setPreviewSource(file, headers["x-minipape-media-kind"] ?: "image")
                respond(output, 204, "text/plain", byteArrayOf())
            }
            method == "PUT" && path == "/v1/preview/state" -> {
                val json = JSONObject(readBody(input, length).decodeToString())
                val crop = json.optJSONObject("recipe") ?: JSONObject()
                repository.updatePreview(
                    CropRecipe(
                        scale = crop.optDouble("scale", 1.0).toFloat(),
                        offsetX = crop.optDouble("offsetX", 0.0).toFloat(),
                        offsetY = crop.optDouble("offsetY", 0.0).toFloat(),
                        rotation = crop.optDouble("rotation", 0.0).toFloat(),
                        muted = crop.optBoolean("muted", true),
                        loop = crop.optBoolean("loop", true),
                    ),
                    json.optDouble("playhead", 0.0),
                    json.optBoolean("playing", true),
                )
                respond(output, 204, "text/plain", byteArrayOf())
            }
            method == "POST" && path == "/v1/wallpapers" -> {
                val name = headers["x-minipape-name"] ?: "wallpaper-${System.currentTimeMillis()}"
                val kind = headers["x-minipape-media-kind"] ?: "image"
                val file = repository.wallpaperFile(name)
                receiveFile(input, file, length)
                repository.addWallpaper(file, kind)
                respond(output, 201, "application/json", "{\"stored\":true}".toByteArray())
            }
            else -> respond(output, 404, "text/plain", "Not found".toByteArray())
        }
    }

    private fun status(output: BufferedOutputStream) {
        val name = Build.MODEL.ifBlank { "Galaxy Z Flip" }
        val json = JSONObject()
            .put("receiverName", "${Build.MANUFACTURER} $name")
            .put("deviceModel", name)
            .put("protocolVersion", 1)
            .put("canvasWidth", 1048)
            .put("canvasHeight", 948)
            .toString()
        respond(output, 200, "application/json", json.toByteArray())
    }

    private fun receiveFile(input: BufferedInputStream, file: File, length: Long) {
        file.outputStream().buffered().use { output ->
            var remaining = length
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (remaining > 0) {
                val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (count < 0) break
                output.write(buffer, 0, count)
                remaining -= count
            }
        }
    }

    private fun readBody(input: BufferedInputStream, length: Long): ByteArray {
        val result = ByteArray(length.toInt())
        var offset = 0
        while (offset < result.size) {
            val count = input.read(result, offset, result.size - offset)
            if (count < 0) break
            offset += count
        }
        return if (offset == result.size) result else result.copyOf(offset)
    }

    private fun readLine(input: BufferedInputStream): String? {
        val bytes = ArrayList<Byte>()
        while (true) {
            val value = input.read()
            if (value < 0) return null
            if (value == '\n'.code) break
            if (value != '\r'.code) bytes += value.toByte()
            if (bytes.size > 16_384) return null
        }
        return bytes.toByteArray().decodeToString()
    }

    private fun respond(output: BufferedOutputStream, code: Int, type: String, body: ByteArray) {
        val reason = when (code) { 200 -> "OK"; 201 -> "Created"; 204 -> "No Content"; 401 -> "Unauthorized"; 404 -> "Not Found"; 413 -> "Payload Too Large"; else -> "Error" }
        output.write("HTTP/1.1 $code $reason\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
        output.write(body)
        output.flush()
    }

    companion object {
        const val PORT = 47977
        const val MAX_UPLOAD_BYTES = 250L * 1024L * 1024L
    }
}

