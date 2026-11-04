package com.liskovsoft.smartyoutubetv2.common.vot;

import java.util.Arrays;

/** Fixed protobuf wire vectors; runs without Android or network access. */
public final class VotProtocolTest {
    private static byte[] hex(String value) {
        value = value.replace(" ", "");
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        return bytes;
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(Arrays.equals(VotProtobuf.encodeSessionRequest("u", "v"), hex("0a0175 120176")), "session request");
        VotProtobuf.SessionResponse session = VotProtobuf.decodeSessionResponse(hex("0a036b6579 10ac02"));
        check("key".equals(session.secretKey) && session.expires == 300, "session lifetime");
        VotProtobuf.TranslationResponse ready = VotProtobuf.decodeTranslationResponse(
                hex("0a0161 110000000000004540 2001 2800 3a0169 4202656e 4a026f6b"));
        check(ready.status == 1 && ready.duration == 42 && "a".equals(ready.url), "ready audio");
        check("i".equals(ready.translationId) && "en".equals(ready.language) && "ok".equals(ready.message), "metadata");
        VotProtobuf.TranslationResponse waiting = VotProtobuf.decodeTranslationResponse(hex("2002 289601"));
        check(waiting.status == 2 && waiting.remainingTime == 150, "waiting time varint");
        VotProtobuf.TranslationResponse unknown = VotProtobuf.decodeTranslationResponse(
                hex("a00601 a9060000000000000000 b206026869 bd0600000000 2006"));
        check(unknown.status == 6, "unknown fields must not hide audio-upload status");
        boolean rejected = false;
        try { VotProtobuf.decodeTranslationResponse(hex("2080")); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "truncated varint must fail");
        System.out.println("VOT protocol: 6 checks passed");
    }
}
