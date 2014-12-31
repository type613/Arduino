package jp.co.socym.Drobook.DemoKitSimple;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class AdkBaseActivity extends Activity {
	private static final String TAG = "Socym.DemoKitSimple";
	private UsbManager mUsbManager;
	private ParcelFileDescriptor mFileDescriptor;
	private UsbAccessory mAccessory;

	private FileInputStream mInputStream;
	private FileOutputStream mOutputStream;

	public final String ACTION_USB_PERMISSION;
	private boolean mPermissionRequestPending;
	private PendingIntent mPermissionIntent;

	public AdkBaseActivity(final String a) {
		// ACTION_USB_PERMISSIONの定義 (1)
		ACTION_USB_PERMISSION = a;
	}

	// USB接続状態を監視するBroadcastReceiver mUsbReceiver(2)
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "[AdkBaseActivity.mUsbReceiver] ");
			String action = intent.getAction();
			Log.i(TAG, "[AdkBaseActivity.mUsbReceiver] action=" + action);

			// ACTION_USB_PERMISSIONの場合 (2.1)
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					// intentからaccessoryを取得
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					// 接続許可ダイアログで OK=true, Cancel=false のどちらかを押したか
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						Log.i(TAG, "[AdkBaseActivity.mUsbReceiver] pressed OK");
						openAccessory(accessory);
					} else {
						Log.i(TAG,
								"[AdkBaseActivity.mUsbReceiver] pressed Cancel");
					}
					mPermissionRequestPending = false;
				}
				// ADK BoardがUSBコネクタから外された場合 (2.2)
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				// 接続中のaccesoryか
				Log.i(TAG, "[AdkBaseActivity.mUsbReceiver] USB detached 1");
				if (accessory != null && accessory.equals(mAccessory)) {
					// 接続中のaccesoryなら閉じる
					Log.i(TAG,
							"[AdkBaseActivity.mUsbReceiver] USB detached 2 close ADK");
					closeAccessory();
				}
			}
		}
	};

	// アプリ起動時の処理 onCreate(3)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "[AdkBaseActivity] onCreate");

		// USB Managerの取得(3.1)
		mUsbManager = UsbManager.getInstance(this);
		// パーミッションインテントの作成 (3.2)
		// アプリから送信する
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);

		// ブロードキャストレシーバで受信するインテントを登録(3.3)
		// USB Accesoryが接続/切断されたときに呼ばれる
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		// USB Accesoryが切断されたとき
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		// 画面の縦横向きなどが変化したときの復帰処理(3.4)
		if (getLastNonConfigurationInstance() != null) {
			// onRetainNonConfigurationInstance()でキャッシュしたmAccessoryものを復帰
			mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
			openAccessory(mAccessory);
		}
	}

	// アプリ起動時の処理 onResume(4)
	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "[AdkBaseActivity] onResume");

		// 既に通信しているか(4.1)
		if (mInputStream != null && mOutputStream != null) {
			return;
		}

		// 接続されているアクセサリの確認(4.2)
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				// 接続許可されているならば、アプリ起動(4.3)
				Log.d(TAG, "[AdkBaseActivity] onResume : hasPermission ture");
				openAccessory(accessory);
			} else {
				Log.d(TAG, "[AdkBaseActivity] onResume : hasPermission false");
				// 接続許可されていないのならば、パーミッションインテント発行(4.4)
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						Log.d(TAG,
								"[AdkBaseActivity] onResume : mPermissionRequestPending start");
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "[AdkBaseActivity] onResume : mAccessory is null");
		}
	}

	// 画面の縦横向きなどが変化したときに現在の状態を保持 (5)
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mAccessory != null) {
			return mAccessory;
		} else {
			return super.onRetainNonConfigurationInstance();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "[AdkBaseActivity] onPause");
		closeAccessory();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		Log.d(TAG, "[AdkBaseActivity] onPause");
		super.onDestroy();
	}

	// アクセサリ開始処理 (6)
	protected void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
		}
	}

	// アクセサリ終了処理 (7)
	protected void closeAccessory() {
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}

	// 通信オープンしたか
	protected boolean isOpened() {
		if (mFileDescriptor != null) {
			return true;
		} else {
			return false;
		}
	}

	// アクセサリーボードを見つけたか
	protected boolean isAccessoryOpen() {
		if (mAccessory != null) {
			return true;
		} else {
			return false;
		}
	}

	// Android←ADK Boardの受信 (8)
	protected int readBuffer(byte[] buffer) throws IOException {
		int ret = mInputStream.read(buffer);
		return ret;
	}

	// Android→ADK Boardへ送信 (9)
	protected void writeBuffer(byte[] buffer) throws IOException {
		if (mOutputStream != null && buffer[1] != -1 && buffer.length >= 3) {
			mOutputStream.write(buffer);
		}
	}

}
