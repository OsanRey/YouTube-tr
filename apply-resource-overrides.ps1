param(
    [Parameter(Mandatory)][string]$Bundle,
    [Parameter(Mandatory)][string]$Out
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$source = (Resolve-Path -LiteralPath $Bundle).Path
$target = [System.IO.Path]::GetFullPath($Out)
if ($source -eq $target) { throw 'The original patch bundle must remain unchanged.' }
$overrides = Join-Path $PSScriptRoot 'overrides'
New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
Copy-Item -LiteralPath $source -Destination $target -Force
$archive = [System.IO.Compression.ZipFile]::Open($target, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    foreach ($file in Get-ChildItem -LiteralPath $overrides -File -Recurse) {
        $entryName = [System.IO.Path]::GetRelativePath($overrides, $file.FullName).Replace('\', '/')
        $entry = $archive.GetEntry($entryName)
        if ($null -eq $entry) { throw "Upstream resource not found: $entryName" }
        $null = [xml](Get-Content -Raw -LiteralPath $file.FullName)
        $entry.Delete()
        $null = [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $file.FullName, $entryName)
        Write-Host "Resource override: $entryName"
    }
} finally {
    $archive.Dispose()
}
