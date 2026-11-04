package com.liskovsoft.smartyoutubetv2.common.vot;

import com.liskovsoft.mediaserviceinterfaces.data.MediaFormat;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;

/** Sends the original compressed audio, only when Yandex explicitly requests it. */
final class SourceAudioUploader {
    interface Cancelled { boolean get(); }
    private static final int CHUNK = 5_295_308;

    static void upload(MediaItemFormatInfo source, String videoUrl, String translationId,
                       Cancelled cancelled) throws IOException {
        if (source == null) throw new IOException("No audio source");
        MediaFormat audio = select(source.getAdaptiveFormats());
        if (audio == null) throw new IOException("No direct audio format");
        long length;
        try { length = Long.parseLong(audio.getClen()); }
        catch (Exception e) { throw new IOException("Unknown audio length"); }
        if (length <= 0 || length > 2_000_000_000L) throw new IOException("Invalid audio length");
        int parts = (int) ((length + CHUNK - 1) / CHUNK);
        String fileId = "random-web_mse_proxy-" + UUID.randomUUID();
        String userAgent = source.getClientInfo() == null ? "Mozilla/5.0" : source.getClientInfo().getUserAgent();
        for (int i = 0; i < parts; i++) {
            if (cancelled.get()) throw new IOException("Cancelled");
            long start = (long) i * CHUNK;
            long end = Math.min(length - 1, start + CHUNK - 1);
            byte[] bytes = download(audio.getUrl(), userAgent, start, end, length, cancelled);
            if (cancelled.get()) throw new IOException("Cancelled");
            if (!VotApiClient.sendPartialAudio(videoUrl, translationId, fileId,
                    i == parts - 1 ? parts : 0, 1, i, bytes)) throw new IOException("Audio upload failed");
        }
    }

    private static MediaFormat select(List<MediaFormat> formats) {
        MediaFormat result = null;
        if (formats == null) return null;
        for (MediaFormat format : formats) {
            if (format.getMimeType() == null || !format.getMimeType().startsWith("audio/")
                    || format.getUrl() == null || !format.getUrl().startsWith("https://")
                    || format.getFormatType() == MediaFormat.FORMAT_TYPE_SABR || format.isOtf()) continue;
            if (result == null || bitrate(format) < bitrate(result)) result = format;
        }
        return result;
    }

    private static long bitrate(MediaFormat format) {
        try { return Long.parseLong(format.getBitrate()); }
        catch (Exception ignored) { return Long.MAX_VALUE; }
    }

    private static byte[] download(String url, String agent, long start, long end, long total,
                                   Cancelled cancelled) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", agent);
        conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
        try {
            int code = conn.getResponseCode();
            String range = conn.getHeaderField("Content-Range");
            if (code != 206 || !String.format("bytes %d-%d/%d", start, end, total).equals(range))
                throw new IOException("Audio range mismatch: HTTP " + code);
            int expected = (int) (end - start + 1);
            ByteArrayOutputStream output = new ByteArrayOutputStream(expected);
            try (InputStream input = conn.getInputStream()) {
                byte[] buffer = new byte[32768];
                while (output.size() < expected) {
                    if (cancelled.get()) throw new IOException("Cancelled");
                    int count = input.read(buffer, 0, Math.min(buffer.length, expected - output.size()));
                    if (count < 0) throw new IOException("Truncated audio range");
                    output.write(buffer, 0, count);
                }
                if (input.read() != -1) throw new IOException("Oversized audio range");
            }
            return output.toByteArray();
        } finally { conn.disconnect(); }
    }
}
