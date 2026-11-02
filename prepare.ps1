param([switch]$SkipApk)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

function Get-VerifiedDownload {
    param([string]$Url, [string]$Path, [string]$Sha256)
    if (Test-Path -LiteralPath $Path) {
        $existing = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
        if ($existing -eq $Sha256) {
            Write-Host "Already verified: $Path"
            return
        }
        throw "Existing file has an unexpected SHA-256: $Path. Move it aside before retrying."
    }
    New-Item -ItemType Directory -Path (Split-Path -Parent $Path) -Force | Out-Null
    $partial = "$Path.part"
    Write-Host "Downloading: $Path"
    Invoke-WebRequest -Uri $Url -OutFile $partial -TimeoutSec 600
    if ((Get-FileHash -LiteralPath $partial -Algorithm SHA256).Hash -ne $Sha256) {
        throw "Downloaded file has an unexpected SHA-256: $partial"
    }
    Move-Item -LiteralPath $partial -Destination $Path
}

Get-VerifiedDownload `
    -Url 'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.20.0/morphe-desktop-1.20.0-all.jar' `
    -Path 'tools\morphe-cli.jar' `
    -Sha256 '679f579b3ad743ac0ca697ef71e57430d00be59411b04494afd1845c43fb79f0'
Get-VerifiedDownload `
    -Url 'https://github.com/anddea/revanced-patches/releases/download/v4.3.0/patches-4.3.0.mpp' `
    -Path 'tools\patches-4.3.0.mpp' `
    -Sha256 'deb9dbe2fe1d70300d3fe4b6c99c0042a6f3095dfb932e5d79ca40eaeb59f454'
if (-not $SkipApk) {
    Get-VerifiedDownload `
        -Url 'https://archive.org/download/jhc-apks/apks/com.google.android.youtube/com.google.android.youtube-21.13.164-all.apk' `
        -Path 'downloads\youtube-21.13.164-verified.apk' `
        -Sha256 'c193daf879d36e04886c34dcc44580b1c98c20534165d306c2a04b8025fc3f46'
}
