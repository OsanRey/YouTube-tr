package com.liskovsoft.smartyoutubetv2.common.vot;

import android.app.Activity;
import android.webkit.CookieManager;
import android.webkit.WebStorage;

/** Uses the same sign-in flow as the credited upstream Android VOT port. */
final class VotAccountDialog {
    static void show(Activity activity, Runnable onSuccess) {
        if (activity == null || activity.isFinishing()) return;
        new VotAuthWebViewDialog(activity, new VotAuthWebViewDialog.OnTokenReceivedListener() {
            @Override public void onTokenReceived(String token, long expiresIn) {
                Settings.VOT_OAUTH_TOKEN.save(token);
                Settings.VOT_OAUTH_TOKEN_EXPIRES_AT.save(expiresIn > 0
                        ? System.currentTimeMillis() + Math.max(0, expiresIn - 60) * 1000L : 0);
                Settings.VOT_USE_LIVE_VOICES.save(true);
                VotApiClient.clearTokenValidationCache();
                onSuccess.run();
            }
            @Override public void onCancelled() { }
        }).show();
    }
    static void clear() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        WebStorage.getInstance().deleteAllData();
    }
}
