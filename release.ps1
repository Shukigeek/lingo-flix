# LingoFlix Automated Release Script (Improved)

# 0. Check for Keystore (Now checking in app/ folder where Gradle expects it)
$keystorePath = "app/release.keystore"
if (-not (Test-Path $keystorePath)) {
    Write-Error "ERROR: release.keystore not found at $keystorePath!"
    Write-Host "Please make sure the keystore file is inside the 'app' folder." -ForegroundColor Yellow
    exit
}

# 1. Get current version info
$gradleFile = "app/build.gradle.kts"
$content = Get-Content $gradleFile -Raw

$versionNameMatch = [regex]::Match($content, 'versionName = "(.*)"')
$versionCodeMatch = [regex]::Match($content, 'versionCode = (\d+)')

if (-not $versionNameMatch.Success -or -not $versionCodeMatch.Success) {
    Write-Error "Could not find version info in $gradleFile"
    exit
}

$currentVersion = $versionNameMatch.Groups[1].Value
$currentCode = [int]$versionCodeMatch.Groups[1].Value

# 2. Ask for new version
Write-Host "Current Version: $currentVersion (Code: $currentCode)" -ForegroundColor Yellow
$newVersion = Read-Host "Enter new version name (leave empty to keep $currentVersion)"
if ([string]::IsNullOrWhiteSpace($newVersion)) { $newVersion = $currentVersion }
$newCode = $currentCode + 1

Write-Host "Target Version: $newVersion (Code: $newCode)" -ForegroundColor Cyan

# 3. Update build.gradle.kts
$newContent = $content -replace 'versionCode = \d+', "versionCode = $newCode"
$newContent = $newContent -replace 'versionName = ".*?"', "versionName = `"$newVersion`""
Set-Content $gradleFile $newContent

# 4. Build APK
Write-Host "Building Signed APK..." -ForegroundColor Cyan
./gradlew assembleRelease

if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed! Make sure local.properties has the correct RELEASE_STORE_PASSWORD, etc."
    Set-Content $gradleFile $content
    exit
}

# Determine APK path
$apkPath = "app/build/outputs/apk/release/app-release.apk"
if (-not (Test-Path $apkPath)) {
    Write-Error "Could not find APK at $apkPath"
    exit
}

# 5. GitHub Release
Write-Host "Creating GitHub Release..." -ForegroundColor Cyan

# Attempt to find gh.exe if not in PATH
$ghCmd = "gh"
if (-not (Get-Command "gh" -ErrorAction SilentlyContinue)) {
    $ghPath = "$env:LocalAppData\Microsoft\WinGet\Packages\GitHub.cli_Microsoft.Winget.Source_8wekyb3d8bbwe\gh.exe"
    if (Test-Path $ghPath) {
        $ghCmd = "& `"$ghPath`""
    } else {
        $ghPath = "C:\Program Files\GitHub CLI\gh.exe"
        if (Test-Path $ghPath) {
            $ghCmd = "& `"$ghPath`""
        }
    }
}

# Delete existing release/tag if it exists to allow re-uploading
Invoke-Expression "$ghCmd release delete v$newVersion --yes" 2>$null
git push --delete origin "v$newVersion" 2>$null

# Create new release
Invoke-Expression "$ghCmd release create v$newVersion $apkPath --title `"Release v$newVersion`" --notes `"Automated release of LingoFlix v$newVersion`""

if ($LASTEXITCODE -ne 0) {
    Write-Error "GitHub Release failed! Check if you are logged in using 'gh auth login'"
    exit
}

# 6. Update index.html
Write-Host "Updating index.html..." -ForegroundColor Cyan
$htmlFile = "index.html"
$htmlContent = Get-Content $htmlFile -Raw
$newDownloadUrl = "https://github.com/Shukigeek/lingo-flix/releases/download/v$newVersion/app-release.apk"

# Robust replacement for the specific download button link
$htmlContent = $htmlContent -replace 'https://github.com/Shukigeek/lingo-flix/releases/download/.*?/app-release\.apk"', "$newDownloadUrl`""

Set-Content $htmlFile $htmlContent

# 7. Git Commit & Push
Write-Host "Committing and Pushing to Git..." -ForegroundColor Cyan
git add .
git commit -m "Release v$newVersion (Code: $newCode)"
git push

Write-Host "Successfully released v$newVersion!" -ForegroundColor Green
