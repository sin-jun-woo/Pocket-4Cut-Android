$ErrorActionPreference = 'Stop'
$reelBase = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$reelOutput = Join-Path $reelBase 'deliverables'
$reelNames = @(
    'Pocket4Cut-Reels-30s.mp4',
    'Pocket4Cut-Reels-30s-NoMusic.mp4',
    'Pocket4Cut-Reels-Cover.jpg',
    'INSTAGRAM-CAPTION.txt',
    'CAPTIONS-KO.srt',
    'UPLOAD-GUIDE.md'
)
$reelFiles = @($reelNames | ForEach-Object { Join-Path $reelOutput $_ })
foreach ($reelFile in $reelFiles) {
    if (-not (Test-Path -LiteralPath $reelFile -PathType Leaf)) { throw "Missing deliverable: $reelFile" }
}
$reelZipPath = Join-Path $reelBase 'Pocket4Cut-Reels-Package.zip'
Compress-Archive -LiteralPath $reelFiles -DestinationPath $reelZipPath -CompressionLevel Optimal -Force

# Read every ZIP entry without extracting it and compare it to the exact source.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$reelArchive = [System.IO.Compression.ZipFile]::OpenRead($reelZipPath)
try {
    if ($reelArchive.Entries.Count -ne $reelNames.Count) { throw 'Unexpected ZIP entry count.' }
    foreach ($reelEntry in $reelArchive.Entries) {
        if ($reelEntry.FullName -notin $reelNames) { throw "Unexpected ZIP entry: $($reelEntry.FullName)" }
        $reelOriginal = Join-Path $reelOutput $reelEntry.FullName
        $reelStream = $reelEntry.Open()
        $reelHasher = [System.Security.Cryptography.SHA256]::Create()
        try {
            $reelEntryHash = [System.BitConverter]::ToString($reelHasher.ComputeHash($reelStream)).Replace('-', '')
        } finally {
            $reelStream.Dispose()
            $reelHasher.Dispose()
        }
        if ($reelEntryHash -ne (Get-FileHash -LiteralPath $reelOriginal -Algorithm SHA256).Hash) {
            throw "ZIP hash mismatch: $($reelEntry.FullName)"
        }
    }
    Write-Output "ZIP: $($reelArchive.Entries.Count) entries, all source SHA-256 values match."
} finally { $reelArchive.Dispose() }
[PSCustomObject]@{
    File = [System.IO.Path]::GetFileName($reelZipPath)
    Bytes = (Get-Item -LiteralPath $reelZipPath).Length
    SHA256 = (Get-FileHash -LiteralPath $reelZipPath -Algorithm SHA256).Hash
} | ConvertTo-Json
