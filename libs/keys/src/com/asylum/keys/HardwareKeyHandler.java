/*
 * Copyright (C) 2016-2018 SlimRoms Project
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

import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.dreams.DreamManagerInternal;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.view.HapticFeedbackConstants;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import java.util.Arrays;
import java.util.HashMap;

import asylum.provider.AsylumSettings;

import com.asylum.action.Action;
import com.asylum.action.ActionConstants;
import com.asylum.action.ActionsManager;

import com.asylum.keys.parser.Key;
import com.asylum.keys.parser.KeyCategory;
import com.asylum.keys.parser.KeyParser;

public class HardwareKeyHandler {

    private static final String TAG = "HardwareKeyHandler";

    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .build();

    private ArrayMap<Category, ArrayMap<Integer, Button>> mButtons = new ArrayMap<>();

    // Custom hardware key rebinding
    private boolean mKeysDisabled;
    private boolean mDisableVibration;
    private boolean mPreloadedRecentApps;

    private Context mContext;
    private Handler mHandler;
    private DreamManagerInternal mDreamManagerInternal;
    private Vibrator mVibrator;

    private long[] mLongPressVibePattern;
    private long[] mVirtualKeyVibePattern;

    private HwKeySettingsObserver mHwKeySettingsObserver;

    private class HwKeySettingsObserver extends ContentObserver {
        HwKeySettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            for (Category category : mButtons.keySet()) {
                category.observe(this, resolver);
                category.updateAssignments();
                for (Button button : mButtons.get(category).values()) {
                    button.observe(this, resolver);
                    button.updateAssignments();
                }
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            for (Category category : mButtons.keySet()) {
                category.updateAssignments();
                for (Button button : mButtons.get(category).values()) {
                    button.updateAssignments();
                }
            }
        }
    }

    public HardwareKeyHandler(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;

        mLongPressVibePattern = getLongIntArray(mContext.getResources(),
                com.android.internal.R.array.config_longPressVibePattern);
        mVirtualKeyVibePattern = getLongIntArray(mContext.getResources(),
                com.android.internal.R.array.config_virtualKeyVibePattern);

        mVibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);

       for (KeyCategory category : KeyParser.parseKeys(mContext).values()) {
           Category cat = new Category(category.key);
           mButtons.put(cat, new ArrayMap<Integer, Button>());
           for (Key key : category.keys) {
               if (key.supportsMultipleActions) {
                   mButtons.get(cat).put(key.keyCode, new MultiFunctionButton(key.keyCode));
               } else {
                   mButtons.get(cat).put(key.keyCode,
                           new SingleFunctionButton(key.keyCode, key.def));
               }
           }
       }

        mHwKeySettingsObserver = new HwKeySettingsObserver(mHandler);
        mHwKeySettingsObserver.observe();
    }

    static long[] getLongIntArray(Resources r, int resid) {
        int[] ar = r.getIntArray(resid);
        if (ar == null) {
            return null;
        }
        long[] out = new long[ar.length];
        for (int i=0; i<ar.length; i++) {
            out[i] = ar[i];
        }
        return out;
    }

    public boolean isHwKeysDisabled() {
        return mKeysDisabled;
    }

    private boolean isKeyDisabled(int keyCode) {
        for (Category cat : mButtons.keySet()) {
            if (mButtons.get(cat).containsKey(keyCode) && cat.mDisabled) {
                return true;
            }
        }
        return false;
    }

    public boolean handleKeyBeforeQueueing(KeyEvent event, boolean keyguardOn, boolean interactive) {
        return handleKeyEvent(event, keyguardOn, interactive, false);
    }

    public boolean handleKeyBeforeDispatching(KeyEvent event, boolean keyguardOn, boolean interactive) {
        return handleKeyEvent(event, keyguardOn, interactive, true);
    }

    private boolean handleKeyEvent(KeyEvent event, boolean keyguardOn, boolean interactive, boolean multiKey) {
        int keyCode = event.getKeyCode();
        final boolean isVirtualKey = event.getDeviceId() == KeyCharacterMap.VIRTUAL_KEYBOARD;

        Log.d("TEST", "key - " + KeyEvent.keyCodeToString(keyCode));
        Log.d("TEST", "keyCode - " + event.getKeyCode());

        for (Category category : mButtons.keySet()) {
            Button button = mButtons.get(category).get(keyCode);
            if (button != null) {
                if (multiKey != (button instanceof MultiFunctionButton)) {
                    return false;
                }
                if (category.mDisabled
                        && (!category.mKey.equals("hw_keys")
                                || (category.mKey.equals("hw_keys") && !isVirtualKey))) {
                    return true;
                }
                return button.handleKeyEvent(event, keyguardOn, interactive);
            }
        }
        return false;
    }

    private void preloadRecentApps() {
        mPreloadedRecentApps = true;
        ActionsManager actionsManager = ActionsManager.getInstance(mContext);
        if (actionsManager != null) {
            actionsManager.preloadRecentApps();
        }
    }

    private void cancelPreloadRecentApps() {
        if (mPreloadedRecentApps) {
            mPreloadedRecentApps = false;
            ActionsManager actionsManager = ActionsManager.getInstance(mContext);
            if (actionsManager != null) {
                actionsManager.cancelPreloadRecentApps();
            }
        }
    }

    private void finishLockTask() {
        try {
            ActivityManagerNative.getDefault().stopSystemLockTaskMode();
        } catch (Exception e) {
        }
    }

    private boolean isInLockTask() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (Exception e) {
        }
        return false;
    }

    private boolean performHapticFeedback(int effectId, boolean always) {
       if (mDisableVibration) {
            mDisableVibration = false;
            return false;
        }
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            return false;
        }
        final boolean hapticsDisabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0, UserHandle.USER_CURRENT) == 0;
        if (hapticsDisabled && !always) {
            return false;
        }
        long[] pattern = null;
        switch (effectId) {
            case HapticFeedbackConstants.LONG_PRESS:
                pattern = mLongPressVibePattern;
                break;
            case HapticFeedbackConstants.VIRTUAL_KEY:
                pattern = mVirtualKeyVibePattern;
                break;
            default:
                return false;
        }
        int owningUid = android.os.Process.myUid();
        String owningPackage = mContext.getOpPackageName();
        if (pattern.length == 1) {
            // One-shot vibration
            mVibrator.vibrate(owningUid, owningPackage,
                    VibrationEffect.createOneShot(pattern[0],
                    VibrationEffect.DEFAULT_AMPLITUDE), VIBRATION_ATTRIBUTES);
        } else {
            // Pattern vibration
            mVibrator.vibrate(owningUid, owningPackage,
                    VibrationEffect.createWaveform(pattern, -1),
                    VIBRATION_ATTRIBUTES);
        }
        return true;
    }

    private String getStringFromSettings(String key, String def) {
        String val = AsylumSettings.System.getStringForUser(
                mContext.getContentResolver(), key, UserHandle.USER_CURRENT);
        return (val == null) ? def : val;
    }

    private interface Button {
        void observe(ContentObserver observer, ContentResolver resolver);
        void updateAssignments();
        boolean handleKeyEvent(KeyEvent event, boolean keyguardOn, boolean interactive);

    }

    private class SingleFunctionButton implements Button {
        private int mKeyCode;
        private String mAction;
        private String mDefaultAction;
        private String mSettingsKey;

        SingleFunctionButton(int keyCode, String defaultAction) {
            mKeyCode = keyCode;
            mDefaultAction = defaultAction;
            mSettingsKey = KeyParser.getPreferenceKey(keyCode);
        }

        @Override
        public void observe(ContentObserver observer, ContentResolver resolver) {
            resolver.registerContentObserver(AsylumSettings.System.getUriFor(
                    mSettingsKey), false, observer, UserHandle.USER_ALL);
        }

        @Override
        public void updateAssignments() {
            mAction = getStringFromSettings(mSettingsKey, mDefaultAction);
        }

        @Override
        public boolean handleKeyEvent(KeyEvent event, boolean keyguardOn, boolean interactive) {
            android.util.Log.d("TEST", "action - " + mAction);
            if (isDisabledByPhoneState()) {
                return false;
            }
            if (interactive || event.getAction() != KeyEvent.ACTION_UP) {
                return false;
            }
            Action.processAction(mContext, mAction, false);
            return true;
        }
    }

    private boolean isDisabledByPhoneState() {
        //if (mTelecomManager != null) {
        //    return mTelecomManager.isInCall() || mTelecomManager.isRinging();
        //}
        return false;
    }

    private class MultiFunctionButton implements Button {

        private String mKey;
        private int mKeyCode;

        private boolean mButtonPressed;
        private boolean mButtonConsumed;
        private boolean mDoubleTapPending;

        private String mTapAction;
        private String mDoubleTapAction;
        private String mLongPressAction;

        private final Runnable mDoubleTapTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (mDoubleTapPending) {
                    mDoubleTapPending = false;
                    if (!mTapAction.equals(ActionConstants.ACTION_RECENTS)) {
                        cancelPreloadRecentApps();
                    }
                    //mDisableVibration = maybeDisableVibration(mPressOnHomeBehavior);
                    Action.processAction(mContext, mTapAction, false);
                }
            }
        };

        private MultiFunctionButton(int keyCode) {
            mKeyCode = keyCode;
            String key = KeyEvent.keyCodeToString(keyCode);
            mKey = key.replace("KEYCODE_", "key_").toLowerCase();
        }

        @Override
        public void observe(ContentObserver observer, ContentResolver resolver) {
            resolver.registerContentObserver(AsylumSettings.System.getUriFor(
                    (mKey + "_action")), false, observer,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(AsylumSettings.System.getUriFor(
                    (mKey + "_long_press_action")), false, observer,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(AsylumSettings.System.getUriFor(
                    (mKey + "_double_tap_action")), false, observer,
                    UserHandle.USER_ALL);
        }

        @Override
        public void updateAssignments() {
            mTapAction = getStringFromSettings((mKey + "_action"),
                    HwKeyHelper.getDefaultTapActionForKeyCode(mContext, mKeyCode));
            mDoubleTapAction = getStringFromSettings((mKey + "_double_tap_action"),
                    HwKeyHelper.getDefaultDoubleTapActionForKeyCode(mContext, mKeyCode));
            mLongPressAction = getStringFromSettings((mKey + "_long_press_action"),
                    HwKeyHelper.getDefaultLongPressActionForKeyCode(mContext, mKeyCode));
        }

        @Override
        public boolean handleKeyEvent(KeyEvent event, boolean keyguardOn, boolean interactive) {
            final boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
            final boolean canceled = event.isCanceled();
            final int flags = event.getFlags();
            final boolean longpress = (flags & KeyEvent.FLAG_LONG_PRESS) != 0;
            final int repeatCount = event.getRepeatCount();

            // If we have released the assistant key, and didn't do anything else
            // while it was pressed, then it is time to process the assistant action!
            if (!down && mButtonPressed) {
                mButtonPressed = false;
                if (mButtonConsumed) {
                    mButtonConsumed = false;
                    return true;
                }

                if (canceled) {
                    Log.i(TAG, "Ignoring " + mKey + ", event canceled.");
                    return true;
                }

                // Delay handling assistant if a double-tap is possible.
                if (!mDoubleTapAction.equals(ActionConstants.ACTION_NULL)) {
                    mHandler.removeCallbacks(mDoubleTapTimeoutRunnable); // just in case
                    mDisableVibration = false; // just in case
                    mDoubleTapPending = true;
                    mHandler.postDelayed(mDoubleTapTimeoutRunnable,
                            ViewConfiguration.getDoubleTapTimeout());
                    return true;
                }

                if (mKeyCode == KeyEvent.KEYCODE_HOME &&
                        mDreamManagerInternal != null && mDreamManagerInternal.isDreaming()) {
                    mDreamManagerInternal.stopDream(false);
                    return true;
                }

                if (!mTapAction.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                //mDisableVibration = maybeDisableVibration(mPressOnCameraBehavior);
                Action.processAction(mContext, mTapAction, false);
                return true;
            }

            // Remember that camera key is pressed and handle special actions.
            if (down) {
                Log.i(TAG, "if(down) : Entering");
                if (!mPreloadedRecentApps &&
                        (mLongPressAction.equals(ActionConstants.ACTION_RECENTS)
                         || mDoubleTapAction.equals(ActionConstants.ACTION_RECENTS)
                         || mTapAction.equals(ActionConstants.ACTION_RECENTS))) {
                    preloadRecentApps();
                }
                if (repeatCount == 0) {
                    Log.i(TAG, "if(repeatCount == 0) : Entering");
                    mButtonPressed = true;
                    if (mDoubleTapPending) {
                        mDoubleTapPending = false;
                        mDisableVibration = false;
                        mButtonConsumed = true;
                        mHandler.removeCallbacks(mDoubleTapTimeoutRunnable);
                        if (!mDoubleTapAction.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        Action.processAction(mContext, mDoubleTapAction, false);
                    }
                    Log.i(TAG, "if(repeatCount == 0) : Leaving");
                } else if (longpress) {
                    Log.i(TAG, "if(longpress) : Entering");
                    if (!keyguardOn
                            && !mLongPressAction.equals(ActionConstants.ACTION_NULL)) {
                        if (!mLongPressAction.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        Log.i(TAG, "if(longpress) : Executing long action");
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, false);
                        Action.processAction(mContext, mLongPressAction, false);
                        mButtonConsumed = true;
                    }
                    Log.i(TAG, "if(longpress) : Leaving");
                }
                Log.i(TAG, "if(down) : Leaving");
            }
            return true;
        }
    }

    private class Category {
        private String mKey;
        private boolean mDisabled;

        Category(String key) {
            mKey = key;
        }

        public void observe(ContentObserver observer, ContentResolver resolver) {
            resolver.registerContentObserver(AsylumSettings.System.getUriFor(
                    (mKey + "_disabled")), false, observer,
                    UserHandle.USER_ALL);
        }

        public void updateAssignments() {
            mDisabled = AsylumSettings.System.getIntForUser(mContext.getContentResolver(),
                    (mKey + "_disabled"), 0, UserHandle.USER_CURRENT) == 1;
        }
    }
}
