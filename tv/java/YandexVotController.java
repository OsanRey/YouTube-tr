package com.liskovsoft.smartyoutubetv2.common.vot;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.BasePlayerController;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.utils.SimpleEditDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** A second audio player following SmartTube's actual playback clock, on the main thread. */
public final class YandexVotController extends BasePlayerController {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newFixedThreadPool(2);
    private volatile int generation;
    private volatile MediaItemFormatInfo source;
    private Future<?> request;
    private MediaPlayer voice;
    private boolean prepared, seeking, seekAgain, wanted;
    private boolean ducked;
    private float savedVolume, appliedSpeed = -1;
    private long lastSeek;
    private String status = "Выключен";
    private String requestedVideoId;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (voice == null || !prepared || getPlayer() == null) return;
            sync(false);
            if (voice != null && prepared) main.postDelayed(this, 250);
        }
    };

    @Override public void onInit() { Settings.init(getContext()); }

    public void setSource(MediaItemFormatInfo info) { source = info; }

    @Override public void onNewVideo(Video video) {
        Settings.init(getContext());
        stop();
        source = null;
        wanted = Settings.AUTO.get();
        requestedVideoId = null;
    }

    @Override public void onVideoLoaded(Video video) {
        if (wanted && video != null && video.videoId != null && !video.videoId.equals(requestedVideoId)) start();
    }

    @Override public void onEngineReleased() { stop(); requestedVideoId = null; }
    @Override public void onViewDestroyed() { wanted = false; stop(); }
    @Override public void onFinish() { wanted = false; stop(); }
    @Override public void onPlayEnd() { pauseVoice(); restoreVolume(); }
    @Override public void onPlay() { sync(true); logSync("play"); }
    @Override public void onPause() { pauseVoice(); logSync("pause"); }
    @Override public void onSeekEnd() { sync(true); logSync("seek"); }
    @Override public void onSpeedChanged(float speed) { sync(true); }

    private void pauseVoice() {
        if (voice != null && prepared) {
            try { if (voice.isPlaying()) voice.pause(); }
            catch (IllegalStateException ignored) { }
        }
    }

    private void logSync(String event) {
        if (voice == null || !prepared || getPlayer() == null) return;
        try {
            Log.i("YouTubeTrTV", "Audio " + event + ": videoMs=" + getPlayer().getPositionMs()
                    + ", voiceMs=" + voice.getCurrentPosition() + ", voicePlaying=" + voice.isPlaying());
        } catch (IllegalStateException ignored) { }
    }

    private boolean current(int id) { return id == generation && wanted; }

    private void message(String value) {
        status = value;
        MessageHelpers.showMessage(getContext(), "Перевод: " + value);
    }

    private void start() {
        stop();
        wanted = true;
        final Video video = getVideo();
        if (video == null || video.videoId == null || getPlayer() == null) {
            message("сначала откройте видео"); return;
        }
        if (video.isLive) { wanted = false; message("доступен для записанных видео"); return; }
        requestedVideoId = video.videoId;
        final int id = generation;
        // Match the canonical YouTube URL used by the upstream VOT client.
        final String url = "https://youtu.be/" + video.videoId;
        final String language = Settings.sourceLanguage();
        final double duration = Math.max(1, getPlayer().getDurationMs() / 1000.0);
        final MediaItemFormatInfo audioSource = source;
        Log.i("YouTubeTrTV", "Translation requested: seconds=" + Math.round(duration) + ", language=" + language);
        message("запрашиваю русский перевод…");
        request = worker.submit(() -> {
            final boolean[] uploaded = {false};
            final String[] error = {null};
            final boolean[] recovered = {false};
            VotApiClient.TranslationResult result = VotApiClient.pollUntilReady(
                    url, duration, language, "ru", video.getTitle(), 0,
                    new VotApiClient.PollHandler() {
                public boolean isCancelled() { return !current(id) || Thread.currentThread().isInterrupted(); }
                public void onAudioReady(VotApiClient.TranslationResult result) {
                    main.post(() -> { if (current(id)) prepare(id, result.audioUrl()); });
                }
                public void onAudioRequested(String videoUrl, String translationId) {
                    if (uploaded[0] || isCancelled()) return;
                    uploaded[0] = true;
                    main.post(() -> { if (current(id)) message("передаю исходную аудиодорожку…"); });
                    try {
                        SourceAudioUploader.upload(audioSource, videoUrl, translationId, this::isCancelled);
                    } catch (Exception e) {
                        error[0] = "не удалось передать исходную дорожку. Попробуйте другое видео";
                        main.post(() -> { if (current(id)) { wanted = false; stop(); message(error[0]); } });
                    }
                }
                public boolean onFailed(VotApiClient.TranslationResult result) {
                    Log.i("YouTubeTrTV", "Translation service status=" + result.status());
                    if (!recovered[0]) {
                        recovered[0] = true;
                        uploaded[0] = false;
                        final boolean live = !Settings.VOT_USE_LIVE_VOICES.get();
                        Settings.VOT_USE_LIVE_VOICES.save(live);
                        main.post(() -> { if (current(id)) message(live ? "пробую живые голоса…" : "пробую обычные голоса…"); });
                        return true;
                    }
                    error[0] = result.message() == null ? "сервис не смог перевести это видео" : result.message();
                    return false;
                }
                public void onSessionRequired() { error[0] = "для нового перевода войдите в Яндекс: Перевод Яндекса → Аккаунт Яндекса и живые голоса"; }
                public void onWaiting(int seconds, boolean first) {
                    main.post(() -> {
                        if (current(id)) {
                            status = "Готовится, оценка сервиса: " + seconds + " с";
                            if (first) message(status);
                        }
                    });
                }
            });
            if (result == null) main.post(() -> {
                if (current(id)) {
                    wanted = false;
                    stop();
                    message(error[0] != null ? error[0] : "не удалось получить перевод. Проверьте сеть и повторите");
                }
            });
        });
    }

    private void prepare(int id, String url) {
        if (!current(id)) return;
        try {
            voice = new MediaPlayer();
            final MediaPlayer player = voice;
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            player.setVolume(1, 1);
            player.setDataSource(getContext(), android.net.Uri.parse(url));
            player.setOnPreparedListener(p -> {
                if (!current(id) || p != voice || getPlayer() == null) return;
                prepared = true;
                savedVolume = getPlayer().getVolume();
                ducked = true;
                applyOriginalVolume();
                sync(true);
                main.removeCallbacks(tick);
                main.postDelayed(tick, 250);
                message("включён, русский язык");
                Log.i("YouTubeTrTV", "Translation prepared; sync active");
            });
            player.setOnSeekCompleteListener(p -> {
                if (p != voice || !current(id)) return;
                seeking = false;
                boolean force = seekAgain;
                seekAgain = false;
                sync(force);
            });
            player.setOnErrorListener((p, what, extra) -> {
                if (current(id) && p == voice) {
                    wanted = false; stop(); message("ошибка загрузки аудио перевода. Включите его повторно");
                }
                return true;
            });
            player.setOnCompletionListener(p -> {
                if (current(id) && p == voice) {
                    pauseVoice();
                    // Keep the prepared file seekable when the viewer rewinds from its end.
                    status = "Дорожка перевода закончилась";
                    restoreVolume();
                }
            });
            player.prepareAsync();
            status = "Загружаю аудио…";
        } catch (Exception e) {
            wanted = false; stop(); message("не удалось открыть аудио перевода");
        }
    }

    private void applyOriginalVolume() {
        if (ducked && getPlayer() != null) getPlayer().setVolume(savedVolume * Settings.originalVolume());
    }

    private void restoreVolume() {
        if (ducked && getPlayer() != null) getPlayer().setVolume(savedVolume);
        ducked = false;
    }

    private void sync(boolean force) {
        if (voice == null || !prepared || getPlayer() == null) return;
        try {
            long position = Math.max(0, getPlayer().getPositionMs());
            boolean playing = getPlayer().isPlaying();
            float speed = Math.max(0.25f, Math.min(3f, getPlayer().getSpeed()));
            if (Build.VERSION.SDK_INT >= 23 && speed != appliedSpeed) {
                voice.setPlaybackParams(new PlaybackParams().setSpeed(speed).setPitch(1));
                appliedSpeed = speed;
                if (!playing) pauseVoice();
            }
            if (position >= voice.getDuration()) { pauseVoice(); restoreVolume(); return; }
            if (!ducked) { savedVolume = getPlayer().getVolume(); ducked = true; applyOriginalVolume(); }
            long now = SystemClock.elapsedRealtime();
            if (seeking && force) seekAgain = true;
            if (!seeking && (force || (Math.abs(voice.getCurrentPosition() - position) > 450 && now - lastSeek > 1000))) {
                pauseVoice();
                seeking = true;
                lastSeek = now;
                if (Build.VERSION.SDK_INT >= 26) voice.seekTo(position, MediaPlayer.SEEK_CLOSEST);
                else voice.seekTo((int) Math.min(Integer.MAX_VALUE, position));
            }
            if (!seeking && playing && !voice.isPlaying()) voice.start();
            else if (!playing) pauseVoice();
        } catch (IllegalStateException | IllegalArgumentException e) {
            wanted = false; stop(); message("ошибка синхронизации аудио");
        }
    }

    private void stop() {
        generation++;
        if (request != null) { request.cancel(true); request = null; }
        main.removeCallbacks(tick);
        prepared = false;
        seeking = false;
        seekAgain = false;
        appliedSpeed = -1;
        if (voice != null) { voice.release(); voice = null; }
        restoreVolume();
        status = "Выключен";
    }

    public void showSettings() {
        Settings.init(getContext());
        AppDialogPresenter dialog = getAppDialogPresenter();
        dialog.appendSingleButton(UiOptionItem.from(wanted ? "Выключить перевод" : "Перевести на русский", item -> {
            if (wanted) { wanted = false; stop(); message("выключен"); }
            else start();
            dialog.closeDialog();
        }));
        dialog.appendSingleButton(UiOptionItem.from("Состояние: " + status, item -> message(status)));
        dialog.appendCheckedCategory("Для следующих видео", Arrays.asList(
                UiOptionItem.from("Переводить автоматически", item -> Settings.AUTO.save(item.isSelected()), Settings.AUTO.get())));
        List<OptionItem> volume = new ArrayList<>();
        for (int percent : new int[] {0, 10, 20, 35, 50, 75, 100}) {
            volume.add(UiOptionItem.from(percent + "%", item -> {
                Settings.originalVolume(percent); applyOriginalVolume();
            }, Math.round(Settings.originalVolume() * 100) == percent));
        }
        dialog.appendRadioCategory("Громкость оригинала", volume);
        List<OptionItem> languages = new ArrayList<>();
        String[][] choices = {{"auto", "Определять автоматически"}, {"en", "Английский"}, {"de", "Немецкий"}, {"es", "Испанский"}, {"fr", "Французский"}, {"zh", "Китайский"}, {"ja", "Японский"}, {"ko", "Корейский"}};
        for (String[] choice : choices) languages.add(UiOptionItem.from(choice[1], item -> {
            Settings.sourceLanguage(choice[0]); if (wanted) start();
        }, Settings.sourceLanguage().equals(choice[0])));
        dialog.appendRadioCategory("Язык видео → русский", languages);
        dialog.appendSingleButton(UiOptionItem.from("Аккаунт Яндекса и живые голоса", item -> showAccount()));
        dialog.appendSingleButton(UiOptionItem.from("Авторы и исходный код", item -> showCredits()));
        dialog.showDialog("Перевод Яндекса");
    }

    private void showAccount() {
        AppDialogPresenter dialog = getAppDialogPresenter();
        dialog.appendSingleButton(UiOptionItem.from("Войти в Яндекс", item -> {
            dialog.closeDialog();
            VotAccountDialog.show(getActivity(), () -> message("аккаунт Яндекса подключён"));
        }));
        dialog.appendSingleButton(UiOptionItem.from("Указать OAuth-токен", item -> {
            dialog.closeDialog();
            SimpleEditDialog.showPassword(getContext(), "OAuth Яндекса", "", value -> {
                Settings.VOT_OAUTH_TOKEN.save(value.trim());
                Settings.VOT_OAUTH_TOKEN_EXPIRES_AT.save(0L);
                VotApiClient.clearTokenValidationCache();
                return true;
            });
        }));
        dialog.appendCheckedCategory("Озвучка", Arrays.asList(UiOptionItem.from("Живые голоса (нужен аккаунт)", item -> {
            Settings.VOT_USE_LIVE_VOICES.save(item.isSelected());
            if (wanted) start();
        }, Settings.VOT_USE_LIVE_VOICES.get())));
        dialog.appendSingleButton(UiOptionItem.from("Выйти из Яндекса", item -> {
            Settings.VOT_OAUTH_TOKEN.save(""); Settings.VOT_USE_LIVE_VOICES.save(false);
            VotApiClient.clearTokenValidationCache();
            VotAccountDialog.clear();
            message("аккаунт Яндекса отключён");
        }));
        dialog.showDialog("Аккаунт Яндекса");
    }

    public void showCredits() {
        AppDialogPresenter dialog = getAppDialogPresenter();
        dialog.appendStringsCategory("YouTube+tr TV", Arrays.asList(
                UiOptionItem.from("ТВ-интерфейс: SmartTube — yuliskov и участники (MIT)"),
                UiOptionItem.from("Перевод: Яндекс; протокол VOT — ilyhalight, sodapng (MIT)"),
                UiOptionItem.from("Java-клиент VOT: anddea, Jav1x, sashade8-ship-it (GPLv3)"),
                UiOptionItem.from("Адаптация и исходники ТВ-версии: github.com/OsanRey/YouTube-tr")));
        dialog.appendSingleButton(UiOptionItem.from("Лицензии и уведомления", item -> {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                    getContext().getAssets().open("youtube_tr_notices.txt"), java.nio.charset.StandardCharsets.UTF_8))) {
                StringBuilder text = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) text.append(line).append('\n');
                dialog.appendLongTextCategory("Лицензии и уведомления", UiOptionItem.from(text.toString()));
                dialog.showDialog("Лицензии и уведомления");
            } catch (java.io.IOException e) { message("не удалось открыть лицензии"); }
        }));
        dialog.showDialog("Авторы и исходный код");
    }
}
