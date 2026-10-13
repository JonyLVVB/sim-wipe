# Builds simwipe.apk with the SDK command-line tools only (no Gradle, no downloads).
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$bt = "$sdk\build-tools\36.0.0"
$jar = "$sdk\platforms\android-36\android.jar"
# needs JDK 11+; Android Studio's bundled one is used unless JAVA_HOME is already set
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$out = "$here\build"

if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force "$out\classes", "$out\dex" | Out-Null

& "$bt\aapt2.exe" link -o "$out\base.apk" --manifest "$here\AndroidManifest.xml" -I $jar `
    --min-sdk-version 29 --target-sdk-version 36 --version-code 1 --version-name 1.0
if ($LASTEXITCODE) { throw 'aapt2 failed' }

$src = Get-ChildItem "$here\src" -Recurse -Filter *.java | ForEach-Object FullName
& javac -nowarn -source 11 -target 11 -classpath $jar -d "$out\classes" $src
if ($LASTEXITCODE) { throw 'javac failed' }

$classes = Get-ChildItem "$out\classes" -Recurse -Filter *.class | ForEach-Object FullName
& "$bt\d8.bat" --lib $jar --min-api 29 --output "$out\dex" $classes
if ($LASTEXITCODE) { throw 'd8 failed' }

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::Open("$out\base.apk", 'Update')
[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, "$out\dex\classes.dex", 'classes.dex') | Out-Null
$zip.Dispose()

& "$bt\zipalign.exe" -f 4 "$out\base.apk" "$out\aligned.apk"
if ($LASTEXITCODE) { throw 'zipalign failed' }

# Keep signing.jks: updates must be signed with the same key to install over the old version.
# Neither signing.jks nor signing.pass is in the repository; a first build creates its own.
$ks = "$here\signing.jks"
$passFile = "$here\signing.pass"
if (-not (Test-Path $ks)) {
    [guid]::NewGuid().ToString('N') | Set-Content -NoNewline -Encoding ascii $passFile
    $pass = Get-Content -Raw $passFile
    & keytool -genkeypair -keystore $ks -storepass $pass -keypass $pass -alias simwipe `
        -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=SIM Wipe'
}
$pass = (Get-Content -Raw $passFile).Trim()
& "$bt\apksigner.bat" sign --ks $ks --ks-pass "pass:$pass" --key-pass "pass:$pass" `
    --out "$here\simwipe.apk" "$out\aligned.apk"
if ($LASTEXITCODE) { throw 'apksigner failed' }
& "$bt\apksigner.bat" verify --print-certs "$here\simwipe.apk" | Select-Object -First 2
Get-Item "$here\simwipe.apk" | ForEach-Object { "$($_.Name)  $($_.Length) bytes" }
