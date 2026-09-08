param([switch]$KeepWorkDirectoryOnFailure)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$pom = [xml][IO.File]::ReadAllText((Join-Path $root "pom.xml"))
$ns = [Xml.XmlNamespaceManager]::new($pom.NameTable)
$ns.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
$developmentVersion = $pom.SelectSingleNode("/m:project/m:version", $ns).InnerText.Trim()
if (-not $developmentVersion.EndsWith("-SNAPSHOT", [StringComparison]::Ordinal)) {
    throw "Versioned documentation simulation requires a snapshot source version."
}
$futureReleaseVersion = $developmentVersion.Substring(0, $developmentVersion.Length - "-SNAPSHOT".Length)
$work = Join-Path ([IO.Path]::GetTempPath()) ("test-lens-versioned-docs-" + [guid]::NewGuid())
$ok = $false
function TreeHash([string]$Path) {
    $items = Get-ChildItem $Path -File -Recurse | Sort-Object FullName | ForEach-Object {
        $rel = $_.FullName.Substring($Path.Length).Replace('\','/')
        "$rel=$((Get-FileHash $_.FullName -Algorithm SHA256).Hash)"
    }
    $bytes = [Text.Encoding]::UTF8.GetBytes(($items -join "`n"))
    $sha = [Security.Cryptography.SHA256]::Create()
    return ([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace("-", "")
}
try {
    New-Item -ItemType Directory -Path $work | Out-Null
    $repo = Join-Path $work "repo"
    New-Item -ItemType Directory -Path $repo | Out-Null
    foreach ($directory in @("docs", "docs-versions", "docs-versioning", "overrides", "scripts")) {
        Copy-Item -LiteralPath (Join-Path $root $directory) -Destination $repo -Recurse
    }
    foreach ($file in @("mkdocs.yml", "mkdocs-0.1.0.yml", "mkdocs-release.yml", "requirements-docs.txt", "README.md")) {
        Copy-Item -LiteralPath (Join-Path $root $file) -Destination $repo
    }
    Push-Location $repo
    try {
        & git init -q
        & git config user.name "Test Lens docs validation"
        & git config user.email "docs-validation@example.invalid"
        & git config core.autocrlf false
        & git add .
        & git commit -q -m source
        & ./scripts/publish-versioned-docs.ps1 -Operation bootstrap-0.1.0 -Confirmation publish-immutable-0.1.0 -NoPush
        & ./scripts/publish-versioned-docs.ps1 -Operation dev -Version $developmentVersion -NoPush
        $stage = Join-Path $work "site-one"
        New-Item -ItemType Directory -Path $stage | Out-Null
        & git archive gh-pages -o (Join-Path $work "pages-one.tar")
        & tar -xf (Join-Path $work "pages-one.tar") -C $stage
        $stableHash = TreeHash (Join-Path $stage "0.1.0")

        Add-Content docs/index.md "`n<!-- validation-only dev redeploy -->"
        & git add docs/index.md
        & git commit -q -m dev-change
        & ./scripts/publish-versioned-docs.ps1 -Operation dev -Version $developmentVersion -NoPush
        $stage2 = Join-Path $work "site-two"
        New-Item -ItemType Directory -Path $stage2 | Out-Null
        & git archive gh-pages -o (Join-Path $work "pages-two.tar")
        & tar -xf (Join-Path $work "pages-two.tar") -C $stage2
        if ((TreeHash (Join-Path $stage2 "0.1.0")) -ne $stableHash) { throw "Redeploying dev changed immutable 0.1.0." }

        $duplicateFailed = $false
        try { & ./scripts/publish-versioned-docs.ps1 -Operation bootstrap-0.1.0 -Confirmation publish-immutable-0.1.0 -NoPush } catch { $duplicateFailed = $true }
        if (-not $duplicateFailed) { throw "Duplicate 0.1.0 publication was not rejected." }

        $env:DOCS_RELEASE_VERSION = $futureReleaseVersion
        $env:DOCS_RELEASE_EDIT_URI = "edit/v$futureReleaseVersion/docs/"
        & ./scripts/publish-versioned-docs.ps1 -Operation release -Version $futureReleaseVersion -NoPush
        $stage3 = Join-Path $work "site-three"
        New-Item -ItemType Directory -Path $stage3 | Out-Null
        & git archive gh-pages -o (Join-Path $work "pages-three.tar")
        & tar -xf (Join-Path $work "pages-three.tar") -C $stage3

        foreach ($required in @(
            "0.1.0/index.html", "0.1.0/search/search_index.json",
            "dev/index.html", "dev/search/search_index.json",
            "latest/index.html", "index.html", "versions.json"
        )) {
            if (-not (Test-Path (Join-Path $stage2 $required))) { throw "Missing versioned output: $required" }
        }
        foreach ($versionDirectory in @("0.1.0", "dev")) {
            $assets = Join-Path $stage2 "$versionDirectory/assets"
            if (-not (Get-ChildItem $assets -Filter *.css -File -Recurse)) { throw "No CSS assets published for $versionDirectory." }
            if (-not (Get-ChildItem $assets -Filter *.js -File -Recurse)) { throw "No JavaScript assets published for $versionDirectory." }
        }
        $banner = "Development documentation for"
        foreach ($html in Get-ChildItem (Join-Path $stage2 "dev") -Filter *.html -File -Recurse) {
            $text = [IO.File]::ReadAllText($html.FullName)
            if (-not $text.Contains($banner)) { throw "Development banner missing: $($html.FullName)" }
            if (-not $text.Contains('class="tl-version-switcher"')) { throw "Version switcher missing from dev: $($html.FullName)" }
        }
        $devHome = [IO.File]::ReadAllText((Join-Path $stage2 "dev/index.html"))
        if (-not $devHome.Contains("edit/main/docs/index.md")) { throw "dev edit link does not target main." }
        foreach ($html in Get-ChildItem (Join-Path $stage2 "0.1.0") -Filter *.html -File -Recurse) {
            $text = [IO.File]::ReadAllText($html.FullName)
            if ($text.Contains($banner)) { throw "Development banner leaked into stable docs." }
            if ($text.Contains("edit/main/docs/")) { throw "Stable edit link points to main." }
            if (-not $text.Contains('class="tl-version-switcher"')) { throw "Version switcher missing from stable docs." }
        }
        $versions = Get-Content (Join-Path $stage2 "versions.json") -Raw | ConvertFrom-Json
        if (-not (($versions.version -contains "0.1.0") -and ($versions.version -contains "dev"))) { throw "mike metadata lacks stable/dev versions." }
        $future = Get-Content (Join-Path $stage3 "versions.json") -Raw | ConvertFrom-Json
        $futureRelease = $future | Where-Object version -eq $futureReleaseVersion
        if ($null -eq $futureRelease -or $futureRelease.aliases -notcontains "latest") { throw "Future release did not move latest." }
        if (-not (Test-Path (Join-Path $stage3 "0.1.0/index.html"))) { throw "Future release removed 0.1.0." }
        $futureHome = [IO.File]::ReadAllText((Join-Path $stage3 "$futureReleaseVersion/index.html"))
        if (-not $futureHome.Contains("edit/v$futureReleaseVersion/docs/index.md")) { throw "Tagged release edit link does not target its tag." }
        $rootRedirect = [IO.File]::ReadAllText((Join-Path $stage2 "index.html"))
        if (-not $rootRedirect.Contains('url=latest/')) { throw "Root default does not redirect to latest." }
        $latestHome = [IO.File]::ReadAllText((Join-Path $stage2 "latest/index.html"))
        if (-not $latestHome.Contains("Selenium Test Lens 0.1.0")) { throw "latest does not serve stable 0.1.0." }
        Write-Host "Versioned docs simulation OK: stable immutable, duplicate rejected, future latest advanced."
        $ok = $true
    } finally { Pop-Location }
} finally {
    if ($ok -or -not $KeepWorkDirectoryOnFailure) { Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue }
    else { Write-Warning "Validation work directory retained: $work" }
}
