package com.donut.mixfile.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import com.donut.mixfile.MainActivity
import com.donut.mixfile.R
import com.donut.mixfile.appScope
import com.donut.mixfile.kv
import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.core.routes.api.routes.webdav.objects.WebDavManager
import com.donut.mixfile.server.core.utils.MixUploadTask
import com.donut.mixfile.server.core.utils.extensions.kb
import com.donut.mixfile.server.core.utils.ignoreError
import com.donut.mixfile.server.core.utils.toJsonString
import com.donut.mixfile.server.image.createBlankBitmap
import com.donut.mixfile.server.image.toGif
import com.donut.mixfile.ui.routes.favorites.result
import com.donut.mixfile.ui.routes.home.UploadTask
import com.donut.mixfile.ui.routes.home.serverAddress
import com.donut.mixfile.ui.routes.increaseDownloadData
import com.donut.mixfile.ui.routes.increaseUploadData
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.file.favorites
import com.donut.mixfile.util.showError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.donut.mixfile.server.core.objects.WebDavFile
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*

var DOWNLOAD_TASK_COUNT by cachedMutableOf(5, "download_task_count")
var UPLOAD_TASK_COUNT by cachedMutableOf(10, "upload_task_count")
var UPLOAD_RETRY_COUNT by cachedMutableOf(10, "UPLOAD_RETRY_TIMES")
var SERVER_PASSWORD by cachedMutableOf("", "MIXFILE_SERVER_PASSWORD")
var MIXFILE_CHUNK_SIZE by cachedMutableOf(1024, "mixfile_server_chunk_size")


