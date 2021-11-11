package com.kazuki19992.tracker

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
//import androidx.compose.material.Text
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazuki19992.tracker.ui.theme.TrackerTheme
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

// デバッグログのタグ
const val debugTag = "DebugTag"


class MainActivity : ComponentActivity() {
  val REQUEST_ENABLE_BT = 1

  // Bluetooth設定
  val bluetoothTarget = "RNBT-330C"


  // BluetoothAdapter を取得
  val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // bluetooth対応端末か
    if (bluetoothAdapter == null) {
      makeText(applicationContext, "Bluetoothはこの端末で使用できません", Toast.LENGTH_LONG).show()
      Log.d(debugTag, "Bluetooth not supported.")
      finish()
      return
    }
    // Bluetoothが無効なら有効にするダイアログを表示
    if (bluetoothAdapter?.isEnabled == false) {
      Log.d(debugTag, "Bluetooth無効状態")
      val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }
    var deviceName:String = ""

    val pairdDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
    pairdDevices?.forEach { device ->
      deviceName = device.name                // デバイス名
      val deviceHardwareAddress = device.address  // MACアドレス
      if (deviceName == bluetoothTarget) {
        Log.d(debugTag, "name = %s, MAC <%s>".format(deviceName, deviceHardwareAddress))

        // UUIDをログに表示
        device.uuids.forEach { uuid ->
          Log.d(debugTag, "UUID is %s".format(uuid.uuid))
        }

        var connectThread = ConnectThread(device)
        connectThread?.start()
        // return
      }
    }

    setContent {
      TrackerTheme {
        TopView(deviceName)
      }
    }
  }

  fun manageMyConnectedSocket(socket: BluetoothSocket) {
    val connectedThread = ConnectedThread(socket)
    connectedThread?.start()
  }

  private inner class ConnectThread(device: BluetoothDevice) : Thread() {
    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
//       device.createInsecureRfcommSocketToServiceRecord(device.uuids[0].uuid)
      device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
      // Insecureだとアプリが落ちる
    }

    public override fun run() {
      bluetoothAdapter?.cancelDiscovery()
      if (mmSocket == null) {
        return
      }
      val socket = mmSocket
      log("ソケット: " + socket.toString())
      socket ?: return
      try {
        socket.connect()
        log("ソケット接続確立")
      }catch (e: IOException){
        err(e.message.toString())
        err(e.stackTraceToString())
      }
      // The connection attempt succeeded. Perform work associated with
      // the connection in a separate thread.
      manageMyConnectedSocket(socket)
    }

    // Closes the client socket and causes the thread to finish.
    fun cancel() {
      try {
        mmSocket?.close()
      } catch (e: IOException) {
        Log.e(debugTag, "Could not close the client socket", e)
      }
    }
  }


  private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

    private val mmInStream: InputStream = mmSocket.inputStream
    private val mmOutStream: OutputStream = mmSocket.outputStream
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

    override fun run() {
      var numBytes: Int // bytes returned from read()
      Log.d(debugTag, "connect start!")
      // Keep listening to the InputStream until an exception occurs.
      while (true) {
        // Read from the InputStream.
        numBytes = try {
          mmInStream.read(mmBuffer)
        } catch (e: IOException) {
          Log.d(debugTag, "Input stream was disconnected", e)
          break
        }
        Log.d(debugTag, mmBuffer[0].toString())
      }
    }

    fun cancel() {
      try {
        mmSocket.close()
      } catch (e: IOException) {
        Log.e(debugTag, "Could not close the connect socket", e)
      }
    }
  }


  // 有効化処理メソッド
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    when (resultCode) {
      RESULT_OK -> {
        makeText(applicationContext, "Bluetoothが有効になりました", Toast.LENGTH_LONG).show()
        Log.d(debugTag, "有効")
      }
      RESULT_CANCELED -> {
        makeText(
          applicationContext,
          "アプリケーションを使用するには\nBluetoothを有効にしてください",
          Toast.LENGTH_LONG
        ).show()
        Log.d(debugTag, "拒否")
      }
    }
  }
}

@Composable
fun TopView(sensor: String) {
  var received by remember { mutableStateOf("接続中...") }
  Log.d(debugTag, "起動完了")

  // トップ画面
  Column {
    Header(sensor)
    SerialTerm(received)
  }
}

@Composable
fun Header(sensor:String) {
  TopAppBar(
    title = { Text("Tracker (Sensor: " + sensor + ")")}
  )
}

@Composable
fun SerialTerm(received: String) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(15.dp)
      .height(200.dp)
      .clickable { },
    elevation = 10.dp,
    backgroundColor = Color.DarkGray
  ) {
    Column(modifier = Modifier.padding(15.dp)) {
      Text(
        text = "Received GPS monitor(BT Serial)",
        color = Color.Cyan,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.size(5.dp))
      Text(
        text = "$received",
        color = Color.Cyan,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace
      )
    }

  }
}

fun log(text:String) {
  Log.d(debugTag, text)
}
fun err(text:String) {
  Log.e(debugTag, text)
}

//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    AndroidTutorialTheme {
//        Greeting("Android")
//    }
//}
