$ErrorActionPreference = 'Stop'
$reelBase = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$reelOutput = Join-Path $reelBase 'deliverables'
$reelNames = @(
    'Pocket4Cut-Reels-Reference-Voice.mp4',
    'Pocket4Cut-Reference-Narration.mp3',
    'Pocket4Cut-Reference-Cover.jpg',
    'INSTAGRAM-CAPTION.txt',
    'CAPTIONS-KO.srt',
    'NARRATION-KO.txt',
    'UPLOAD-GUIDE.md'
)
$reelFiles = @($reelNames | ForEach-Object { Join-Path $reelOutput $_ })
foreach ($reelFile in $reelFiles) {
    if (-not (Test-Path -LiteralPath $reelFile -PathType Leaf)) { throw "Missing deliverable: $reelFile" }
}
$reelZipPath = Join-Path $reelBase 'Pocket4Cut-Reference-Reels-Package.zip'
Compress-Archive -LiteralPath $reelFiles -DestinationPath $reelZipPath -CompressionLevel Optimal -Force
Add-Type -AssemblyName System.IO.Compression.FileSystem
$reelArchive = [System.IO.Compression.ZipFile]::OpenRead($reelZipPath)
$reelEntries = @()
try {
    if ($reelArchive.Entries.Count -ne $reelNames.Count) { throw 'Unexpected ZIP entry count.' }
    foreach ($reelEntry in $reelArchive.Entries) {
        if ($reelEntry.FullName -notin $reelNames) { throw "Unexpected ZIP entry: $($reelEntry.FullName)" }
        $reelOriginal = Join-Path $reelOutput $reelEntry.FullName
        $reelStream = $reelEntry.Open()
        $reelHasher = [System.Security.Cryptography.SHA256]::Create()
        try {
            $reelEntryHash = [System.BitConverter]::ToString($reelHasher.ComputeHash($reelStream)).Replace('-', '')
        } finally { $reelStream.Dispose(); $reelHasher.Dispose() }
        if ($reelEntryHash -ne (Get-FileHash -LiteralPath $reelOriginal -Algorithm SHA256).Hash) {
            throw "ZIP hash mismatch: $($reelEntry.FullName)"
        }
        $reelEntries += [PSCustomObject]@{ File=$reelEntry.FullName; SHA256=$reelEntryHash; Match=$true }
    }
} finally { $reelArchive.Dispose() }
$reelReport = [PSCustomObject]@{
    CheckedAt = [DateTime]::UtcNow.ToString('o')
    File = [System.IO.Path]::GetFileName($reelZipPath)
    Bytes = (Get-Item -LiteralPath $reelZipPath).Length
    SHA256 = (Get-FileHash -LiteralPath $reelZipPath -Algorithm SHA256).Hash
    Entries = $reelEntries
}
$reelReport | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $reelBase 'verification/package-validation.json') -Encoding UTF8
$reelReport | ConvertTo-Json -Depth 5