val mixFileServer = object : MixFileServer(
) {

    override fun onDownloadData(data: ByteArray) {
        increaseDownloadData(data.size.toLong())
    }

    override fun onUploadData(data: ByteArray) {
        increaseUploadData(data.size.toLong())
    }


    override val downloadTaskCount: Int
        get() = DOWNLOAD_TASK_COUNT.toInt()

    override val uploadTaskCount: Int
        get() = UPLOAD_TASK_COUNT.toInt()

    override val uploadRetryCount: Int
        get() = UPLOAD_RETRY_COUNT.toInt()

    override val chunkSize: Int
        get() = MIXFILE_CHUNK_SIZE.toInt() * 1.kb

    override val password: String
        get() = SERVER_PASSWORD

    override val webDav: WebDavManager = object : WebDavManager() {

        val mutex = Mutex()
        var saveTask: Job? = null

        override suspend fun saveWebDavData(data: ByteArray) {
            synchronized(this) {
                saveTask?.cancel()
                saveTask = appScope.launch(Dispatchers.Main) {
                    mutex.withLock {
                        delay(100)
                        kv.encode(WEB_DAV_KEY, data)
                    }
                }
            }
        }
    }


    override fun onError(error: Throwable) {
        showError(error)
    }

    override fun getUploader(): Uploader {
        return getCurrentUploader()
    }

    override suspend fun genDefaultImage(): ByteArray {
        return createBlankBitmap().toGif()
    }

    override suspend fun getStaticFile(path: String): java.io.InputStream? {
        if (path == "index.html") {
            val originalStream = super.getStaticFile(path) ?: return null
            val originalHtml = originalStream.bufferedReader().use { it.readText() }
            val injectedScript = """
                <script>
                function injectWebRestoreUI() {
                    // Wait for the main app to load
                    const root = document.getElementById('root');
                    if (!root || !root.firstChild) {
                        setTimeout(injectWebRestoreUI, 100);
                        return;
                    }

                    // 1. Create the main button
                    const restoreBtn = document.createElement('button');
                    restoreBtn.innerText = 'Restore from WebDAV';
                    restoreBtn.style.cssText = 'position: fixed; top: 10px; right: 10px; z-index: 9999; padding: 8px 12px; background-color: #8a2bfe; color: white; border: none; border-radius: 5px; cursor: pointer;';
                    document.body.appendChild(restoreBtn);

                    // 2. Add event listener to the button
                    restoreBtn.onclick = () => showRestoreDialog();

                    // 3. State management
                    let state = { url: '', user: '', pass: '', path: '/', files: [], dialogOpen: false, view: 'config' };

                    // 4. Dialog logic
                    function showRestoreDialog() {
                        if (state.dialogOpen) return;
                        state = { ...state, dialogOpen: true, view: 'config' };
                        renderDialog();
                    }

                    function renderDialog() {
                        // Remove existing dialog
                        const existingDialog = document.getElementById('restoreDialog');
                        if (existingDialog) existingDialog.remove();

                        const dialog = document.createElement('div');
                        dialog.id = 'restoreDialog';
                        dialog.style.cssText = 'position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 80%; max-width: 600px; max-height: 80%; background: white; border-radius: 8px; box-shadow: 0 4px 20px rgba(0,0,0,0.2); z-index: 10000; display: flex; flex-direction: column;';
                        
                        let innerHTML = '';
                        if (state.view === 'config') {
                            innerHTML = `
                                <h3 style="padding: 16px; margin: 0; border-bottom: 1px solid #eee;">Connect to WebDAV</h3>
                                <div style="padding: 16px; display: flex; flex-direction: column; gap: 10px;">
                                    <input id="restoreUrl" placeholder="URL" value="${state.url}" style="padding: 8px;"/>
                                    <input id="restoreUser" placeholder="Username" value="${state.user}" style="padding: 8px;"/>
                                    <input id="restorePass" type="password" placeholder="Password" value="${state.pass}" style="padding: 8px;"/>
                                </div>
                                <div style="padding: 16px; border-top: 1px solid #eee; text-align: right;">
                                    <button id="cancelBtn">Cancel</button> <button id="connectBtn">Connect</button>
                                </div>`;
                        } else { // File browser view
                            innerHTML = `
                                <h3 style="padding: 16px; margin: 0; border-bottom: 1px solid #eee;">Select Archive File (${state.path})</h3>
                                <div style="flex-grow: 1; overflow-y: auto; padding: 8px;">
                                    ${state.path !== '/' ? `<div class="file-item" data-path="${state.path.substring(0, state.path.lastIndexOf('/')) || '/'}">[..] Back</div>` : ''}
                                    ${state.files.map(f => `<div class="file-item" data-folder="${f.isFolder}" data-name="${f.name}">${f.isFolder ? '&#128193;' : '&#128196;'} ${f.name}</div>`).join('')}
                                </div>
                                <div style="padding: 16px; border-top: 1px solid #eee; text-align: right;">
                                    <button id="cancelBtn">Cancel</button>
                                </div>`;
                        }
                        dialog.innerHTML = innerHTML;
                        document.body.appendChild(dialog);
                        attachDialogListeners();
                    }

                    function attachDialogListeners() {
                        document.getElementById('cancelBtn').onclick = () => {
                            state.dialogOpen = false;
                            document.getElementById('restoreDialog').remove();
                        };

                        if (state.view === 'config') {
                            document.getElementById('connectBtn').onclick = async () => {
                                state.url = document.getElementById('restoreUrl').value;
                                state.user = document.getElementById('restoreUser').value;
                                state.pass = document.getElementById('restorePass').value;
                                
                                const params = new URLSearchParams({ url: state.url, user: state.user, pass: state.pass, path: state.path });
                                const response = await fetch('/api/webrestore/list', { method: 'POST', body: params });
                                if (response.ok) {
                                    state.files = await response.json();
                                    state.view = 'browser';
                                    renderDialog();
                                } else {
                                    alert('Failed to connect: ' + await response.text());
                                }
                            };
                        } else {
                            document.querySelectorAll('.file-item').forEach(item => {
                                item.onclick = async (e) => {
                                    const isFolder = e.currentTarget.dataset.folder === 'true';
                                    if (isFolder) {
                                        state.path = (state.path === '/' ? '' : state.path) + '/' + e.currentTarget.dataset.name;
                                        const params = new URLSearchParams({ url: state.url, user: state.user, pass: state.pass, path: state.path });
                                        const response = await fetch('/api/webrestore/list', { method: 'POST', body: params });
                                        state.files = await response.json();
                                        renderDialog();
                                    } else { // Is a file, trigger restore
                                        const fileName = e.currentTarget.dataset.name;
                                        if (!fileName.endsWith('.mix_dav')) {
                                            alert('Please select a .mix_dav archive file.');
                                            return;
                                        }
                                        const restoreParams = new URLSearchParams({ url: state.url, user: state.user, pass: state.pass, path: state.path, fileName: fileName });
                                        await fetch('/api/webrestore/start', { method: 'POST', body: restoreParams });
                                        alert('Restore started in background!');
                                        state.dialogOpen = false;
                                        document.getElementById('restoreDialog').remove();
                                    }
                                };
                            });
                        }
                    }
                }
                window.addEventListener('load', injectWebRestoreUI);
                </script>
            """.trimIndent()
            val modifiedHtml = originalHtml.replace("</body>", "$injectedScript</body>")
            return modifiedHtml.byteInputStream()
        }
        return super.getStaticFile(path)
    }

    override suspend fun getFileHistory(): String {
        return withContext(Dispatchers.Main) {
            if (result.isEmpty()) {
                return@withContext favorites.asReversed().take(1000).toJsonString()
            }
            result.take(1000).toJsonString()
        }
    }

    override fun getUploadTask(
        name: String,
        size: Long,
        add: Boolean
    ): MixUploadTask {
        return UploadTask(name, size, add)
    }

    override val extendModule: Application.() -> Unit = {
        routing {
            post("/api/webrestore/list") {
                val params = call.receiveParameters()
                val url = params["url"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val user = params["user"] ?: ""
                val pass = params["pass"] ?: ""
                val path = params["path"] ?: "/"
                try {
                    val files = instance?.listRemoteWebDavFiles(url, path, user, pass)
                    call.respond(files ?: emptyList())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown error")
                }
            }
            post("/api/webrestore/start") {
                val params = call.receiveParameters()
                val url = params["url"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val user = params["user"] ?: ""
                val pass = params["pass"] ?: ""
                val path = params["path"] ?: "/"
                val fileName = params["fileName"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                try {
                    val fileToRestore = WebDavFile(name = fileName, isFolder = false) // Simplified for the call
                    instance?.startRestoreFromArchive(fileToRestore, url, user, pass, path)
                    call.respond(HttpStatusCode.OK, "Restore started.")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown error")
                }
            }
        }
    }
}
var serverStarted by mutableStateOf(false)

const val WEB_DAV_KEY = "mixfile_web_dav_data"


class FileService : Service() {

    companion object {
        var instance: FileService? = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val CHANNEL_ID = "MixFileServerChannel"


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        appScope.launch(Dispatchers.IO) {
            mixFileServer.apply {
                webDav.loaded = false
                launch(Dispatchers.IO) {
                    ignoreError {
                        webDav.loadDataFromBytes(kv.decodeBytes(WEB_DAV_KEY) ?: byteArrayOf())
                    }
                    webDav.loaded = true
                }
                start(false)
            }
            delay(1000)
            serverStarted = true
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, getNotification())
        instance = this
    }


    private fun getNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MixFile局域网服务器")
            .setOnlyAlertOnce(true) // so when data is updated don't make sound and alert in android 8.0+
            .setOngoing(true)
            .setContentText("运行中: $serverAddress")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun updateNotification() {
        val notification: Notification = getNotification()

        val mNotificationManager =
            getSystemService(NotificationManager::class.java)
        mNotificationManager.notify(1, notification)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "局域网文件服务器",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    suspend fun listRemoteWebDavFiles(url: String, path: String, user: String, pass: String): List<WebDavFile> {
        val requestUrl = (url.removeSuffix("/") + "/" + path.removePrefix("/")).trimEnd('/')
        val response: HttpResponse = mixFileServer.httpClient.request(requestUrl) {
            method = HttpMethod("PROPFIND")
            header("Depth", "1")
            if (user.isNotEmpty() || pass.isNotEmpty()) {
                val credentials = "$user:$pass"
                header("Authorization", "Basic ${credentials.encodeBase64()}")
            }
        }

        if (response.status != HttpStatusCode.MultiStatus) {
            throw Exception("Failed to list files, status: ${response.status}")
        }

        val xmlString = response.bodyAsText()
        return parsePropfindXml(xmlString, path)
    }

    private fun parsePropfindXml(xml: String, basePath: String): List<WebDavFile> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val xpp = factory.newPullParser()
        xpp.setInput(StringReader(xml))

        val files = mutableListOf<WebDavFile>()
        var eventType = xpp.eventType
        var currentFile: WebDavFile? = null
        var text: String? = null

        var inProp = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = xpp.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when {
                        tagName.equals("response", ignoreCase = true) -> currentFile = WebDavFile(name = "")
                        tagName.equals("prop", ignoreCase = true) -> inProp = true
                    }
                }
                XmlPullParser.TEXT -> text = xpp.text
                XmlPullParser.END_TAG -> {
                    if (currentFile != null) {
                        when {
                            tagName.equals("href", ignoreCase = true) -> {
                                val href = text ?: ""
                                val decodedHref = href.decodeURLPart()
                                // Skip the root folder itself in the list
                                if (decodedHref.trimEnd('/') != basePath.trimEnd('/')) {
                                    currentFile.setName(decodedHref.substringAfterLast('/'))
                                }
                            }
                            tagName.equals("getcontentlength", ignoreCase = true) && inProp -> currentFile = currentFile.copy(size = text?.toLongOrNull() ?: 0)
                            tagName.equals("getlastmodified", ignoreCase = true) && inProp -> {
                                val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
                                currentFile = currentFile.copy(lastModified = text?.let { sdf.parse(it)?.time } ?: System.currentTimeMillis())
                            }
                            tagName.equals("collection", ignoreCase = true) && inProp -> currentFile = currentFile.copy(isFolder = true)
                            tagName.equals("prop", ignoreCase = true) -> inProp = false
                            tagName.equals("response", ignoreCase = true) -> {
                                // Add to list if it's not the root folder itself
                                if (currentFile.getName().isNotEmpty()) {
                                    files.add(currentFile)
                                }
                                currentFile = null
                            }
                        }
                    }
                }
            }
            eventType = xpp.next()
        }
        return files
    }

    fun startRestoreFromArchive(file: WebDavFile, sourceUrl: String, sourceUser: String, sourcePass: String, targetPath: String) {
        appScope.launch(Dispatchers.IO) {
            if (file.isFolder || !file.getName().endsWith(".mix_dav")) {
                showError(Exception("Please select a .mix_dav archive file."))
                return@launch
            }

            val fileUrl = (sourceUrl.removeSuffix("/") + "/" + (targetPath.removePrefix("/") + "/" + file.getName()).removePrefix("/")).trimEnd('/')

            try {
                // 1. Download the archive file from the remote WebDAV
                val downloadResponse: HttpResponse = mixFileServer.httpClient.get(fileUrl) {
                    if (sourceUser.isNotEmpty() || sourcePass.isNotEmpty()) {
                        val credentials = "$sourceUser:$sourcePass"
                        header("Authorization", "Basic ${credentials.encodeBase64()}")
                    }
                }

                if (downloadResponse.status != HttpStatusCode.OK) {
                    throw Exception("Failed to download archive: ${downloadResponse.status}")
                }
                val archiveData = downloadResponse.bodyAsBytes()

                // 2. PUT the archive to the local server to trigger the restore logic
                val localPutUrl = "http://127.0.0.1:${mixFileServer.serverPort}/api/webdav${targetPath}/${file.getName()}"
                val uploadResponse = mixFileServer.httpClient.put(localPutUrl) {
                    setBody(archiveData)
                    // Add local server password header if needed
                    if (SERVER_PASSWORD.isNotEmpty()) {
                        header(HttpHeaders.Authorization, SERVER_PASSWORD)
                    }
                }

                if (uploadResponse.status.isSuccess()) {
                    println("Successfully restored from ${file.getName()}")
                    // The webdav manager in mixFileServer is already updated, just need to save.
                    mixFileServer.webDav.saveData()
                } else {
                    throw Exception("Failed to restore archive on local server: ${uploadResponse.status}")
                }

            } catch (e: Exception) {
                mixFileServer.onError(e)
            }
        }
    }
}