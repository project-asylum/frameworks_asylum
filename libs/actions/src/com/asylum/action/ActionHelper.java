/*
* Copyright (C) 2016-2018 SlimRoms Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.asylum.action;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.UserHandle;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import asylum.provider.AsylumSettings;

import com.asylum.actions.R;
import com.asylum.utils.ConfigSplitHelper;
import com.asylum.utils.DeviceUtils;
import com.asylum.utils.ImageHelper;

public class ActionHelper {

    // General methods to retrieve the correct icon for the respective action.
    public static Drawable getActionIconImage(Context context,
            String clickAction, String customIcon) {
        int resId = -1;
        Drawable d = null;
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }

        Resources resources = context.getResources();

        if (!clickAction.startsWith("**")) {
            try {
                String extraIconPath = clickAction.replaceAll(".*?hasExtraIcon=", "");
                if (extraIconPath != null && !extraIconPath.isEmpty()) {
                    File f = new File(Uri.parse(extraIconPath).getPath());
                    if (f.exists()) {
                        d = new BitmapDrawable(context.getResources(),
                                f.getAbsolutePath());
                    }
                }
                if (d == null) {
                    d = pm.getActivityIcon(Intent.parseUri(clickAction, 0));
                }
            } catch (NameNotFoundException e) {
                return resources.getDrawable(R.drawable.ic_sysbar_null);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        if (customIcon != null && customIcon.startsWith(ActionConstants.SYSTEM_ICON_IDENTIFIER)) {
            resId = resources.getIdentifier(customIcon.substring(
                        ActionConstants.SYSTEM_ICON_IDENTIFIER.length()),
                                            "drawable", "");
            if (resId > 0) {
                return resources.getDrawable(resId);
            }
        } else if (customIcon != null && !customIcon.equals(ActionConstants.ICON_EMPTY)) {
            File f = new File(Uri.parse(customIcon).getPath());
            if (f.exists()) {
                return new BitmapDrawable(context.getResources(),
                    ImageHelper.getRoundedCornerBitmap(
                        new BitmapDrawable(context.getResources(),
                        f.getAbsolutePath()).getBitmap()));
            } else {
                Log.e("ActionHelper:", "can't access custom icon image");
                return null;
            }
        } else if (clickAction.startsWith("**")) {
            resId = getActionSystemIcon(resources, clickAction);

            if (resId > 0) {
                return resources.getDrawable(resId);
            }
        }
        return d;
    }

    private static int getActionSystemIcon(Resources resources, String clickAction) {
        switch (clickAction) {
            case ActionConstants.ACTION_HOME:
                return R.drawable.ic_sysbar_home;
            case ActionConstants.ACTION_BACK:
                return R.drawable.ic_sysbar_back;
            case ActionConstants.ACTION_RECENTS:
                return R.drawable.ic_sysbar_recent;
            case ActionConstants.ACTION_SEARCH:
            case ActionConstants.ACTION_ASSIST:
            case ActionConstants.ACTION_NOWONTAP:
            case ActionConstants.ACTION_VOICE_SEARCH:
            case ActionConstants.ACTION_KEYGUARD_SEARCH:
                return R.drawable.ic_sysbar_search;
            case ActionConstants.ACTION_MENU:
                return R.drawable.ic_sysbar_menu_big;
            case ActionConstants.ACTION_IME:
                return R.drawable.ic_sysbar_ime_switcher;
            case ActionConstants.ACTION_TOGGLE_SCREEN:
            case ActionConstants.ACTION_POWER:
                return R.drawable.ic_sysbar_power;
            case ActionConstants.ACTION_POWER_MENU:
                return R.drawable.ic_sysbar_power_menu;
            case ActionConstants.ACTION_VIB:
                return R.drawable.ic_sysbar_vib;
            case ActionConstants.ACTION_SILENT:
                return R.drawable.ic_sysbar_silent;
            case ActionConstants.ACTION_VIB_SILENT:
                return R.drawable.ic_sysbar_ring_vib_silent;
            case ActionConstants.ACTION_EXPANDED_DESKTOP:
                return R.drawable.ic_sysbar_expanded_desktop;
            case ActionConstants.ACTION_KILL:
                return R.drawable.ic_sysbar_killtask;
            case ActionConstants.ACTION_LAST_APP:
                return R.drawable.ic_sysbar_lastapp;
            case ActionConstants.ACTION_NOTIFICATIONS:
                return R.drawable.ic_sysbar_notifications;
            case ActionConstants.ACTION_SETTINGS_PANEL:
                return R.drawable.ic_sysbar_qs;
            case ActionConstants.ACTION_SCREENSHOT:
                return R.drawable.ic_sysbar_screenshot;
            case ActionConstants.ACTION_TORCH:
                return R.drawable.ic_sysbar_torch;
            case ActionConstants.ACTION_CAMERA:
                return R.drawable.ic_sysbar_camera;
            default:
                return R.drawable.ic_sysbar_null;
        }
    }

    public static Drawable getPowerMenuIconImage(Context context,
            String clickAction, String customIcon) {
        Drawable d = getActionIconImage(context, clickAction, customIcon);
        if (d != null) {
            d.mutate();
            d = ImageHelper.getColoredDrawable(d,
                    context.getResources().getColor(R.color.dslv_icon_dark));
        }
        return d;
    }

    public static String getActionDescription(Context context, String action) {
        Resources resources = context.getResources();
        ActionsArray actionsArray = new ActionsArray(context);

        int index = -1;
        for (int i = 0; i < actionsArray.getEntries().length; i++) {
            if (action.equals(actionsArray.getValues()[i])) {
                return actionsArray.getEntries()[i];
            }
        }
        return resources.getString(R.string.shortcut_action_none);
    }
}
