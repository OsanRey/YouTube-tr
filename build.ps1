param(
    [string]$Java = 'java',
    [string]$Apk = (Join-Path $PSScriptRoot 'downloads\youtube-21.13.164-verified.apk')
)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$inputs = @{
    'tools\morphe-cli.jar' = '679f579b3ad743ac0ca697ef71e57430d00be59411b04494afd1845c43fb79f0'
    'tools\patches-4.3.0.mpp' = 'deb9dbe2fe1d70300d3fe4b6c99c0042a6f3095dfb932e5d79ca40eaeb59f454'
}
foreach ($item in $inputs.GetEnumerator()) {
    if ((Get-FileHash -LiteralPath $item.Key -Algorithm SHA256).Hash.ToLowerInvariant() -ne $item.Value) {
        throw "SHA-256 mismatch: $($item.Key)"
    }
}
if ((Get-FileHash -LiteralPath $Apk -Algorithm SHA1).Hash.ToLowerInvariant() -ne 'e67829376d9644c7e6e1b54c7a702f2c9b3b3d58') {
    throw 'Unexpected input APK. This configuration requires the verified YouTube 21.13.164 universal APK.'
}
New-Item -ItemType Directory -Path 'build-cache', 'output', 'logs' -Force | Out-Null
$env:MORPHE_DATA_DIR = Join-Path $PSScriptRoot 'build-cache'
$patchedBundle = Join-Path $env:MORPHE_DATA_DIR 'local-patches\patches-4.3.0.mpp'
& (Join-Path $PSScriptRoot 'apply-resource-overrides.ps1') -Bundle 'tools\patches-4.3.0.mpp' -Out $patchedBundle
$buildArgs = @(
    '-Xmx6g', '-jar', 'tools\morphe-cli.jar', 'patch',
    '-p', $patchedBundle,
    '--options-file', 'build-options.json',
    '--striplibs', 'arm64-v8a',
    '-o', 'output\YouTube+tr-21.13.164.apk',
    '-r', 'output\patch-result.json',
    $Apk
)
& $Java @buildArgs 2>&1 | Tee-Object -FilePath 'logs\build.log'
if ($LASTEXITCODE -ne 0) { throw "Patching failed: exit $LASTEXITCODE. See logs\build.log" }
Copy-Item -LiteralPath 'build-cache\morphe.keystore' -Destination 'output\tablet-vot.keystore' -Force
Get-FileHash -LiteralPath 'output\YouTube+tr-21.13.164.apk' -Algorithm SHA256 |
    Format-List | Out-File -LiteralPath 'output\SHA256.txt' -Encoding utf8
