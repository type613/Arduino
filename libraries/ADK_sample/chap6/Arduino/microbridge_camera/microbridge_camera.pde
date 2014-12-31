#include <SPI.h>
#include <Adb.h>

// (1) グローバル変数定義
Connection* shell;
int shatterFlag = 0;
int connectFlag = 0;

/**
 * (2) ADBのイベントハンドラ
 *
 * @param[in] connection : イベントが発生した接続
 * @param[in] event      : 発生したイベントの内容
 * @param[in] length     : 受信したデータの長さ
 * @param[in] data       : 受信したデータ
 */
void adbEventHandler(Connection * connection, adb_eventType event, uint16_t length, uint8_t * data)
{
  int i;

  if (event == ADB_CONNECTION_RECEIVE) {
    for (i=0; i<length; i++) {
      Serial.print(data[i]);
    }
  }
}


void setup()
{
  // (3) ピンモードの設定
  pinMode(2, INPUT);

  // シリアル通信の開始 
  Serial.begin(115200);

  // (4)ADBサブシステムを初期化  
  ADB::init()
  // ADB用のストリームをオープン
  shell = ADB::addConnection("shell:", true, adbEventHandler);  
}


void loop()
{
  // (5)ADB用のストリームがオープンしている時にAndroid上のshellへ書き込み 
  if (shell->status == ADB_OPEN) {
    // (6)スイッチを押した際に処理
    if (digitalRead(2) == HIGH) {
      if (shatterFlag == 0) {
        // カメラボタンを押したイベントを発行
        shell->writeString("input keyevent 27\n");
        Serial.println("HIGH!!");
      }
      shatterFlag = 1;
    } else {
      shatterFlag = 0;
    }
    
    connectFlag = 0;  
      
  } else {
    if (connectFlag == 0) {
      Serial.println("Shell not open");
    }
    connectFlag = 1;  
    shatterFlag = 0;
  }

  // ADB用ストリームのポーリング
  ADB::poll();
  
  delay(20);
}

