param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$AndroidSdk = $env:ANDROID_HOME,
    [switch]$PrepareOnly
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
Set-Location $taskRoot
if (!$JavaHome -and (Test-Path 'tools/jdk17/jdk-17.0.21+9')) {
    $JavaHome = (Resolve-Path 'tools/jdk17/jdk-17.0.21+9').Path
}
if (!$AndroidSdk) { $AndroidSdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
if (!(Test-Path "$JavaHome/bin/java.exe")) { throw 'Specify -JavaHome with JDK 17' }
if ((& "$JavaHome/bin/java.exe" -version 2>&1 | Out-String) -notmatch 'version "17\.') {
    throw 'This pinned Gradle toolchain requires JDK 17; specify -JavaHome explicitly'
}
if (!(Test-Path "$AndroidSdk/platforms/android-36/android.jar")) { throw 'Install Android SDK platform 36' }
if (!(Test-Path "$AndroidSdk/build-tools/36.0.0/apksigner.bat")) { throw 'Install Android SDK build-tools 36.0.0' }
$env:JAVA_HOME = $JavaHome
$env:GRADLE_USER_HOME = Join-Path $taskRoot 'build-cache/tv-gradle'
$pins = Get-Content "$PSScriptRoot/upstream.json" -Raw | ConvertFrom-Json
New-Item -ItemType Directory -Force sources,logs,output,build-cache | Out-Null
foreach ($pair in @(@('SmartTube', $pins.SmartTube), @('anddea-patches', $pins.anddea))) {
    $repoPath = Join-Path $taskRoot "sources/$($pair[0])"
    if (!(Test-Path "$repoPath/.git")) {
        if (Test-Path "$repoPath/.youtube-tr-upstream") {
            if ((Get-Content "$repoPath/.youtube-tr-upstream" -Raw).Trim() -ne $pair[1].commit) {
                throw "Unexpected bundled source revision: $repoPath"
            }
            continue
        }
        if (Test-Path $repoPath) { throw "Existing source folder has no revision metadata: $repoPath" }
        & git clone --no-checkout --filter=blob:none $pair[1].repository $repoPath
        if ($LASTEXITCODE) { throw 'Source clone failed' }
        & git -C $repoPath checkout --detach $pair[1].commit
        if ($LASTEXITCODE) { throw 'Pinned checkout failed' }
    }
    $head = (& git -C $repoPath rev-parse HEAD).Trim()
    if ($head -ne $pair[1].commit) { throw "Unexpected upstream revision: $repoPath" }
}
if (Test-Path sources/SmartTube/.git) {
    & git -C sources/SmartTube submodule update --init --recursive
    if ($LASTEXITCODE) { throw 'Submodule checkout failed' }
} else {
    foreach ($module in @(
        @('SharedModules', $pins.SharedModules.commit),
        @('MediaServiceCore', $pins.MediaServiceCore.commit),
        @('MediaServiceCore/SharedModules', $pins.SharedModules.commit))) {
        $marker = "sources/SmartTube/$($module[0])/.youtube-tr-upstream"
        if (!(Test-Path $marker) -or (Get-Content $marker -Raw).Trim() -ne $module[1]) {
            throw "Missing or unexpected bundled module: $($module[0])"
        }
    }
}
& python "$PSScriptRoot/prepare_source.py"
if ($LASTEXITCODE) { throw 'TV overlay failed' }
Set-Content sources/SmartTube/local.properties "sdk.dir=$($AndroidSdk.Replace('\','/'))" -Encoding ascii
$classes = Join-Path $taskRoot 'build-cache/tv-protocol-tests'
New-Item -ItemType Directory -Force $classes | Out-Null
& "$JavaHome/bin/javac.exe" -encoding UTF-8 -source 8 -target 8 -d $classes `
    sources/SmartTube/common/src/main/java/com/liskovsoft/smartyoutubetv2/common/vot/VotProtobuf.java `
    tv/tests/VotProtocolTest.java
if ($LASTEXITCODE) { throw 'Protocol test compilation failed' }
& "$JavaHome/bin/java.exe" -cp $classes com.liskovsoft.smartyoutubetv2.common.vot.VotProtocolTest
if ($LASTEXITCODE) { throw 'Protocol tests failed' }
if ($PrepareOnly) { return }
& sources/SmartTube/gradlew.bat -p sources/SmartTube :smarttubetv:assembleStstableRelease --console=plain --max-workers=4 `
    "-Pandroid.aapt2FromMavenOverride=$AndroidSdk/build-tools/36.0.0/aapt2.exe"
if ($LASTEXITCODE) { throw 'TV build failed' }

$unsigned = 'sources/SmartTube/smarttubetv/build/outputs/apk/ststable/release/SmartTube_stable_32.69-tr1_universal.apk'
if (!(Test-Path $unsigned)) { throw "APK not found: $unsigned" }
$signingFile = Join-Path $taskRoot 'build-cache/tv-signing.json'
if (!(Test-Path $signingFile) -or !(Test-Path 'build-cache/tv-signing.p12')) {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $rng.Dispose()
    $password = [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
    @{ password = $password; alias = 'youtube-tr-tv' } | ConvertTo-Json | Set-Content $signingFile -Encoding utf8
    $env:YTTR_TV_KEYPASS = $password
    & "$JavaHome/bin/keytool.exe" -genkeypair -keystore build-cache/tv-signing.p12 -storetype PKCS12 `
        -storepass:env YTTR_TV_KEYPASS -keypass:env YTTR_TV_KEYPASS -alias youtube-tr-tv `
        -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=YouTube tr TV, O=YouTube-tr'
    if ($LASTEXITCODE) { throw 'Key generation failed' }
}
$signing = Get-Content $signingFile -Raw | ConvertFrom-Json
$env:YTTR_TV_KEYPASS = $signing.password
try {
    & "$AndroidSdk/build-tools/36.0.0/zipalign.exe" -f -p 4 $unsigned build-cache/tv-aligned.apk
    if ($LASTEXITCODE) { throw 'APK alignment failed' }
    $output = Join-Path $taskRoot 'output/YouTube+tr-TV-32.69-tr1-universal.apk'
    & "$AndroidSdk/build-tools/36.0.0/apksigner.bat" sign --ks build-cache/tv-signing.p12 `
        --ks-pass env:YTTR_TV_KEYPASS --key-pass env:YTTR_TV_KEYPASS --ks-key-alias $signing.alias `
        --out $output build-cache/tv-aligned.apk
    if ($LASTEXITCODE) { throw 'APK signing failed' }
    & "$AndroidSdk/build-tools/36.0.0/apksigner.bat" verify --verbose --print-certs $output
    if ($LASTEXITCODE) { throw 'APK signature verification failed' }
    $hash = (Get-FileHash $output -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  YouTube+tr-TV-32.69-tr1-universal.apk" | Set-Content output/TV-SHA256SUMS.txt -Encoding ascii
    Write-Output "Built: $output"
} finally { Remove-Item Env:YTTR_TV_KEYPASS -ErrorAction SilentlyContinue }
