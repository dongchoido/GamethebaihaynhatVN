param([string]$JdkPath)

$ErrorActionPreference = 'Stop'
if ($env:OS -ne 'Windows_NT') { throw 'Run this script on Windows.' }

# Find a full JDK. Players do not need this JDK; it is only used to build.
if (-not $JdkPath) {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) { $candidates += Split-Path (Split-Path $compiler.Source -Parent) -Parent }
    $javaDirectory = Join-Path $env:ProgramFiles 'Java'
    if (Test-Path -LiteralPath $javaDirectory) {
        $candidates += Get-ChildItem -LiteralPath $javaDirectory -Directory |
            Sort-Object LastWriteTime -Descending | ForEach-Object { $_.FullName }
    }
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath (Join-Path $candidate 'bin/jpackage.exe')) {
            $JdkPath = $candidate
            break
        }
    }
}
if (-not $JdkPath) { throw 'JDK 17+ not found. Use -JdkPath to select a full JDK.' }
$JdkPath = (Resolve-Path -LiteralPath $JdkPath).Path
foreach ($tool in @('javac', 'jar', 'jlink', 'jpackage')) {
    if (-not (Test-Path -LiteralPath (Join-Path $JdkPath "bin/$tool.exe"))) {
        throw "Missing $tool.exe in JDK: $JdkPath"
    }
}

# Unique output folders keep previous releases and their saved matches intact.
$buildId = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 6)
$work = Join-Path $PSScriptRoot "target/windows-$buildId"
$output = Join-Path $PSScriptRoot "dist/windows-$buildId"
$classes = Join-Path $work 'classes'
$inputDirectory = Join-Path $work 'input'
New-Item -ItemType Directory -Path $classes, $inputDirectory, $output -Force | Out-Null

Write-Host '1/4 Compiling Java sources...'
$sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src/main/java') -Recurse -Filter '*.java' |
    ForEach-Object { $_.FullName })
& (Join-Path $JdkPath 'bin/javac.exe') --release 17 -encoding UTF-8 -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed; JDK 17+ is required.' }

Write-Host '2/4 Packaging code, images and sounds...'
$jarFile = Join-Path $inputDirectory 'hearthstone-swing.jar'
& (Join-Path $JdkPath 'bin/jar.exe') --create --file $jarFile --main-class hearthstone.Main `
    -C $classes . -C (Join-Path $PSScriptRoot 'src/main/resources') .
if ($LASTEXITCODE -ne 0) { throw 'JAR creation failed.' }

Write-Host '3/4 Creating Windows launcher and bundled Java runtime...'
$packageArgs = @(
    '--type', 'app-image', '--name', 'HearthstoneSwing', '--app-version', '1.0.0',
    '--description', 'Offline card game for two players',
    '--input', $inputDirectory, '--main-jar', 'hearthstone-swing.jar',
    '--main-class', 'hearthstone.Main', '--dest', $output,
    '--module-path', (Join-Path $JdkPath 'jmods'), '--add-modules', 'java.desktop',
    '--jlink-options', '--strip-debug --no-man-pages --no-header-files',
    '--java-options', '-Dhearthstone.dataDir=$APPDIR/../data'
)
& (Join-Path $JdkPath 'bin/jpackage.exe') @packageArgs
if ($LASTEXITCODE -ne 0) { throw 'Windows packaging failed.' }

$application = Join-Path $output 'HearthstoneSwing'
$instructions = @'
HEARTHSTONE SWING - WINDOWS

1. Extract the entire ZIP to a writable path without accented characters.
   Example: C:\Games\HearthstoneSwing. Spaces are supported.
2. Open HearthstoneSwing.exe to play. No NetBeans or Java installation is needed.
3. Keep app/ and runtime/ beside the EXE. Do not copy only the EXE.

Saved users, deck selections and match history are in data/ beside the EXE.
Each new build starts with no saved matches. To keep your history, close the game
and copy data/ from your previous release into the new HearthstoneSwing folder.
You can create a Windows shortcut to the EXE without moving the EXE itself.
The bundled Java version may fail to start when its folder path contains
Vietnamese accents. Use an unaccented path for this release.

This release is built for the Windows architecture of the JDK used to build it.
'@
Set-Content -LiteralPath (Join-Path $application 'READ-ME.txt') -Value $instructions -Encoding UTF8

Write-Host '4/4 Creating ZIP for sharing...'
$zipFile = Join-Path $output 'HearthstoneSwing-Windows.zip'
Compress-Archive -LiteralPath $application -DestinationPath $zipFile -CompressionLevel Optimal
Write-Output "EXE: $(Join-Path $application 'HearthstoneSwing.exe')"
Write-Output "ZIP: $zipFile"
