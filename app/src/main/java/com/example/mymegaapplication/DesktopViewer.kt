package com.example.mymegaapplication

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.View.DragShadowBuilder
import android.view.View.OnDragListener
import android.view.View.OnTouchListener
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.handshake.ServerHandshakeBuilder
import java.io.FileInputStream
import java.net.InetAddress
import java.net.URI
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.net.SocketFactory
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import kotlin.system.exitProcess


class DesktopViewer : AppCompatActivity() {
    private var ssl = true
    private var port = 0
    private var ip: String? = ""

    private lateinit var context: Context
    private lateinit var button: Button

    private var webSocketClient: WebSocketClient? = null
    private var url: URI? = null
    private lateinit var actionButton: FloatingActionButton

    private var startmode = false
    private var updated = false;
    private lateinit var seekbar: SeekBar
    private lateinit var keyboardentry: EditText

    private var mousesenstivity = 1f
    private var timer: CountDownTimer? = null
    var phoneheight = 0
    var phonewidth = 0
    private var startingImageFormat = "<ServicePNG>"
    private lateinit var imageWidget: ImageView
    private lateinit var progressWidget: ProgressBar
    var sslFactory: SocketFactory? = SSLSocketFactory.getDefault()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE); //will hide the title
        getSupportActionBar()?.hide(); // hide the title bar
        setContentView(R.layout.activity_desktop_viewer)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        context = this
        phonewidth = displayMetrics.widthPixels
        phoneheight = displayMetrics.heightPixels
        progressWidget = findViewById(R.id.progressBar)
        seekbar = findViewById(R.id.seekBar)
        seekbar.visibility = View.GONE
        var extras: Bundle? = intent.extras
        imageWidget = findViewById(R.id.imageView)
        keyboardentry = findViewById(R.id.keyboardEdit)
        if (extras != null) {
            ip = extras.getString("ip")
            if(ip.isNullOrBlank())
            {
                Log.d("MegaUI","Could not get ip addr")
                exitProcess(-1)
            }
            actionButton = findViewById(R.id.floatingActionButton)
            button = findViewById(R.id.button)
            keyboardentry.visibility = View.GONE
            button.visibility = View.GONE
            port = extras.getInt("port")
            Log.d("WebSocketClient","Got IP as $ip and Port as $port")
            var addr = InetAddress.getByName(ip)
            var path = filesDir
            if(addr.isSiteLocalAddress)
                ssl = false
            if(ssl)
            {
                path = path.resolve("client-cert1.pfx")
                Log.d("WebSocketClient","Path: ${path.toString()}")
                var fis = FileInputStream(path);
                val ks = KeyStore.getInstance("pkcs12")
                ks.load(fis, "PasswordString".toCharArray())
                val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("X509")
                tmf.init(ks)
                val kmf: KeyManagerFactory = KeyManagerFactory.getInstance("X509")
                kmf.init(ks, "PasswordString".toCharArray())
                val sslContext: SSLContext = SSLContext.getInstance("TLS")
                sslContext.init(kmf.keyManagers, tmf.trustManagers, null);

            }
            keyboardentry.setOnEditorActionListener(object: OnEditorActionListener{
                override fun onEditorAction(p0: TextView?, p1: Int, p2: KeyEvent?): Boolean {
                        if(p2 == null || p0 == null)
                            return false
                    when(p2?.action){
                          KeyEvent.ACTION_UP -> {
                              webSocketClient?.send("<Key=${p2.unicodeChar}>")
                          }
                    }
                    return true
                }

            })
            button.setOnClickListener {
                if (webSocketClient != null && startingImageFormat == "<ServicePNG>") {
                    startingImageFormat = "<ServiceJPG>"
                    Log.d("WebSocketClient","Action Button Long press to JPG send")
                    webSocketClient?.send("<ServiceJPG>")
                    button.text = "JPEG"
                    return@setOnClickListener
                }
                if (webSocketClient != null && startingImageFormat == "<ServiceJPG>") {
                    startingImageFormat = "<ServicePNG>"
                    Log.d("WebSocketClient","Action Button Long press to PNG send")
                    webSocketClient?.send("<ServicePNG>")
                    button.text = "PNG"
                    return@setOnClickListener
                }
            }


            imageWidget.setOnTouchListener(object: OnTouchListener{
                private var startx = 0f
                private var starty = 0f
                private var prevx = 0f
                private var prevy = 0f
                private var timeout = false
                private var tim = object: CountDownTimer(500,500)
                {
                    override fun onTick(p0: Long) {

                    }

                    override fun onFinish() {
                    timeout = true
                    }

                }


                override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                    if(p0 == null || p1 == null)
                        return false

                    var action = p1?.action
                    when(action)
                    {
                        MotionEvent.ACTION_DOWN -> {
                            startx = p1!!.x
                            starty = p1!!.y
                            prevx = startx
                            prevy = starty
                            timeout = false
                            tim.start()
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val vx = (p1!!.x - prevx)
                            val vy = (p1!!.y - prevy)
                            webSocketClient?.send("<MouseMoveX=${vx}Y=$vy>")
                            Log.d("WebSocketClient","Mouse Moved ${p1!!.x} , ${p1!!.y}")
                            prevx = p1!!.x
                            prevy = p1!!.y
                        }
                        MotionEvent.ACTION_UP -> {
                            if(startx == p1!!.x && starty == p1!!.y && !timeout)
                            {
                                p0!!.performClick()
                            }
                            if(startx == p1!!.x && starty == p1!!.y && timeout)
                            {
                                p0!!.performLongClick()
                            }
                        }

                    }
                    return true
                }

            })
            imageWidget.setOnClickListener{
                webSocketClient?.send("<Click>")
            }
            imageWidget.setOnLongClickListener {
                webSocketClient?.send("<RightClick>")
                return@setOnLongClickListener true
            }

            actionButton.setOnLongClickListener {
                val myShadow = DragShadowBuilder(actionButton)
                it.startDragAndDrop(null, myShadow, null, View.DRAG_FLAG_GLOBAL)
                true
            }
            actionButton.setOnClickListener {
                timer = object: CountDownTimer(3000,3000){
                    override fun onTick(p0: Long) {
                    }

                    override fun onFinish() {
                        seekbar.visibility = View.GONE
                        button.visibility = View.GONE
                        keyboardentry.visibility = View.GONE
                    }

                }
                button.visibility = View.VISIBLE
                seekbar.visibility = View.VISIBLE
                keyboardentry.visibility = View.VISIBLE
                timer!!.start()
            }
             actionButton.rootView.setOnDragListener(object: OnDragListener{
                private var dx = 0f
                private var dy = 0f
                override fun onDrag(p0: View?, p1: DragEvent?): Boolean {
                    if(p1 == null || p0 == null)
                    {
                        return false
                    }
                    when(p1?.action)
                    {
                        DragEvent.ACTION_DRAG_LOCATION -> {
                            dx = p1.x
                            dy = p1.y
                        }
                        DragEvent.ACTION_DRAG_ENDED -> {
                            actionButton.x = dx - actionButton.width/2
                            actionButton.y = dy - actionButton.height/2
                        }
                    }
                    return true
                }

            })
            seekbar.setOnSeekBarChangeListener(object: OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    Log.d("WebSocketClient","Request Server Change Image Quality to $p1")
                    webSocketClient?.send("<SetImageQuality=$p1>")
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                timer!!.cancel()
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                timer!!.start()
                }
            })

            initWebSocket()
        }
    }

    private fun initWebSocket() {
        Log.d("WebSocketClient","Initializing WebSocket Client as wss://$ip:$port")
        url = URI("wss://$ip:$port")

        createWebSocketClient(url)
    }

    private fun pictureBuilder(bytes: ByteBuffer?) {
        var byteArray:ByteArray = ByteArray(bytes?.capacity() ?: 0)
        bytes?.get(byteArray)
        var bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
        bitmap = Bitmap.createScaledBitmap(bitmap,phonewidth,phoneheight,false);
        runOnUiThread {
            imageWidget.setImageBitmap(bitmap)

        }
        webSocketClient?.send("<ClientReady>")
    }

    private fun createWebSocketClient(url: URI?) {
        webSocketClient = object : WebSocketClient(url) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d("WebSocketClient", "onOpen")
                subscribe()
            }

            override fun onMessage(bytes: ByteBuffer?) {
                super.onMessage(bytes)


                if(startmode)
                {
                    if(progressWidget.visibility != View.GONE)
                    {
                        runOnUiThread {
                            progressWidget.visibility = View.GONE
                        }
                    }
                    pictureBuilder(bytes)
                }

            }

            override fun onMessage(message: String?) {
                Log.d("WebSocketClient","Message From Server: $message")
                if(message == "<ServiceStart>")
                    startmode = true;
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                if(remote)
                {
                    Log.d("WebSocketClient","Socket Closed by server due to: $reason")
                    intent = Intent(context,MainActivity::class.java)
                    intent.putExtra("Closed","Server $reason")
                    startActivity(intent)
                }
                else
                {
                    Log.d("WebSocketClient","Socket Closed by client due to: $reason")
                }
            }

            override fun onWebsocketHandshakeSentAsClient(
                conn: WebSocket?,
                request: ClientHandshake?
            ) {
                super.onWebsocketHandshakeSentAsClient(conn, request)
            }

            override fun onWebsocketHandshakeReceivedAsServer(
                conn: WebSocket?,
                draft: Draft?,
                request: ClientHandshake?
            ): ServerHandshakeBuilder {
                return super.onWebsocketHandshakeReceivedAsServer(conn, draft, request)
            }
            override fun onError(ex: Exception?) {
                Log.e("WebSocketClient","Client EXCEPTION: ${ex.toString()}")
            }

        }
        if(sslFactory == null)
        {
            Log.e("WebSocketClient","Socket Factory Returned NULL Default")
            exitProcess(-1)
        }
        webSocketClient!!.setSocketFactory(sslFactory)
        webSocketClient!!.connect()
    }
    private fun subscribe() {
        webSocketClient!!.send("<ClientReady>")
    }
    override fun onResume() {
        super.onResume()
        //initWebSocket()
    }

    override fun onPause() {
        super.onPause()
        webSocketClient!!.close()
    }
}