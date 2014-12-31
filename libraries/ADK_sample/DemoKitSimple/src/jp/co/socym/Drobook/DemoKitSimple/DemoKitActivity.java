package jp.co.socym.Drobook.DemoKitSimple;

import java.io.IOException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.future.usb.UsbAccessory;

public class DemoKitActivity extends AdkBaseActivity implements Runnable {
	// 本アプリを識別するためのインテントアクション名
	private static final String mActionUsbPermission = "jp.co.socym.Drobook.DemoKitSimple.action.USB_PERMISSION";

	private TextView mTxt0;
	private TextView mTxt1;
	private TextView mTxt2;

	private static final String TAG = "Socym.DemoKitSimple";

	// コンストラクタ
	public DemoKitActivity() {
		// インテントアクション名をAdkBaseActivityに通知する(1)
		super(mActionUsbPermission);
	}

	// アクティビティ起動時の処理 (2)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "[DemoKitActivity] onCreate");
		//Accesoryが既に接続されいるか？
		if (super.isAccessoryOpen()) {
			// Accesory が既にオープンしているならば、コントロールGUI表示
			showControls();
		} else {
			// Accesory が既にオープンしていなければ、待ち画面の表示
			hideControls();
		}
	}

	// ADKボードが接続されたときの処理 (3)
	// AdkBaseActivityからの継承
	@Override
	protected void openAccessory(UsbAccessory accessory) {
		super.openAccessory(accessory);

		if (super.isOpened()) {
			Log.d(TAG, "[DemoKitActivity] accessory opened");
			// ADKボードをオープン出来たなら受信スレッドを開始
			Thread thread = new Thread(null, this, "DemoKit");
			thread.start();
			// コントロールUI表示
			showControls();
		} else {
			Log.d(TAG, "[DemoKitActivity] accessory open fail");
		}
	}

	// ADKボードが切断されたときの処理 (4)
	// AdkBaseActivityからの継承
	@Override
	protected void closeAccessory() {
		Log.d(TAG, "[DemoKitActivity] closeAccessory");
		hideControls();
		super.closeAccessory();
	}

	// ADK通信受信処理1/2 (5)
	// ADKとの通信の受信スレッド
	@Override
	public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		while (ret >= 0) {
			try {
				// 受信
				ret = super.readBuffer(buffer);
			} catch (IOException e) {
				break;
			}

			// 入力したバッファ分読みとる
			i = 0;// buf[]の位置
			while (i < ret) {
				int len = ret - i;

				// バッファをモニタリングして内容によって、コントロールの表示を更新する
				byte type=buffer[i];
				switch (type) {
				case 0x1:
					if (len >= 3) {
						// ハンドラに渡すメッセージの作成
						Message msg = Message.obtain(mHandler);
						// メッセージの中身
						byte sw = buffer[i + 1];
						byte state = buffer[i + 2];
						msg.obj = new SwitchMsg(sw, state);
						mHandler.sendMessage(msg);
					}
					i += 3;
					break;

				default:
					// Log.d(TAG, "unknown msg: " + buffer[i]);
					i = len;
					break;
				}
			}

		}
	}

	// ADK通信受信処理2/2 (6)
	// GUI用のハンドラ
	// 上記スレッドrun()から呼ばれる。（GUIにはスレッドからアクセスできないため）
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// スレッドから渡されたメッセージを展開
			SwitchMsg o = (SwitchMsg) msg.obj;
			byte sw = o.getSw();
			byte state = o.getState();

			//TextViewの表示の切り替え
			if (sw == 0) {
				if (state == 0x1) {
					mTxt0.setText("SW1 = ON");
				} else {
					mTxt0.setText("SW1 = OFF");
				}
			}
			if (sw == 1) {
				if (state == 0x1) {
					mTxt1.setText("SW2 = ON");
				} else {
					mTxt1.setText("SW2 = OFF");
				}
			}
			if (sw == 2) {
				if (state == 0x1) {
					mTxt2.setText("SW3 = ON");
				} else {
					mTxt2.setText("SW3 = OFF");
				}
			}
		}
	};

	// ADK通信送信関数 (7)
	private void sendCommand(byte command, byte target, int value) {
		if (value > 255) {
			value = 255;
		}

		//送信データの格納
		byte[] buffer = new byte[3];
		buffer[0] = command;
		buffer[1] = target;
		buffer[2] = (byte) value;

		try {
			// 送信
			super.writeBuffer(buffer);
		} catch (IOException e) {
			Log.e(TAG, "write failed", e);
		}
	}

	// スイッチのステータス
	private class SwitchMsg {
		private byte sw;
		private byte state;

		public SwitchMsg(byte sw, byte state) {
			this.sw = sw;
			this.state = state;
		}

		public byte getSw() {
			return sw;
		}

		public byte getState() {
			return state;
		}
	}

	// USBが繋がっているときのLayout表示 (8)
	protected void showControls() {
		setContentView(R.layout.main);

		Button btn1 = (Button) findViewById(R.id.btnTest1);
		Button btn2 = (Button) findViewById(R.id.btnTest2);
		Button btn3 = (Button) findViewById(R.id.btnTest3);

		mTxt0 = (TextView) findViewById(R.id.txtTest0);
		mTxt1 = (TextView) findViewById(R.id.txtTest1);
		mTxt2 = (TextView) findViewById(R.id.txtTest2);

		// ボタンを押したときの処理
		btn1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendCommand((byte) 2, (byte) 0, 10);// LED1 RED ON
				sendCommand((byte) 2, (byte) 3, 0);// LED2 RED OFF
				sendCommand((byte) 2, (byte) 6, 0);// LED3 RED OFF
			}
		});
		btn2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendCommand((byte) 2, (byte) 0, 0);// LED1 RED OFF
				sendCommand((byte) 2, (byte) 3, 10);// LED2 RED ON
				sendCommand((byte) 2, (byte) 6, 0);// LED3 RED OFF
			}
		});
		btn3.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendCommand((byte) 2, (byte) 0, 0);// LED1 RED OFF
				sendCommand((byte) 2, (byte) 3, 0);// LED2 RED OFF
				sendCommand((byte) 2, (byte) 6, 10);// LED3 RED ON
			}
		});
	}

	// USBが繋がっていないときのLayout表示 (9)
	protected void hideControls() {
		setContentView(R.layout.no_device);
	}
}
