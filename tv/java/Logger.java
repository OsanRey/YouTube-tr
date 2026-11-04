package com.liskovsoft.smartyoutubetv2.common.vot;

import android.util.Log;
import java.util.function.Supplier;

/** Suppress API payloads and signed audio URLs, including in release logcat. */
final class Logger {
    static void printDebug(Supplier<String> message) { }
    static void printInfo(Supplier<String> message) { }
    static void printInfo(Supplier<String> message, Throwable error) {
        Log.w("YouTubeTrTV", "Translation network operation: " + error.getClass().getSimpleName());
    }
    static void printException(Supplier<String> message, Throwable error) {
        Log.w("YouTubeTrTV", "Translation operation: " + error.getClass().getSimpleName());
    }
}
