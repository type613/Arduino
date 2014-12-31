#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

AndroidAccessory acc("Dorobook, Socym",
"AdkNoSuchAppTitle", //存在しないアプリのタイトル名
"このAndroidはADKが使えます ADK Connection Test  ", //ダイアログで表示されるメッセージ
"1.0",
"http://accessories.android.com/",
"0000000012345678");

void setup()
{
  Serial.begin(115200);
  Serial.print("\r\nStart");
  //Androidを起動／接続する命令を送る
  acc.powerOn();
}

void loop()
{
  if (acc.isConnected()) {
    //communicate with Android application
  }
  else{
    //set the accessory to its default state
  }
  delay(10);
}


