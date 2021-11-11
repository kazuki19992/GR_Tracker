package com.kazuki19992.tracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
//import androidx.compose.material.Text
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazuki19992.tracker.ui.theme.TrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrackerTheme {
                TopView()
            }
        }
    }
}

//@Composable
//fun Greeting(name: String) {
//    Text(text = "Hello $name!")
//}

@Composable
fun TopView() {
  val received by remember { mutableStateOf("読込中です……") }
  Log.d("APPS_Debug", "起動完了")
  // トップ画面
  Column {
    Header()
    SerialTerm(received)
  }
}

@Composable
fun Header() {
  TopAppBar(
    title = { Text("Tracker") }
  )
}

@Composable
fun SerialTerm(received: String) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(15.dp)
      .height(200.dp)
      .clickable {  },
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

//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    AndroidTutorialTheme {
//        Greeting("Android")
//    }
//}
