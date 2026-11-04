package com.liskovsoft.smartyoutubetv2.common.vot;

import android.content.Context;
import android.content.SharedPreferences;

/** Private, device-local preferences. No account data is included in a build. */
public final class Settings {
    private static SharedPreferences prefs;
    public static void init(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("youtube_tr_tv", Context.MODE_PRIVATE);
    }
    public static final BooleanPref ENABLED = new BooleanPref("enabled", false);
    public static final BooleanPref AUTO = new BooleanPref("auto", false);
    public static final BooleanPref VOT_USE_LIVE_VOICES = new BooleanPref("live", true);
    public static final BooleanPref VOT_AUDIO_PROXY_ENABLED = new BooleanPref("proxy", false);
    public static final StringPref VOT_OAUTH_TOKEN = new StringPref("oauth", "");
    public static final StringPref VOT_PROXY_URL = new StringPref("proxy_url", "vot.toil.cc");
    public static final LongPref VOT_OAUTH_TOKEN_EXPIRES_AT = new LongPref("oauth_expiry", 0);
    public static float originalVolume() { return prefs.getInt("original_volume", 20) / 100f; }
    public static void originalVolume(int value) { prefs.edit().putInt("original_volume", value).apply(); }
    public static String sourceLanguage() { return prefs.getString("source", "auto"); }
    public static void sourceLanguage(String value) { prefs.edit().putString("source", value).apply(); }
    public static final class BooleanPref {
        final String key; final boolean defaultValue;
        BooleanPref(String key, boolean value) { this.key = key; defaultValue = value; }
        public boolean get() { return prefs.getBoolean(key, defaultValue); }
        public void save(boolean value) { prefs.edit().putBoolean(key, value).apply(); }
    }
    public static final class StringPref {
        final String key; public final String defaultValue;
        StringPref(String key, String value) { this.key = key; defaultValue = value; }
        public String get() { return prefs.getString(key, defaultValue); }
        public void save(String value) { prefs.edit().putString(key, value).apply(); }
    }
    public static final class LongPref {
        final String key; final long defaultValue;
        LongPref(String key, long value) { this.key = key; defaultValue = value; }
        public long get() { return prefs.getLong(key, defaultValue); }
        public void save(long value) { prefs.edit().putLong(key, value).apply(); }
    }
}
