
package jp.co.socym.dorobook.qsteer;

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

/**
 * ADKボードとの通信処理を行うクラス
 * 
 * @author Iwata Naoki
 */
public class BaseActivity extends Activity {

    /** Logのタグ */
    private static final String TAG = BaseActivity.class.getSimpleName();

    /** パーミッション */
    private static final String ACTION_USB_PERMISSION = "jp.co.socym.dorobook.qsteer.action.USB_PERMISSION";

    /** パーミッション確認のインテント */
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;

    /** USBマネージャー */
    private UsbManager mUsbManager;

    /** 現在通信中のUSBアクセサリ */
    private UsbAccessory mAccessory;

    private ParcelFileDescriptor mFileDescriptor;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;

    /** ブロードキャストレシーバー */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, action);

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = UsbManager.getAccessory(intent);
                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory);
                    } else {
                        Log.d(TAG, "permission denied for accessory "
                                + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = UsbManager.getAccessory(intent);
                if (accessory != null && accessory.equals(mAccessory)) {
                    closeAccessory();
                }
            }
        }
    };

    /**
     * USBアクセサリとの通信を開始
     * 
     * @param accessory
     */
    protected void openAccessory(UsbAccessory accessory) {

        mFileDescriptor = mUsbManager.openAccessory(accessory);

        if (mFileDescriptor != null) {
            mAccessory = accessory;

            // 通信用のファイルディスクリプタを開く
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);

            Log.d(TAG, "accessory opened");
        } else {
            Log.d(TAG, "accessory open fail");
        }
    }

    /**
     * ADKとの通信を終了
     */
    protected void closeAccessory() {
        Log.d(TAG, "closeAccessory");

        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
            if (mInputStream != null) {
                mInputStream.close();
            }
            if (mOutputStream != null) {
                mOutputStream.close();
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mOutputStream = null;
            mInputStream = null;
            mAccessory = null;
        }
    }

    protected int readBuffer(byte[] buffer) throws IOException {
        int ret = mInputStream.read(buffer);
        return ret;
    }

    /**
     * ADKにデータを送信する
     * 
     * @param data
     * @throws IOException
     */
    public void writeBuffer(byte[] data) throws IOException {
        if (mOutputStream != null) {
            mOutputStream.write(data);
        }
    }

    /**
     * アプリ開始時にコールされる。USBマネージャーのインスタンス取得、Intentを設定し、
     * USBアクセサリが接続された際のIntentを取得するブロードキャストレシーバーを設定する
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // USBマネージャーのインスタンスを取得
        mUsbManager = UsbManager.getInstance(this);

        // アクセサリに関するIntentを設定
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);

        // ブロードキャストレシーバーへのIntentの登録
        registerReceiver(mUsbReceiver, filter);

        if (getLastNonConfigurationInstance() != null) {
            mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
            openAccessory(mAccessory);
        }

    }

    /**
     * アプリがレジューム時にコールされる。USBアクセサリへのアクセスを開始する
     */
    @Override
    public void onResume() {
        super.onResume();

        // すでにストリームが開かれていた場合は何もしない
        if (mInputStream != null && mOutputStream != null) {
            return;
        }

        // 接続されているUSBアクセサリのリストを取得
        UsbAccessory[] accessories = mUsbManager.getAccessoryList();

        // 接続されているUSBアクセサリが無ければnull、１つ以上あれば最初の１つを取得
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);

        if (accessory != null) {
            // USBアクセサリにアクセスするパーミッションがあるか確認
            if (mUsbManager.hasPermission(accessory)) {
                Log.d(TAG, "hasPermission");

                // USBアクセサリへのアクセスを開始
                openAccessory(accessory);
            } else {
                Log.d(TAG, "not hasPermission");

                // パーミッションの要求
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory,
                                mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            // USBアクセサリが接続されていないか、認識できていない
            Log.d(TAG, "mAccessory is null");
        }
    }

    /**
     * 状態が変化した際のインスタンスを取得する
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mAccessory != null) {
            return mAccessory;
        } else {
            return super.onRetainNonConfigurationInstance();
        }
    }

    /**
     * アプリがポーズ時にコールされる。USBアクセサリへのアクセスを終了する
     */
    @Override
    public void onPause() {
        super.onPause();
        closeAccessory();
    }

    /**
     * アプリが終了時にコールされる。 USBアクセサリが接続された際のIntentを取得するブロードキャストレシーバーを解除する
     */
    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

}
