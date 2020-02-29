package cn.jzvd.demo;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;

import androidx.annotation.NonNull;


/**
 * Created by HRR on 2020/02/22.
 */
public class ScreenRotateUtils {
    private Activity mActivity;
    private boolean isOpenSensor = true;      // 是否打开传输，默认打开
    private boolean isEffectSysSetting = true;   // 手机系统的重力感应设置是否生效，默认无效，想要生效改成true就好了
    private SensorManager sm;
    private OrientationSensorListener listener;
    private OrientationChangeListener changeListener;
    private Sensor sensor;
    public static float orientationDirection;
    private static int DATA_X = 0;
    private static int DATA_Y = 1;
    private static int DATA_Z = 2;
    private static int ORIENTATION_UNKNOWN = -1;
    private static ScreenRotateUtils instance;

    public ScreenRotateUtils(Context context) {
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        listener = new OrientationSensorListener(mHandler);
    }

    public static ScreenRotateUtils getInstance(Context context) {
        if (instance == null) {
            instance = new ScreenRotateUtils(context);
        }
        return instance;
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 888) {
                int orientation = msg.arg1;
                /**
                 * 根据手机屏幕的朝向角度，来设置内容的横竖屏，并且记录状态
                 */
                changeListener.orientationChange(orientation);
            }
        }
    };

    void setOrientationChangeListener(OrientationChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    interface OrientationChangeListener {
        void orientationChange(int orientation);
    }

    class OrientationSensorListener implements SensorEventListener {
        private Handler rotateHandler;

        public OrientationSensorListener(Handler rotateHandler) {
            this.rotateHandler = rotateHandler;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            int orientation = ORIENTATION_UNKNOWN;
            float x = -values[DATA_X];
            orientationDirection=-x;
            float y = -values[DATA_Y];
            float z = -values[DATA_Z];
            float magnitude = x * x + y * y;
            if (magnitude * 4 >= z * z) {
                float oneEightyOverPi = 57.29577957855f;
                float angle = (float) (Math.atan2(-y,x) * oneEightyOverPi);

                orientation = 90 - Math.round(angle);
                // normalize to 0 - 359 range
                while (orientation >= 360) {
                    orientation -= 360;
                }
                while (orientation < 0) {
                    orientation += 360;
                }
            }

            /**
             * 获取手机系统的重力感应开关设置，这段代码看需求，不要就删除
             * screenchange = 1 表示开启，screenchange = 0 表示禁用
             * 要是禁用了就直接返回
             */
            if (isEffectSysSetting) {
                try {
                    int isRotate = Settings.System.getInt(mActivity.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
                    // 如果用户禁用掉了重力感应就直接return
                    if (isRotate == 0) {
                        return;
                    }
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
                // 判断是否要进行中断信息传递
                if (!isOpenSensor) {
                    return;
                }
//                rotateHandler.obtainMessage(888, orientation, 0).sendToTarget();
                changeListener.orientationChange(orientation);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    /**
     * 开启监听
     * 绑定切换横竖屏Activity的生命周期
     *
     * @param activity
     */
    void start(Activity activity) {
        mActivity = activity;
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * 注销监听
     */
    void stop() {
        sm.unregisterListener(listener);
        mActivity = null;  // 防止内存泄漏
    }

}
