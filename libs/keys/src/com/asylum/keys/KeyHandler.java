/*
 * Copyright (C) 2014 Slimroms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asylum.keys;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.asylum.keys.TouchscreenGestureParser.Gesture;
import com.asylum.keys.TouchscreenGestureParser.GesturesArray;

import com.android.internal.util.ArrayUtils;

import asylum.provider.AsylumSettings;
import com.asylum.action.Action;
import com.asylum.action.ActionConstants;


public class KeyHandler {

    private static final String TAG = KeyHandler.class.getSimpleName();
    private static final int GESTURE_REQUEST = 1;

    private final Context mContext;
    private final PowerManager mPowerManager;
    private Resources mSettingsResources = null;
    private EventHandler mEventHandler;
    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    private Vibrator mVibrator;
    WakeLock mProximityWakeLock;
    private TelecomManager mTelecomManager;

    private GesturesArray mGestures;

    public KeyHandler(Context context) {
        mContext = context;
        mEventHandler = new EventHandler();
        mTelecomManager = TelecomManager.from(mContext);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "ProximityWakeLock");

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            mVibrator = null;
        }

        try {
            mSettingsResources = mContext.createPackageContext(
                    "com.android.settings", Context.CONTEXT_IGNORE_SECURITY).getResources();
        } catch (NameNotFoundException e) {
        }

        mGestures = TouchscreenGestureParser.parseGestures(mContext);
    }

    private class EventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            KeyEvent event = (KeyEvent) msg.obj;
            String action = getScreenOffGesturePref(event.getScanCode());

            if (action == null || action != null && action.equals(ActionConstants.ACTION_NULL)) {
                return;
            }
            doHapticFeedback();
            Action.processAction(mContext, action, false);
        }
    }

    public static String buildPreferenceKey(int scanCode) {
        return "gesture_" + Integer.toString(scanCode);
    }

    private String getScreenOffGesturePref(int scanCode) {
        String action = AsylumSettings.System.getStringForUser(mContext.getContentResolver(),
                buildPreferenceKey(scanCode), UserHandle.USER_CURRENT);
        if (TextUtils.isEmpty(action)) {
            return mGestures.get(scanCode).def;
        }
        return action;
    }

    private void doHapticFeedback() {
        if (mVibrator == null) {
            return;
        }
            mVibrator.vibrate(50);
    }

    public boolean handleKeyEvent(KeyEvent event) {
        if (isDisabledByPhoneState()) {
            return false;
        }

        int action = event.getAction();
        int scanCode = event.getScanCode();
        int repeatCount = event.getRepeatCount();

        if (scanCode <= 0 || action != KeyEvent.ACTION_UP || repeatCount != 0) {
            return false;
        }

        boolean isKeySupported = isKeySupported(event.getScanCode());
        if (isKeySupported && !mEventHandler.hasMessages(GESTURE_REQUEST)) {
            Message msg = getMessageForKeyEvent(event);
            if (mProximitySensor != null) {
                mEventHandler.sendMessageDelayed(msg, 200);
                processEvent(event);
            } else {
                mEventHandler.sendMessage(msg);
            }
        }
        return isKeySupported;
    }

    private boolean isKeySupported(int scanCode) {
        return mGestures.get(scanCode) != null;
    }

    private Message getMessageForKeyEvent(KeyEvent keyEvent) {
        Message msg = mEventHandler.obtainMessage(GESTURE_REQUEST);
        msg.obj = keyEvent;
        return msg;
    }

    private boolean isDisabledByPhoneState() {
        if (mTelecomManager != null) {
            return mTelecomManager.isInCall() || mTelecomManager.isRinging();
        }
        return false;
    }


    private void processEvent(final KeyEvent keyEvent) {
        mProximityWakeLock.acquire();
        mSensorManager.registerListener(new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent event) {
                mProximityWakeLock.release();
                mSensorManager.unregisterListener(this);
                if (!mEventHandler.hasMessages(GESTURE_REQUEST)) {
                    // The sensor took to long, ignoring.
                    return;
                }
                mEventHandler.removeMessages(GESTURE_REQUEST);
                if (event.values[0] == mProximitySensor.getMaximumRange()) {
                    Message msg = getMessageForKeyEvent(keyEvent);
                    mEventHandler.sendMessage(msg);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        }, mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

}
