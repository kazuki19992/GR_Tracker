package com.kazuki19992.tracker

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.bluetooth.*
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.text.method.MultiTapKeyListener.getInstance
import android.util.Log
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
//import androidx.compose.material.Text
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.BidiFormatter.getInstance
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.android.libraries.maps.model.PolylineOptions
import com.google.maps.android.ktx.awaitMap
import com.kazuki19992.tracker.ui.theme.TrackerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.String.format
import java.util.*
import kotlin.reflect.typeOf
import kotlin.system.exitProcess
import java.util.Arrays.toString as toString1
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.lang.Thread.sleep


// デバッグログのタグ
const val debugTag = "DebugTag"

var ReceivedString: String = "受信中……"
var deviceUuid: ParcelUuid? = null

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
          deviceUuid = uuid
          Log.d(debugTag, "UUID is %s".format(uuid.uuid))
        }

        var connectThread = ConnectThread(device)
        connectThread?.start()
        // return
      }
    }

    setContent {
      TrackerTheme {
        TopView(deviceName, ReceivedString)
      }
    }
  }

  fun manageMyConnectedSocket(socket: BluetoothSocket) {
    val connectedThread = ConnectedThread(socket)
    connectedThread?.start()
  }

  private inner class ConnectThread(device: BluetoothDevice) : Thread() {
//    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
//      device.createInsecureRfcommSocketToServiceRecord(deviceUuid?.uuid)
//      device.createRfcommSocketToServiceRecord(deviceUuid?.uuid)
//      // Insecureだとアプリが落ちる
//    }

    // なんかサーバーソケット？でも許可してあげなきゃいけないっぽい？

    private val mBtServerSocket: BluetoothServerSocket? = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("com.kazuki19992.tracker", deviceUuid?.uuid)
    // private var mmSocket: BluetoothSocket? = mBtServerSocket?.accept()
    private val mmSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
    //mmSocket = BluetoothDevice.createRfcommSocketToServiceRecord(deviceUuid?.uuid)

    public override fun run() {
      bluetoothAdapter?.cancelDiscovery()
      if (mmSocket == null) {
        return
      }
      val socket = mmSocket
      log("ソケット: " + socket.toString())
      socket ?: return

      while(true){
        try {
          socket.connect()
          log("ソケット接続確立")
          break

        }catch (e: IOException){
          err(e.message.toString())
          err(e.stackTraceToString())
          ReceivedString = e.message.toString()
          // 再試行
          sleep(1000)
          continue
        }
      }

      manageMyConnectedSocket(socket)
    }

    fun cancel() {
      try {
        mmSocket?.close()
      } catch (e: IOException) {
        Log.e(debugTag, "Could not close the client socket", e)
      }
    }
  }


  public inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

    private val mmInStream: InputStream = mmSocket.inputStream
    private val mmOutStream: OutputStream = mmSocket.outputStream
//    val mmBuffer: ByteArray = ByteArray(55 + 47)

    override fun run() {
      var numBytes: Int // bytes returned from read()
      Log.d(debugTag, "connect start!")
      // Keep listening to the InputStream until an exception occurs.
      while (true) {
        var mmBuffer: ByteArray = ByteArray(1024)
        log("loop")

        var tmpText: String = ""
        // Read from the InputStream.
//        numBytes = try {
        try{
          log("データを読みます")
          Log.d(debugTag, mmInStream.toString())
          mmInStream.read(mmBuffer)
        } catch (e: IOException) {
          Log.e(debugTag, "Input stream was disconnected", e)
          ReceivedString = e.toString()
          break
        }
        log("データ:" + tmpText)
        val checkString = String(mmBuffer)
        val compareResult = checkString.startsWith("\$POS")

//        // なぜかこっちだと動く
        when {
          compareResult == true -> notIgnore(checkString)
          compareResult == false -> err("データ不完全のため無視")
        }
        log(mmBuffer.size.toString())

        sleep(1000)
        // 条件分岐でやるのはクソほど重いので一旦なしでやってみる
//        notIgnore(checkString)
      }
    }

    fun notIgnore(checkString: String){
      ReceivedString = checkString
      log(ReceivedString)
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
fun TopView(sensor: String, received: String) {
  Log.d(debugTag, "起動完了")

  // ステート管理する
  val (receivedState, updateReceivedState) = remember {
    mutableStateOf(received)
  }
  // 数秒ごとにデータを取得する
  SideEffect {
    val handler = Handler()
    var runnable = Runnable {  }

    runnable = Runnable {
      updateReceivedState(ReceivedString)
      handler.postDelayed(runnable, 100)
    }
    handler.post(runnable)
  }

//  // マップ関連
//  val mapView = rememberMapViewWithLifecycle()

  // デバッグ用
//  val debug = "\$POS,Nihon,5:57:57,37:21:6824,N,140:22:9944,E,1,4\n"

  // トップ画面
  Column {
    Header(sensor)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(15.dp)
        .height(230.dp)
        .clickable { },
      elevation = 10.dp,
      backgroundColor = Color.DarkGray
    ) {
      MapViewComponents(receivedToLatLng(receivedState), getName(receivedState))
    }
    SerialTerm(receivedState)
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
        text = received,
        color = Color.Cyan,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace
      )
    }

  }
}

