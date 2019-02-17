/*
 * Copyright (C) 2016-2017 The SlimRoms Project
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

package com.asylum.service;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.policy.WindowManagerPolicy;

import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.power.ShutdownThread;

import com.asylum.action.IActionsService;
import com.asylum.core.ServiceConstants;
import com.asylum.core.internal.statusbar.IAsylumStatusBar;

public class ActionsService extends SystemService {
    private static final String TAG = "ActionsService";

    private final Context mContext;
    private Handler mHandler = new Handler();

    private WindowManagerPolicy mPolicy;

    private final IBinder mService = new IActionsService.Stub() {
        private IAsylumStatusBar mBar;

        @Override
        public void registerAsylumStatusBar(IAsylumStatusBar bar) {
            enforceActionsService();
            Slog.i(TAG, "registerAsylumStatusBar bar=" + bar);
            mBar = bar;
        }

        @Override
        public void advancedReboot(String reason) {
            enforceActionsService();
            long identity = Binder.clearCallingIdentity();
            try {
                mHandler.post(() -> {
                    //ShutdownThread.rebootCustom(getUiContext(), reason, false, true);
                });
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /**
         * Ask keyguard to invoke a custom intent after dismissing keyguard
         * @hide
         */
        @Override
        public void showCustomIntentAfterKeyguard(Intent intent) {
            enforceActionsService();
            if (mBar != null) {
                try {
                    mBar.showCustomIntentAfterKeyguard(intent);
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void toggleSplitScreen() {
            if (mBar != null) {
                try {
                    mBar.toggleSplitScreen();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void toggleScreenshot() {
            if (mBar != null) {
                try {
                    mBar.toggleScreenshot();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void toggleLastApp() {
            if (mBar != null) {
                try {
                    mBar.toggleLastApp();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void toggleKillApp() {
            if (mBar != null) {
                try {
                    mBar.toggleKillApp();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void toggleRecentApps() {
            if (mBar != null) {
                try {
                    mBar.toggleRecentApps();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void preloadRecentApps() {
            if (mBar != null) {
                try {
                    mBar.preloadRecentApps();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void cancelPreloadRecentApps() {
            if (mBar != null) {
                try {
                    mBar.cancelPreloadRecentApps();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void startAssist(Bundle args) {
            if (mBar != null) {
                try {
                    mBar.startAssist(args);
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void toggleGlobalMenu() {
            if (mPolicy != null) {
                mPolicy.showGlobalActions();
            }
        }
    };

    public ActionsService(Context context) {
        super(context);
        mContext = context;

        mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
    }

    private void enforceActionsService() {
        // TODO
    }

    @Override
    public void onStart() {
        publishBinderService(ServiceConstants.ACTIONS_SERVICE, mService);
    }
}
