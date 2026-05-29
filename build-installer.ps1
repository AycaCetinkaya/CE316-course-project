param(
    [string]$Version = "1.0.0",
    [string]$InnoSetupCompiler = "C:\Program Files (x86)\Inno Setup 6\ISCC.exe"
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$BuildDir = Join-Path $Root "build"
$DistDir = Join-Path $Root "dist"
$AppDir = Join-Path $DistDir "app"
$InstallerDir = Join-Path $DistDir "installer"
$ClassesDir = Join-Path $BuildDir "classes"
$RuntimeDir = Join-Path $AppDir "runtime"
$ManifestPath = Join-Path $BuildDir "MANIFEST.MF"
$SqliteJar = Join-Path $Root "lib\sqlite-jdbc-3.53.1.0.jar"
$InnoScript = Join-Path $Root "installer\IAE.iss"

function Assert-UnderRoot {
    param([string]$Path)

    $resolvedRoot = [System.IO.Path]::GetFullPath($Root)
    $resolvedPath = [System.IO.Path]::GetFullPath($Path)
    if (-not $resolvedPath.StartsWith($resolvedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify path outside workspace: $resolvedPath"
    }
}

foreach ($path in @($BuildDir, $AppDir)) {
    Assert-UnderRoot $path
    if (Test-Path $path) {
        Remove-Item -LiteralPath $path -Recurse -Force
    }
}

New-Item -ItemType Directory -Force -Path $ClassesDir, $AppDir, $InstallerDir | Out-Null

$sources = Get-ChildItem -Path (Join-Path $Root "src") -Filter "*.java" |
    Where-Object { $_.Name -notlike "*Test.java" -and $_.Name -ne "ZipServiceDemo.java" } |
    ForEach-Object { $_.FullName }

if (-not $sources) {
    throw "No Java sources found."
}

& javac --release 17 -encoding UTF-8 -cp $SqliteJar -d $ClassesDir $sources
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

@"
Manifest-Version: 1.0
Main-Class: Main
Class-Path: lib/sqlite-jdbc-3.53.1.0.jar

"@ | Set-Content -Path $ManifestPath -Encoding ASCII

& jar cfm (Join-Path $AppDir "IAE.jar") $ManifestPath -C $ClassesDir .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE"
}

New-Item -ItemType Directory -Force -Path (Join-Path $AppDir "lib") | Out-Null
Copy-Item -LiteralPath $SqliteJar -Destination (Join-Path $AppDir "lib")
Copy-Item -LiteralPath (Join-Path $Root "README.md") -Destination $AppDir

& jlink --add-modules java.base,java.desktop,java.sql,java.logging,jdk.charsets --strip-debug --no-header-files --no-man-pages --compress zip-6 --output $RuntimeDir
if ($LASTEXITCODE -ne 0) {
    throw "jlink failed with exit code $LASTEXITCODE"
}

if (-not (Test-Path $InnoSetupCompiler)) {
    throw "Inno Setup compiler not found: $InnoSetupCompiler"
}

& $InnoSetupCompiler "/DAppVersion=$Version" $InnoScript
if ($LASTEXITCODE -ne 0) {
    throw "Inno Setup compiler failed with exit code $LASTEXITCODE"
}

Write-Host "Installer created in $InstallerDir"
