package com.asylum.keys.parser;

import static com.android.internal.R.styleable.Key;
import static com.android.internal.R.styleable.Key_allowDisable;
import static com.android.internal.R.styleable.Key_icon;
import static com.android.internal.R.styleable.Key_key;
import static com.android.internal.R.styleable.Key_name;
import static com.android.internal.R.styleable.Key_order;
import static com.android.internal.R.styleable.Key_path;
import static com.android.internal.R.styleable.Key_keyCode;
import static com.android.internal.R.styleable.Key_defaultAction;
import static com.android.internal.R.styleable.Key_supportsMultipleActions;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.util.Xml;
import android.view.KeyEvent;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.LinkedHashMap;


public class KeyParser {

    private static KeysArray sKeys;

    public static LinkedHashMap<String, KeyCategory> sKeyCategories;

    public static KeyCategory getCategory(Context context, String key) {
        if (sKeyCategories == null) {
            parseKeys(context);
        }
        return sKeyCategories.get(key);
    }

    public static boolean containsCategory(Context context, String key) {
        if (sKeyCategories == null) {
            parseKeys(context);
        }
        return sKeyCategories.containsKey(key);
    }

    public static LinkedHashMap<String, KeyCategory> parseKeys(Context context) {
        XmlResourceParser parser;

        if (sKeyCategories != null) {
            return sKeyCategories;
        }

        sKeyCategories = new LinkedHashMap<>();

        try {
            parser = context.getResources().getXml(
                    com.android.internal.R.xml.available_keys);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }
            String nodeName = parser.getName();
            if (!"keys".equals(nodeName)) {
                throw new RuntimeException("XML document must start with "
                        + " <touchscreen-gestures tag; found " + nodeName
                        + " at " + parser.getPositionDescription());
            }
            final int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();
                if ("key-category".equals(nodeName)) {
                    TypedArray sa = context.getResources().obtainAttributes(attrs, Key);

                    KeyCategory category = new KeyCategory();
                    TypedValue tv = sa.peekValue(Key_name);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            category.name = context.getResources().getString(tv.resourceId);
                        } else {
                            category.name = String.valueOf(tv.string);
                        }
                    }
                    if (category.name == null) {
                        throw new RuntimeException("Attribute 'name' is required");
                    }
//                    android.util.Log.d("TEST", "category - " + category.name);

                    tv = sa.peekValue(Key_key);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            category.key = context.getResources().getString(tv.resourceId);
                        } else {
                            category.key = String.valueOf(tv.string);
                        }
                    }
                    if (category.key == null) {
                        throw new RuntimeException("Attribute 'key' is required");
                    }

                    tv = sa.peekValue(Key_icon);
                    if (tv != null) {
                        if (tv.resourceId != 0) {
                            category.drawableId = tv.resourceId;
                        }
                    }

                    tv = sa.peekValue(Key_order);
                    if (tv != null && tv.type == TypedValue.TYPE_INT_DEC) {
                        if (tv.resourceId != 0) {
                            category.order = context.getResources().getInteger(tv.resourceId);
                        } else {
                            category.order = tv.data;
                        }
                    }

                    tv = sa.peekValue(Key_allowDisable);
                    if (tv != null && tv.type == TypedValue.TYPE_INT_BOOLEAN) {
                        category.allowDisable = tv.data != 0;
                    }

                    sa.recycle();
                    category.keys = getKeys(context, parser, attrs);

                    sKeyCategories.put(category.key, category);
                    }
            }
        } catch (IOException|XmlPullParserException e) {
            e.printStackTrace();
        }

        return sKeyCategories;
    }

    private static KeysArray getKeys(Context context, XmlPullParser parser, AttributeSet attrs) throws IOException, XmlPullParserException {
        KeysArray gesturesArray = new KeysArray();
        int type;
        String nodeName;
        final int outerDepth = parser.getDepth();
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }

            nodeName = parser.getName();
            if ("key".equals(nodeName)) {
                TypedArray sa = context.getResources().obtainAttributes(attrs, Key);

                Key k = new Key();
                TypedValue tv = sa.peekValue(Key_name);
                if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                    if (tv.resourceId != 0) {
                        k.name = context.getResources().getString(tv.resourceId);
                    } else {
                        k.name = String.valueOf(tv.string);
                    }
                }
                if (k.name == null) {
                    throw new RuntimeException("Attribute 'name' is required");
                }

                tv = sa.peekValue(Key_path);
                if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                    if (tv.resourceId != 0) {
                        k.path = context.getResources().getString(tv.resourceId);
                    } else {
                        k.path = String.valueOf(tv.string);
                    }
                }

                tv = sa.peekValue(Key_defaultAction);
                if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                    if (tv.resourceId != 0) {
                        k.def = context.getResources().getString(tv.resourceId);
                    } else {
                        k.def = String.valueOf(tv.string);
                    }
                }

                tv = sa.peekValue(Key_keyCode);
                if (tv != null && tv.type == TypedValue.TYPE_INT_DEC) {
                    if (tv.resourceId != 0) {
                        k.keyCode = context.getResources().getInteger(tv.resourceId);
                    } else {
                        k.keyCode = tv.data;
                    }
                }


                tv = sa.peekValue(Key_supportsMultipleActions);
                if (tv != null && tv.type == TypedValue.TYPE_INT_BOOLEAN) {
                    k.supportsMultipleActions = tv.data != 0;
                }

                tv = sa.peekValue(Key_order);
                if (tv != null && tv.type == TypedValue.TYPE_INT_DEC) {
                    if (tv.resourceId != 0) {
                        k.order = context.getResources().getInteger(tv.resourceId);
                    } else {
                        k.order = tv.data;
                    }
                }

                sa.recycle();

                gesturesArray.put(k.keyCode, k);
            }
        }
        return gesturesArray;
    }

    public static String getPreferenceKey(int keyCode) {
        return KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "key_").toLowerCase();
    }
}
