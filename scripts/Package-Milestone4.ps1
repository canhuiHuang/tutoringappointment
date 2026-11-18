param(
    [string]$OutputName = "CMPE172_Milestone4_Submission.zip"
)

$ErrorActionPreference = "Stop"
if ([System.IO.Path]::GetFileName($OutputName) -ne $OutputName -or -not $OutputName.EndsWith('.zip')) {
    throw "Supply a ZIP filename without a directory path."
}
$projectRoot = Split-Path -Parent $PSScriptRoot
$targetDirectory = Join-Path $projectRoot 'target'
New-Item -ItemType Directory -Path $targetDirectory -Force | Out-Null
$destination = Join-Path $targetDirectory $OutputName
$temporary = Join-Path $targetDirectory 'milestone4-package-in-progress.zip'
if ($destination -eq $temporary) { throw "Choose a different output filename." }
if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Force }

# Include source and documents; Git's ignore rules exclude target, dependencies and .env.
$files = git -C $projectRoot -c core.quotepath=false ls-files --cached --others --exclude-standard
if ($LASTEXITCODE -ne 0) { throw "Cannot list repository files." }
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::Open($temporary, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($relativePath in $files) {
        $source = Join-Path $projectRoot $relativePath
        if (Test-Path -LiteralPath $source -PathType Leaf) {
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
                $archive, $source, $relativePath.Replace('\', '/'),
                [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    }
} finally {
    $archive.Dispose()
}
if (Test-Path -LiteralPath $destination) { Remove-Item -LiteralPath $destination -Force }
Move-Item -LiteralPath $temporary -Destination $destination
Write-Output "Created $destination"
Write-Output "Check report details and the video link before submitting."
