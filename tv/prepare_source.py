"""Apply the YouTube+tr TV overlay to the pinned upstream sources."""
from pathlib import Path
import re
import shutil

ROOT = Path(__file__).resolve().parent.parent
SMART = ROOT / 'sources/SmartTube'
COMMON = SMART / 'common/src/main/java/com/liskovsoft/smartyoutubetv2/common'
DEST = COMMON / 'vot'
DEST.mkdir(parents=True, exist_ok=True)
UPSTREAM = ROOT / 'sources/anddea-patches/extensions/shared/src/main/java/app/morphe/extension/youtube/patches/voiceovertranslation'
PACKAGE = 'com.liskovsoft.smartyoutubetv2.common.vot'

for name in ('VotApiClient.java', 'VotProtobuf.java', 'VotAuthWebViewDialog.java'):
    text = (UPSTREAM / name).read_text(encoding='utf-8')
    text = text.replace('package app.morphe.extension.youtube.patches.voiceovertranslation;',
                        '// Modified for YouTube+tr TV: Java 8 compatibility and TV preferences.\npackage ' + PACKAGE + ';')
    text = text.replace('import app.morphe.extension.shared.utils.Logger;', '')
    text = text.replace('import app.morphe.extension.youtube.settings.Settings;', '')
    if name == 'VotApiClient.java':
        text = text.replace('private record CachedResult(TranslationResult result, long createdAt) {', '''private static final class CachedResult {
        final TranslationResult result;
        final long createdAt;
        CachedResult(TranslationResult result, long createdAt) { this.result = result; this.createdAt = createdAt; }
        TranslationResult result() { return result; }''')
        text = re.sub(r'public record TranslationResult\(int status, String audioUrl, int remainingTime,\s*String translationId, String message\) \{\s*}', '''public static final class TranslationResult {
        private final int status, remainingTime;
        private final String audioUrl, translationId, message;
        public TranslationResult(int status, String audioUrl, int remainingTime, String translationId, String message) {
            this.status = status; this.audioUrl = audioUrl; this.remainingTime = remainingTime;
            this.translationId = translationId; this.message = message;
        }
        public int status() { return status; }
        public String audioUrl() { return audioUrl; }
        public int remainingTime() { return remainingTime; }
        public String translationId() { return translationId; }
        public String message() { return message; }
    }''', text)
        text = text.replace('return switch (status) {', 'switch (status) {')
        text = re.sub(r'(case STATUS_\w+|default) -> ("[^"]+");', r'\1: return \2;', text)
        text = text.replace('default: return "UNKNOWN";\n        };', 'default: return "UNKNOWN";\n        }')
        text = text.replace('if (handler.onFailed(result)) {\n                    waitSeconds = 3;',
                            'if (handler.onFailed(result)) {\n                    firstRequest = true; // A changed voice mode is a new translation request.\n                    waitSeconds = 3;')
    elif name == 'VotProtobuf.java':
        text = re.sub(r'case (10|18|34|42) -> ([^;]+);', r'case \1: \2; break;', text)
        start = text.index('        return switch (wireType) {')
        end = text.index('\n        };', start) + len('\n        };')
        text = text[:start] + '''        switch (wireType) {
            case WIRETYPE_VARINT: return readVarint(data, pos)[1];
            case WIRETYPE_64BIT: return pos + 8;
            case WIRETYPE_LENGTH_DELIMITED:
                int[] length = readVarint(data, pos);
                return length[1] + length[0];
            case 5: return pos + 4;
            default: return data.length;
        }''' + text[end:]
    (DEST / name).write_text(text, encoding='utf-8')

for source in (ROOT / 'tv/java').glob('*.java'):
    shutil.copyfile(source, DEST / source.name)

presenter = COMMON / 'app/presenters/PlaybackPresenter.java'
text = presenter.read_text(encoding='utf-8')
anchor = '        mEventListeners.add(new CommentsController());'
addition = '\n        mEventListeners.add(new com.liskovsoft.smartyoutubetv2.common.vot.YandexVotController());'
if addition.strip() not in text:
    assert anchor in text
    text = text.replace(anchor, anchor + addition)
presenter.write_text(text, encoding='utf-8')

hq = COMMON / 'app/models/playback/controllers/HQDialogController.java'
text = hq.read_text(encoding='utf-8')
anchor = '        addQualityCategories();'
addition = '''        mAppDialogPresenter.appendSingleButton(UiOptionItem.from("Перевод Яндекса", option ->
                getController(com.liskovsoft.smartyoutubetv2.common.vot.YandexVotController.class).showSettings()));
'''
if '"Перевод Яндекса"' not in text:
    assert anchor in text
    text = text.replace(anchor, addition + '\n' + anchor)
hq.write_text(text, encoding='utf-8')

print('Applied TV translation sources and player menu')

loader = COMMON / 'app/models/playback/controllers/VideoLoaderController.java'
text = loader.read_text(encoding='utf-8')
anchor = '        getVideo().sync(formatInfo);'
addition = '\n        getController(com.liskovsoft.smartyoutubetv2.common.vot.YandexVotController.class).setSource(formatInfo);'
if addition.strip() not in text:
    assert anchor in text
    text = text.replace(anchor, anchor + addition)
