package com.example.mymegaapplication

import android.content.Context
import android.content.Intent
import android.net.InetAddresses
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.Toast.*
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.util.*

class MainActivity : AppCompatActivity() {
    class checkConnection: Thread() {
        var finished = false
        var reachable = false
        private var iptext = ""
        private lateinit var context: Context
        private var port = 9689

        public fun setPort(port: Int = 9689) {
            this.port = port
        }

        public fun setContext(context: Context) {
            this.context = context
        }

        public fun setIPText(string: String) {
            iptext = string
        }

        public override fun run() {
            if(checkIPText())
                reachable = true
            finished = true
        }

        fun checkIPText(): Boolean
        {
            if(InetAddresses.isNumericAddress(iptext))
            {
                Log.d("NTHread","Is valid IP")
                Log.d("NTHread","Checking for reachability")

                var exists  = false
                try {
                    var addr = InetSocketAddress(iptext, port)
                    var sock = Socket()
                    var timeout = 2000
                    sock.connect(addr,timeout)
                    exists = true
                    sock.close()
                }
                catch (e: java.lang.Exception)
                {
                    Log.d("NTHread","EXCEPTION: " + e.message.toString())
                }
                if(exists)
                {
                    Log.d("NTHread","Is Reachable")
                    return true;
                }
                else
                {
                    Log.d("NTHread","Is Not Reachable")
                }
            }
            return false;
        }

    }
    private lateinit var timer: CountDownTimer
    private lateinit var connectButton: Button
    private lateinit var iptext: EditText
    private lateinit var connect: checkConnection
    private var port = 9689
    fun printLogger(string: String)
    {
        Log.d("MAIN UI",string)
    }

    private fun gotoConnectionUI()
    {
        var intent = Intent(this,DesktopViewer::class.java)

        startActivity(intent.apply {
            putExtra("ip",iptext.text.toString())
            putExtra("port",port)
        })
    }

    private fun init() {
        connectButton = findViewById(R.id.connect);
        iptext = findViewById(R.id.ipText)
        connectButton.setOnClickListener {
            connect = checkConnection()
            connect.setContext(this)
            connect.setIPText(iptext.text.toString())
            connect.start()
            connectButton.isEnabled = false
            timer = object: CountDownTimer(5000, 1000)
            {
                override fun onTick(p0: Long) {

                }

                override fun onFinish() {
                    if(connect.finished && connect.reachable)
                    {
                        printLogger("Is Reachable")
                        gotoConnectionUI()
                    }
                    connectButton.isEnabled = true
                }
            }
            timer.start()

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init();
    }
}