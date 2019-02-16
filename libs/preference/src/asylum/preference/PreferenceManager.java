/*
* Copyright (C) 2016-2017 SlimRoms Project
* Copyright (C) 2013-14 The Android Open Source Project
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

package asylum.preference;

import android.content.Context;
import android.os.UserHandle;
import android.util.Log;

import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;

import asylum.provider.AsylumSettings;
import com.asylum.utils.AttributeHelper;
import com.asylum.preference.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class PreferenceManager {
    public static final int ASYLUM_SYSTEM_SETTING = 0;
    public static final int ASYLUM_GLOBAL_SETTING = 1;
    public static final int ASYLUM_SECURE_SETTING = 2;

    private static PreferenceManager INSTANCE;

    private HashMap<String, ArrayList<Dependent>> mDependents = new HashMap<>();

    public class Dependent {
        Preference dependent;
        String dependencyKey;
        String[] values;
    }

    private PreferenceManager() {
    }

    public static PreferenceManager get() {
        if (INSTANCE == null) {
            INSTANCE = new PreferenceManager();
        }
        return INSTANCE;
    }

    public void registerListDependent(Preference dep, String key, String[] values) {
        Dependent dependent = new Dependent();
        dependent.dependent = dep;
        dependent.dependencyKey = key;
        dependent.values = values;
        ArrayList<Dependent> deps = mDependents.get(key);
        if (deps == null) deps = new ArrayList<>();
        deps.add(dependent);
        mDependents.put(key, deps);
        dep.setEnabled(!Arrays.asList(values).contains(((ListPreference) dep.getPreferenceManager()
                .getPreferenceScreen().findPreference(key)).getValue()));
    }

    public void unregisterListDependent(Preference dep, String key) {
        ArrayList<Dependent> deps = mDependents.get(key);
        if (deps == null) return;
        for (Dependent dependent : deps) {
            if (dependent.dependent.getKey().equals(dep.getKey())) {
                deps.remove(dependent);
            }
        }
    }

    public void updateDependents(ListPreference dep) {
        ArrayList<Dependent> deps = mDependents.get(dep.getKey());
        if (deps == null) return;
        for (Dependent dependent : deps) {
            if (Arrays.asList(dependent.values).contains(dep.getValue())) {
                dependent.dependent.setEnabled(false);
            } else {
                dependent.dependent.setEnabled(true);
            }
        }
    }

    public static int getSettingType(AttributeHelper a) {
        int s = a.getInt(R.styleable.SettingPreference_settingType,
                ASYLUM_SYSTEM_SETTING);
        switch (s) {
            case ASYLUM_GLOBAL_SETTING:
                return ASYLUM_GLOBAL_SETTING;
            case ASYLUM_SECURE_SETTING:
                return ASYLUM_SECURE_SETTING;
            default:
                return ASYLUM_SYSTEM_SETTING;
        }
    }


    public static int getIntFromSettings(
            Context context, int settingType, String key, int def) {
        switch (settingType) {
            case ASYLUM_GLOBAL_SETTING:
                return AsylumSettings.Global.getInt(context.getContentResolver(), key, def);
            case ASYLUM_SECURE_SETTING:
                return AsylumSettings.Secure.getIntForUser(context.getContentResolver(), key,
                        def, UserHandle.USER_CURRENT);
            default:
                return AsylumSettings.System.getIntForUser(context.getContentResolver(), key,
                        def, UserHandle.USER_CURRENT);
        }
    }

    public static void putIntInSettings(Context context, int settingType, String key, int val) {
        switch (settingType) {
            case ASYLUM_GLOBAL_SETTING:
                AsylumSettings.Global.putInt(context.getContentResolver(), key, val);
                break;
            case ASYLUM_SECURE_SETTING:
                AsylumSettings.Secure.putIntForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
            default:
                AsylumSettings.System.putIntForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
        }
    }

    public static String getStringFromSettings(Context context,
            int settingType, String key, String def) {
        if (!settingExists(context, settingType, key)) return def;
        switch (settingType) {
            case ASYLUM_GLOBAL_SETTING:
                return AsylumSettings.Global.getString(context.getContentResolver(), key);
            case ASYLUM_SECURE_SETTING:
                return AsylumSettings.Secure.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT);
            default:
                return AsylumSettings.System.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT);
        }
    }

    public static void putStringInSettings(
            Context context, int settingType, String key, String val) {
        switch (settingType) {
            case ASYLUM_GLOBAL_SETTING:
                AsylumSettings.Global.putString(context.getContentResolver(), key, val);
                break;
            case ASYLUM_SECURE_SETTING:
                AsylumSettings.Secure.putStringForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
            default:
                AsylumSettings.System.putStringForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
        }
    }

    public static boolean settingExists(Context context, int settingType, String key) {
        switch (settingType) {
            case ASYLUM_GLOBAL_SETTING:
                return AsylumSettings.Global.getString(context.getContentResolver(), key) != null;
            case ASYLUM_SECURE_SETTING:
                return AsylumSettings.Secure.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT) != null;
            default:
                return AsylumSettings.System.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT) != null;
        }
    }
}