loader.write_text(text, encoding='utf-8')

# Build using the locally available SDK; retain the upstream target behaviour.
for constants in (SMART / 'SharedModules/constants.gradle', SMART / 'MediaServiceCore/SharedModules/constants.gradle'):
    text = constants.read_text(encoding='utf-8')
    text = re.sub(r'(?m)^    compileSdkVersion = \d+', '    compileSdkVersion = 36', text)
    text = re.sub(r"(?m)^    buildToolsVersion = '[^']+'", "    buildToolsVersion = '36.0.0'", text)
    constants.write_text(text, encoding='utf-8')
app = SMART / 'smarttubetv/build.gradle'
text = app.read_text(encoding='utf-8')
# This fork has no Firebase project or telemetry backend.
start = text.find('buildscript {')
end = text.find("apply plugin: 'com.android.application'", start)
if start >= 0:
    text = text[:start] + text[end:]
start = text.find('// The first letter of the flavor name')
end = text.find('\nandroid {', start)
if start >= 0:
    assert end > start
    text = text[:start] + text[end:]
text = re.sub(r"    st(?:beta|stable)Implementation\('com.google.firebase:firebase-crashlytics:' \+ firebaseCrashlyticsVersion\) \{.*?\n    }", '', text, flags=re.S)
text = text.replace('versionName "32.69"', 'versionName "32.69-tr1"')
text = text.replace('minSdkVersion project.properties.minSdkVersion', 'minSdkVersion 23')
text = text.replace('targetSdkVersion project.properties.compileSdkVersion', 'targetSdkVersion 34')
text = text.replace('    ndkVersion "21.0.6113669"', '    // ndkVersion "21.0.6113669"')
app.write_text(text, encoding='utf-8')

for resource in (SMART / 'smarttubetv/src').glob('*/res/values*/strings.xml'):
    text = resource.read_text(encoding='utf-8')
    text = re.sub(r'(<string\b[^>]*\bname="app_name"[^>]*>).*?(</string>)', r'\1YouTube+tr TV\2', text)
    resource.write_text(text, encoding='utf-8')

# Never offer an incompatible update signed by upstream over this custom build.
update = COMMON / 'app/presenters/dialogs/AppUpdatePresenter.java'
text = update.read_text(encoding='utf-8')
text = text.replace('    public void start(boolean forceCheck) {', '''    public void start(boolean forceCheck) {
        // Modified fork: updates are distributed at github.com/OsanRey/YouTube-tr.
        if (getContext() != null) {
            if (forceCheck) com.liskovsoft.sharedutils.helpers.MessageHelpers.showMessage(getContext(),
                    "Обновления YouTube+tr TV: github.com/OsanRey/YouTube-tr/releases");
            return;
        }''') if 'Modified fork: updates' not in text else text
update.write_text(text, encoding='utf-8')
print('Applied TV branding, SDK configuration and update isolation')

settings_menu = COMMON / 'misc/AppDataSourceManager.java'
text = settings_menu.read_text(encoding='utf-8')
anchor = '        List<SettingsItem> settingItems = new ArrayList<>();'
addition = '''
        settingItems.add(new SettingsItem("Перевод Яндекса", () ->
                com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter.instance(context)
                    .getController(com.liskovsoft.smartyoutubetv2.common.vot.YandexVotController.class).showSettings(),
                R.drawable.settings_language));'''
if 'new SettingsItem("Перевод Яндекса"' not in text:
    assert anchor in text
    text = text.replace(anchor, anchor + addition)
settings_menu.write_text(text, encoding='utf-8')

properties = SMART / 'gradle.properties'
text = properties.read_text(encoding='utf-8')
for prop in ('android.overridePathCheck=true', 'android.suppressUnsupportedCompileSdk=36'):
    if prop not in text:
        text += '\n' + prop + '\n'
properties.write_text(text, encoding='utf-8')

# Upstream's Windows launcher deletes its project cache on every invocation.
# Keep the standard Gradle behaviour, including for --stop and --version.
wrapper = SMART / 'gradlew.bat'
text = wrapper.read_text(encoding='utf-8')
text = text.replace('rd /s /q "%DIRNAME%\\.gradle"', '@rem Project cache is managed by Gradle')
wrapper.write_text(text, encoding='utf-8')

assets = SMART / 'common/src/main/assets'
assets.mkdir(parents=True, exist_ok=True)
notices = '\n\n'.join([
    'YouTube+tr TV — modified build based on SmartTube, with the anddea Java VOT client.',
    (SMART / 'LICENSE').read_text(encoding='utf-8'),
    (ROOT / 'NOTICE').read_text(encoding='utf-8'),
    (ROOT / 'LICENSE').read_text(encoding='utf-8'),
    (UPSTREAM / 'VotApiClient.java').read_text(encoding='utf-8').split('package ')[0],
])
(assets / 'youtube_tr_notices.txt').write_text(notices, encoding='utf-8')
