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
    $remote = Join-Path $work "remote.git"
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
        & git init -q --bare $remote
        & git remote add origin $remote
        & git push -q -u origin HEAD:main

        # Exercise the no-push path independently, including the title with spaces.
        & ./scripts/publish-versioned-docs.ps1 -Operation dev -Version $developmentVersion -Branch gh-pages-no-push -NoPush
        $noPushMetadata = (& git show gh-pages-no-push`:versions.json) -join "`n" | ConvertFrom-Json
        $noPushDev = $noPushMetadata | Where-Object version -eq "dev"
        if ($null -eq $noPushDev -or $noPushDev.title -ne "$developmentVersion / coming soon") {
            throw "No-push dev deployment did not preserve the exact title argument."
        }

        $remotePagesBefore = @(& git ls-remote --heads origin gh-pages)
        if ($remotePagesBefore.Count -ne 0) {
            throw "Test remote unexpectedly contained gh-pages before the first pushed deployment."
        }
        & ./scripts/publish-versioned-docs.ps1 -Operation dev -Version $developmentVersion
        $remotePagesAfter = @(& git ls-remote --heads origin gh-pages)
        if ($remotePagesAfter.Count -eq 0) {
            throw "First pushed dev deployment did not create gh-pages in the local bare remote."
        }
        & ./scripts/publish-versioned-docs.ps1 -Operation bootstrap-0.1.0 -Confirmation publish-immutable-0.1.0
        $stage = Join-Path $work "site-one"
        New-Item -ItemType Directory -Path $stage | Out-Null
        & git archive gh-pages -o (Join-Path $work "pages-one.tar")
        & tar -xf (Join-Path $work "pages-one.tar") -C $stage
        $stableHash = TreeHash (Join-Path $stage "0.1.0")

        Add-Content docs/index.md "`n<!-- validation-only dev redeploy -->"
        & git add docs/index.md
        & git commit -q -m dev-change
        & ./scripts/publish-versioned-docs.ps1 -Operation dev -Version $developmentVersion
        $stage2 = Join-Path $work "site-two"
        New-Item -ItemType Directory -Path $stage2 | Out-Null
        & git archive gh-pages -o (Join-Path $work "pages-two.tar")
        & tar -xf (Join-Path $work "pages-two.tar") -C $stage2
        if ((TreeHash (Join-Path $stage2 "0.1.0")) -ne $stableHash) { throw "Redeploying dev changed immutable 0.1.0." }

        $repairVersionRejected = $false
        try {
            & ./scripts/publish-versioned-docs.ps1 -Operation repair-0.1.0-docs -Version 9.9.9 -Confirmation repair-immutable-0.1.0-docs -NoPush
        } catch { $repairVersionRejected = $true }
        if (-not $repairVersionRejected) { throw "The fixed 0.1.0 repair accepted an arbitrary version." }

        $devHashBeforeRepair = TreeHash (Join-Path $stage2 "dev")
        & git branch -D gh-pages
        if ($LASTEXITCODE -ne 0) { throw "Could not remove the local publication branch for the fresh-checkout repair simulation." }
        Add-Content docs-versions/0.1.0/index.md "`n<!-- validation-only historical repair -->"
        & git add docs-versions/0.1.0/index.md
        & git commit -q -m stable-repair-source
        & ./scripts/publish-versioned-docs.ps1 -Operation repair-0.1.0-docs -Confirmation repair-immutable-0.1.0-docs
        $stageRepair = Join-Path $work "site-repair"
        New-Item -ItemType Directory -Path $stageRepair | Out-Null
        & git archive gh-pages -o (Join-Path $work "pages-repair.tar")
        & tar -xf (Join-Path $work "pages-repair.tar") -C $stageRepair
        if ((TreeHash (Join-Path $stageRepair "dev")) -ne $devHashBeforeRepair) {
            throw "Repairing 0.1.0 changed the published dev tree."
        }
        if ((TreeHash (Join-Path $stageRepair "0.1.0")) -eq $stableHash) {
            throw "The controlled repair did not update the 0.1.0 tree."
        }
        if (-not ([IO.File]::ReadAllText((Join-Path $stageRepair "latest/index.html"))).Contains("validation-only historical repair")) {
            throw "The latest alias was not refreshed from repaired 0.1.0 documentation."
        }

        $duplicateFailed = $false
        try { & ./scripts/publish-versioned-docs.ps1 -Operation bootstrap-0.1.0 -Confirmation publish-immutable-0.1.0 -NoPush } catch { $duplicateFailed = $true }
        if (-not $duplicateFailed) { throw "Duplicate 0.1.0 publication was not rejected." }

        $env:DOCS_RELEASE_VERSION = $futureReleaseVersion
        $env:DOCS_RELEASE_EDIT_URI = "edit/v$futureReleaseVersion/docs/"
        & ./scripts/publish-versioned-docs.ps1 -Operation release -Version $futureReleaseVersion
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
        $devVersion = $versions | Where-Object version -eq "dev"
        if ($null -eq $devVersion -or $devVersion.title -ne "$developmentVersion / coming soon") {
            throw "Pushed dev metadata did not preserve the exact title argument."
        }
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
        Write-Host "Versioned docs simulation OK: stable repair preserved dev, ordinary duplicate rejected, future latest advanced."
        $ok = $true
    } finally { Pop-Location }
} finally {
    if ($ok -or -not $KeepWorkDirectoryOnFailure) { Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue }
    else { Write-Warning "Validation work directory retained: $work" }
}