fun degminsecToDegree (degminsec: String): Double {
  val splited = degminsec.split(":")
  val degree = splited[0].toDoubleOrNull()
  val minute = (splited[1] + "." + splited[2]).toDoubleOrNull()

  if(degree != null && minute != null) {
    return degree + (minute / 60)
  }
  return -999.9
}

fun receivedToLatLng (received: String): LatLng {
  Log.d("receivedToLatLnd", "受信データ: " + received)
  // $POS,Nihon,23:59:59,37:21:6824,N,140:22:9944,E,1,4
  val ptn = "^\\\$POS,\\w+,[1|2]?\\d:[0-5]?\\d:[0-5]?\\d,\\d+:\\d+:\\d+,N,\\d+:\\d+:\\d+,E,[1|A].*"
  val regex = Regex(pattern = ptn)
  if(regex.containsMatchIn(received)){
    Log.d("receivedToLatLnd","部分的にマッチしました")
//    マッチしたら処理をする
    val splited = received.split(",").map { it.trim() }
    val latitude = splited[3]
    val NorS = splited[4]
    val longitude = splited[5]
    val EorW = splited[6]
    Log.d(debugTag, latitude + NorS + "," + longitude + EorW)

    // 度分秒から度に変換
    val convedLat = degminsecToDegree(latitude)
    val convedLng = degminsecToDegree(longitude)

    return LatLng(convedLat, convedLng)
  }
  Log.e("receivedToLatLnd","マッチしなかったです")
  return LatLng(35.68, 139.76)
}

fun getName (received: String) : String {
  val ptn = "^\\\$POS,[\\w]*,[1|2]?\\d:[0-5]?\\d:[0-5]?\\d,\\d*:\\d*:\\d*,N,\\d*:\\d*:\\d*,E,\\d,\\d\\n"
  val regex = Regex(pattern = ptn)
  if(regex.matches(received)){
    val splited = received.split(",").map { it.trim() }
    return splited[1]
  }
  return "???"
}

@Composable
fun MapViewComponents (location: LatLng, name: String) {
  // マップ関連
  val mapView = rememberMapViewWithLifecycle()
  val mapBitmap: MutableState<Bitmap?> = remember { mutableStateOf(null) }
  val coroutineScope = rememberCoroutineScope()
//  val (locationState, setLocationState) = remember { mutableStateOf(location) }
//
//  // 数秒ごとにデータを取得する
//  SideEffect {
//    val handler = Handler()
//    var runnable = Runnable {  }
//
//    runnable = Runnable {
//      setLocationState(receivedToLatLng(ReceivedString))
//      handler.postDelayed(runnable, 10)
//    }
//    handler.post(runnable)
//  }



//  if (mapBitmap.value != null) {
//    Log.d("mapDebug", "ロケーション(NonNull): " + location.toString())
//    Image(
//      bitmap = mapBitmap.value!!.asImageBitmap(),
//      contentDescription = "Map snapshot",
//    )
//  } else {
//    Log.d("mapDebug", "ロケーション: " + location.toString())
    AndroidView({ mapView }) { mapView ->
//      CoroutineScope(Dispatchers.Main).launch {
      coroutineScope.launch {
        val map = mapView.awaitMap()
        map.uiSettings.isZoomControlsEnabled = true

//        var pinned = receivedToLatLng(received = receivedData)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        val marker = map.addMarker(
          MarkerOptions()
            .title(name)
            .position(location)
        )
//        map.addMarker(
//          MarkerOptions()
//            .title(name)
//            .position(location)
//        )


        marker?.tag = name
        map.setOnMapLoadedCallback {
          map.snapshot {
            mapBitmap.value = it
          }
        }
        Log.d("mapDebug", marker.toString())
//        sleep(5000)
//        marker.remove()
//        map.snapshot {
//          mapBitmap.value = it
//        }
      }
    }
//  }


  
  DisposableEffect(key1 = location) {
    Log.d("mapDebug","再描画")

    mapBitmap.value = null
    onDispose {
      mapBitmap.value = null
    }
  }
}

fun log(text:String) {
  Log.d(debugTag, text)
}
fun err(text:String) {
  Log.e(debugTag, text)
}