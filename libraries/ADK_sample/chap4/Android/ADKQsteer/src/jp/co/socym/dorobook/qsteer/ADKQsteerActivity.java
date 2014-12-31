/**
 * 
 */

package jp.co.socym.dorobook.qsteer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.android.future.usb.UsbAccessory;

import jp.co.socym.dorobook.qsteer.R;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ToggleButton;

/**
 * チョロQの操作画面
 */
public class ADKQsteerActivity extends BaseActivity implements OnTouchListener, Runnable {

    /**
     * ①制御用のデータクラス
     */
    private class CtrlData {
        byte normal = 0;
        byte turbo = 0;

        public CtrlData(byte normal, byte turbo) {
            this.normal = normal;
            this.turbo = turbo;
        }

        public byte getValue() {
            byte ret = normal;
            if (tglTurbo.isChecked()) {
                ret = this.turbo;
            }
            return ret;
        }
    }

    /** ②制御データの定義 */
    private static final byte CTRL_STOP = 0; // 停止
    private static final byte CTRL_FORWARD = 1; // 前進
    private static final byte CTRL_BACK = 2; // 後退
    private static final byte CTRL_LEFT = 3; // ステアリング左
    private static final byte CTRL_RIGHT = 4; // ステアリング右
    private static final byte CTRL_TURBO_FORWARD = 5; // ターボで前進
    private static final byte CTRL_FORWARD_LEFT = 6; // 左折
    private static final byte CTRL_FORWARD_RIGHT = 7; // 右折
    private static final byte CTRL_TURBO_FORWARD_LEFT = 8; // ターボで左折
    private static final byte CTRL_TURBO_FORWARD_RIGHT = 9; // ターボで右折
    private static final byte CTRL_LEFT_BACK = 10; // 左後退
    private static final byte CTRL_RIGHT_BACK = 11; // 右後退
    private static final byte CTRL_TURBO_BACK = 12; // ターボで後退
    private static final byte CTRL_TURBO_LEFT_BACK = 13; // ターボで左後退
    private static final byte CTRL_TURBO_RIGHT_BACK = 14; // ターボで右後退

    /** ③制御データのリスト */
    private List<CtrlData> ctrlDataList = new ArrayList<CtrlData>();

    /** ④操作用のView */
    private ToggleButton tglTurbo;

    /** ⑤バンドデータ */
    private byte mBand = 0;

    /** ADKボードとの通信用スレッド */
    Thread thread = new Thread(null, this, "Qsteer");
    private boolean mRunFlag = false;

    /**
     * ⑥ADKにコマンドを送信する
     * 
     * @param id 制御データの番号
     */
    private void sendCommand(int id) {

        // リストからidに一致するデータを取得する
        CtrlData ctrl = ctrlDataList.get(id);

        // ⑦制御データクラスから送信するデータを生成する
        byte[] data = {
                mBand, ctrl.getValue()
        };

        try {
            // ⑧BaseActivity#writeメソッドを使用してADKへデータを送信
            writeBuffer(data);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Viewにタッチイベントを登録する
     * 
     * @param id ViewのID
     */
    private void setTouchEvent(int id) {
        View v = findViewById(id);
        v.setOnTouchListener(this);
    }

    /**
     * Activity生成時にコールされるメソッド
     * 
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        tglTurbo = (ToggleButton) findViewById(R.id.tgl_turbo);

        // ⑨制御データ用のリストに制御データクラスを追加する
        ctrlDataList.add(new CtrlData(CTRL_STOP, CTRL_STOP));
        ctrlDataList.add(new CtrlData(CTRL_FORWARD, CTRL_TURBO_FORWARD));
        ctrlDataList.add(new CtrlData(CTRL_FORWARD_RIGHT, CTRL_TURBO_FORWARD_RIGHT));
        ctrlDataList.add(new CtrlData(CTRL_RIGHT, CTRL_RIGHT));
        ctrlDataList.add(new CtrlData(CTRL_RIGHT_BACK, CTRL_TURBO_RIGHT_BACK));
        ctrlDataList.add(new CtrlData(CTRL_BACK, CTRL_TURBO_BACK));
        ctrlDataList.add(new CtrlData(CTRL_LEFT_BACK, CTRL_TURBO_LEFT_BACK));
        ctrlDataList.add(new CtrlData(CTRL_LEFT, CTRL_LEFT));
        ctrlDataList.add(new CtrlData(CTRL_FORWARD_LEFT, CTRL_TURBO_FORWARD_LEFT));

        // ⑩タッチイベントのコールバックを登録する
        setTouchEvent(R.id.btn_forward);
        setTouchEvent(R.id.btn_right_forward);
        setTouchEvent(R.id.btn_right);
        setTouchEvent(R.id.btn_right_back);
        setTouchEvent(R.id.btn_back);
        setTouchEvent(R.id.btn_left_back);
        setTouchEvent(R.id.btn_left);
        setTouchEvent(R.id.btn_left_forward);
        setTouchEvent(R.id.btn_stop);

        // ⑪ラジオボタンの状態変化に応じてバンドを変更する
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                View v = findViewById(checkedId);
                if (v.getTag() == null) {
                    return;
                }
                byte band = Byte.parseByte((String) v.getTag());
                if (0 <= band && band <= 3) {
                    mBand = band;
                }
            }
        });

    }

    /**
     * ⑫ボタンクリック時の動作
     * 
     * @param v
     */
    public void onClickCtrl(View v) {
        // int id = Integer.parseInt((String) v.getTag());
        // sendCommand(id);
    }

    /**
     * ⑬タッチ時の動作
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        // main.xmlでViewに登録したタグを取得しint型に変換
        int id = Integer.parseInt((String) v.getTag());

        // 指を離した時は停止信号、それ以外の時はViewに登録したタグを送信
        if (event.getAction() == MotionEvent.ACTION_UP) {
            sendCommand(CTRL_STOP);
        } else {
            sendCommand(id);
        }

        return false;
    }

    /**
     * ⑭ADKボードとの通信が開始された際にコールされるメソッド
     */
    @Override
    protected void openAccessory(UsbAccessory accessory) {
        super.openAccessory(accessory);

        // ADKボードからデータを受信するスレッドを生成
        mRunFlag = true;
        thread = new Thread(null, this, "DemoKit");
        thread.start();
    }

    /**
     * ⑮ADKボードとの通信が切断された際にコールされるメソッド
     */
    @Override
    protected void closeAccessory() {

        // ADKボードからデータを受信するスレッドを終了
        mRunFlag = false;
        try {
            thread.join();
        } catch (InterruptedException e1) {
            Thread.interrupted();
        }

        super.closeAccessory();
    }

    /**
     * ⑯ADKボードからのデータを受信するスレッド
     */
    @Override
    public void run() {

        byte[] buffer = new byte[3];

        // フラグがfalseになるまでループを繰り返す
        while (mRunFlag) {
            try {
                // InputStreamからデータを読み込む
                super.readBuffer(buffer);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
