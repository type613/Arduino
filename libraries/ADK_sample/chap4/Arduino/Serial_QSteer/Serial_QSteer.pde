/**
 * @Author Iwata, N
 */

/** ①赤外線LEDをつなぐポートの定義 */
#define IR_LED      (2)

/** ②バンドデータの定義 */
#define BAND_A      (B00)    /* バンドA */
#define BAND_B      (B01)    /* バンドB */
#define BAND_C      (B10)    /* バンドC */
#define BAND_D      (B11)    /* バンドD */

/** ③各ビットのデータ長 */
#define BASE_TIME   (400)    /* OFFの時間 */
#define HIGH_TIME   (930)    /* 1の時に赤外線を送信する時間 */
#define LOW_TIME    (430)    /* 0の時に赤外線を送信する時間 */
#define START_BIT   (1740)   /* スタートビットとして赤外線を送信する時間 */
#define A_DELAY     (7540)   /* バンドAの時のウェイト時間 */
#define B_DELAY     (21850)  /* バンドBの時のウェイト時間 */
#define C_DELAY     (36200)  /* バンドCの時のウェイト時間 */
#define D_DELAY     (50550)  /* バンドDの時のウェイト時間 */
#define T           (1000000/38000)

/**
 * 初期化
 */
void setup() {
  /* ④シリアル通信の開始 */
  Serial.begin(9600);
  Serial.print("Start\r\n");
  
  /* ⑤ピンモードの設定 */
  pinMode(IR_LED, OUTPUT);
}


/**
 * ループ
 */
void loop() {

  /* ⑥シリアルの受信 */
  if (Serial.available() > 0) {
    int data = Serial.read();
    if (data == '@') {
      byte band = Serial.read();
      byte data = Serial.read();
      sendData(band, data);
    }
  }
  
  delay(10);
}


/**
 * 赤外線LEDの制御
 *
 * @param[in] level 出力レベル
 * @param[in] time 出力時間
 */
void ctrlIr(int level, int time) {
  
  /* ⑦開始時間を測定 */
  unsigned long start = micros();
  
  /* ⑧指定時間の間、38KHzで赤外線LEDを点滅する */
  do {
    digitalWrite(IR_LED, level);
    delayMicroseconds(T/2);
    digitalWrite(IR_LED, LOW);
    delayMicroseconds(T/2);
  } while (long(start + time - micros()) > 0);
}


/**
 * データ送信
 *
 * @param[in] data データ
 */
void sendData(byte band, byte data) {

  for(int cnt = 0; cnt<2; cnt++) {
    /* ⑨スタートビット送信 */
    ctrlIr(HIGH, START_BIT);
    
    /* ⑩バンド部分送信 */
    for(int i=0; i<2; i++) {
      int b = bitRead(band, 1-i);
      ctrlIr(LOW, BASE_TIME);
      if (b == 0) {
        ctrlIr(HIGH, LOW_TIME);
      } else {
        ctrlIr(HIGH, HIGH_TIME);
      }
    }
    
    /* ⑪データ部分送信 */
    for(int i=0; i<4; i++) {
      int b = bitRead(data, 3-i);
      ctrlIr(LOW, BASE_TIME);
      if (b == 0) {
        ctrlIr(HIGH, LOW_TIME);
      } else {
        ctrlIr(HIGH, HIGH_TIME);
      }
    }
    
    /* ⑫データ後の通信がない時間 */
    switch(band) {
      case BAND_B:
        ctrlIr(LOW, B_DELAY);
        break;
        
      case BAND_C:
        ctrlIr(LOW, C_DELAY);
        break;
        
      case BAND_D:
        ctrlIr(LOW, D_DELAY);
        break;
      
      case BAND_A:
      default:
        ctrlIr(LOW, A_DELAY);
        break;
    }
  }
}

