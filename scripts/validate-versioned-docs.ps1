param([switch]$KeepWorkDirectoryOnFailure)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$pom = [xml][IO.File]::ReadAllText((Join-Path $root "pom.xml"))
$ns = [Xml.XmlNamespaceManager]::new($pom.NameTable)
$ns.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
$developmentVersion = $pom.SelectSingleNode("/m:project/m:version", $ns).InnerText.Trim()
if ($developmentVersion -notmatch '^\d+\.\d+\.\d+(-SNAPSHOT)?$') {
    throw "Versioned documentation simulation requires a semantic release or snapshot source version."
}
$sourceIsSnapshot = $developmentVersion.EndsWith("-SNAPSHOT", [StringComparison]::Ordinal)
$futureReleaseVersion = if ($sourceIsSnapshot) {
    $developmentVersion.Substring(0, $developmentVersion.Length - "-SNAPSHOT".Length)
} else {
    $developmentVersion
}
$developmentTitle = if ($sourceIsSnapshot) { "$developmentVersion / coming soon" } else { $developmentVersion }
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
function RepairTempDirectories {
    return @(
        Get-ChildItem ([IO.Path]::GetTempPath()) -Directory -Filter "test-lens-docs-v0.2.0-*" -ErrorAction SilentlyContinue |
            Sort-Object FullName |
            ForEach-Object { $_.FullName }
    )
}
try {
    New-Item -ItemType Directory -Path $work | Out-Null
    $repo = Join-Path $work "repo"
    $remote = Join-Path $work "remote.git"
    New-Item -ItemType Directory -Path $repo | Out-Null
    foreach ($directory in @("docs", "docs-versions", "docs-versioning", "overrides", "scripts")) {
        Copy-Item -LiteralPath (Join-Path $root $directory) -Destination $repo -Recurse
    }
    New-Item -ItemType Directory -Path (Join-Path $repo "docs-hooks") | Out-Null
    Copy-Item -LiteralPath (Join-Path $root "docs-hooks/copy-hud-demo-runtime.py") -Destination (Join-Path $repo "docs-hooks")
    $runtimeSource = Join-Path $root "selenium-test-lens-overlay/src/main/resources/uitestlens/runtime"
    $runtimeRepositoryPath = "selenium-test-lens-overlay/src/main/resources/uitestlens/runtime"
    $stableRuntimeArchive = Join-Path $work "stable-v0.2.0-runtime.tar"
    & git -C $root archive v0.2.0 -o $stableRuntimeArchive -- $runtimeRepositoryPath
    if ($LASTEXITCODE -ne 0) { throw "Unable to extract the v0.2.0 HUD renderer for version-boundary validation." }
    & tar -xf $stableRuntimeArchive -C $work
    if ($LASTEXITCODE -ne 0) { throw "Unable to unpack the v0.2.0 HUD renderer fixture." }
    $stableRuntimeSource = Join-Path $work $runtimeRepositoryPath
    $runtimeFixture = Join-Path $repo "selenium-test-lens-overlay/src/main/resources/uitestlens/runtime"
    New-Item -ItemType Directory -Path $runtimeFixture -Force | Out-Null
    foreach ($runtimeFile in @("hud-panel.js", "highlight.js", "scroll-arrow.js")) {
        Copy-Item -LiteralPath (Join-Path $stableRuntimeSource $runtimeFile) -Destination (Join-Path $runtimeFixture $runtimeFile)
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
        $repoHomepage = Join-Path $repo "docs/index.md"
        $repairedHomepageSource = [IO.File]::ReadAllText($repoHomepage) + "`n<!-- validation-only stable homepage repair -->`n"
        $demoFixture = Join-Path $work "current-hud-demo"
        Copy-Item -LiteralPath (Join-Path $repo "docs/demo/hud") -Destination $demoFixture -Recurse
        Remove-Item -LiteralPath (Join-Path $repo "docs/demo/hud") -Recurse -Force
        [IO.File]::WriteAllText(
            $repoHomepage,
            "---`nhide:`n  - navigation`n  - toc`n---`n`n# Original 0.2.0 homepage`n",
            [Text.UTF8Encoding]::new($false)
        )
        & git add .
        & git commit -q -m release-source
        & git tag v0.2.0
        foreach ($runtimeFile in @("hud-panel.js", "highlight.js", "scroll-arrow.js")) {
            Copy-Item -LiteralPath (Join-Path $runtimeSource $runtimeFile) -Destination (Join-Path $runtimeFixture $runtimeFile) -Force
        }
        [IO.File]::WriteAllText($repoHomepage, $repairedHomepageSource, [Text.UTF8Encoding]::new($false))
        New-Item -ItemType Directory -Path (Join-Path $repo "docs/demo") -Force | Out-Null
        Copy-Item -LiteralPath $demoFixture -Destination (Join-Path $repo "docs/demo/hud") -Recurse
        & git add docs/index.md
        & git add docs/demo/hud
        & git add $runtimeRepositoryPath
        & git commit -q -m development-source
        & git init -q --bare $remote
        & git remote add origin $remote
        & git push -q -u origin HEAD:main

        # Exercise the no-push path independently, including the title with spaces.
        & ./scripts/publish-versioned-docs.ps1 -Operation dev -Version $developmentVersion -Branch gh-pages-no-push -NoPush
        $noPushMetadata = (& git show gh-pages-no-push`:versions.json) -join "`n" | ConvertFrom-Json
        $noPushDev = $noPushMetadata | Where-Object version -eq "dev"
        if ($null -eq $noPushDev -or $noPushDev.title -ne $developmentTitle) {
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

        $env:DOCS_RELEASE_VERSION = "0.2.0"
        $env:DOCS_RELEASE_EDIT_URI = "edit/v0.2.0/docs/"
        $releaseWorktree = Join-Path $work "initial-v0.2.0"
        & git worktree add --detach $releaseWorktree v0.2.0
        try {
            & ./scripts/publish-versioned-docs.ps1 -Operation release -Version 0.2.0 -SourceRoot $releaseWorktree
        } finally {
            & git worktree remove --force $releaseWorktree
        }
        $originalDemoEntry = @(& git ls-tree --name-only gh-pages -- "0.2.0/demo/hud/index.html")
        if ($LASTEXITCODE -ne 0) { throw "Unable to inspect the synthetic original 0.2.0 publication." }
        if ($originalDemoEntry.Count -ne 0) { throw "The synthetic original 0.2.0 publication unexpectedly contained the new HUD demo." }
        $devBeforeRepair = (& git rev-parse gh-pages`:dev).Trim()
        $historicalBeforeRepair = (& git rev-parse gh-pages`:0.1.0).Trim()
        $rootBeforeRepair = (& git rev-parse gh-pages`:index.html).Trim()

        $badRepairConfirmationRejected = $false
        try {
            & ./scripts/repair-0.2.0-homepage.ps1 -Confirmation wrong -NoPush
        } catch {
            $badRepairConfirmationRejected = $true
        }
        if (-not $badRepairConfirmationRejected) { throw "The 0.2.0 homepage repair accepted an invalid confirmation." }

        $worktreesBeforeFailure = (@(& git worktree list --porcelain) -join "`n")
        $repairDirectoriesBeforeFailure = (RepairTempDirectories) -join "`n"
        $failedRepairRejected = $false
        try {
            & ./scripts/repair-0.2.0-homepage.ps1 -Confirmation repair-stable-0.2.0-homepage -Branch missing-pages -NoPush
        } catch {
            $failedRepairRejected = $true
        }
        if (-not $failedRepairRejected) { throw "The repair against a missing publication branch unexpectedly succeeded." }
        $worktreesAfterFailure = (@(& git worktree list --porcelain) -join "`n")
        if ($worktreesAfterFailure -ne $worktreesBeforeFailure) { throw "A failed repair left a detached worktree registered." }
        if (((RepairTempDirectories) -join "`n") -ne $repairDirectoriesBeforeFailure) { throw "A failed repair left a temporary worktree or preview directory on disk." }

        $worktreesBeforeRepair = (@(& git worktree list --porcelain) -join "`n")
        $repairDirectoriesBeforeSuccess = (RepairTempDirectories) -join "`n"
        & ./scripts/repair-0.2.0-homepage.ps1 -Confirmation repair-stable-0.2.0-homepage
        $worktreesAfterRepair = (@(& git worktree list --porcelain) -join "`n")
        if ($worktreesAfterRepair -ne $worktreesBeforeRepair) { throw "A successful repair left a detached worktree registered." }
        if (((RepairTempDirectories) -join "`n") -ne $repairDirectoriesBeforeSuccess) { throw "A successful repair left a temporary worktree or preview directory on disk." }
        if ((& git rev-parse gh-pages`:dev).Trim() -ne $devBeforeRepair) { throw "The stable homepage repair changed dev." }
        if ((& git rev-parse gh-pages`:0.1.0).Trim() -ne $historicalBeforeRepair) { throw "The stable homepage repair changed 0.1.0." }
        if ((& git rev-parse gh-pages`:index.html).Trim() -ne $rootBeforeRepair) { throw "The stable homepage repair changed the root redirect." }
        $repairedHome = ((@(& git show gh-pages`:0.2.0/index.html)) -join "`n")
        if (-not $repairedHome.Contains("validation-only stable homepage repair")) { throw "The stable 0.2.0 homepage was not updated." }
        if ($repairedHome.Contains("edit/main/docs/index.md")) { throw "The repaired stable homepage exposes a misleading edit link to main." }

        & ./scripts/publish-versioned-docs.ps1 -Operation dev -Version $developmentVersion
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
        & ./scripts/publish-versioned-docs.ps1 -Operation release -Version $futureReleaseVersion
        $stage3 = Join-Path $work "site-three"
        New-Item -ItemType Directory -Path $stage3 | Out-Null
        & git archive gh-pages -o (Join-Path $work "pages-three.tar")
        & tar -xf (Join-Path $work "pages-three.tar") -C $stage3

        $requiredVersionedOutputs = @(
            "0.1.0/index.html", "0.1.0/search/search_index.json",
            "0.2.0/index.html", "0.2.0/search/search_index.json",
            "dev/index.html", "dev/search/search_index.json",
            "latest/index.html", "index.html", "versions.json"
        )
        foreach ($versionDirectory in @("0.2.0", "dev", "latest")) {
            foreach ($demoFile in @(
                "index.html", "demo.css", "demo.js",
                "runtime/hud-panel.js", "runtime/highlight.js", "runtime/scroll-arrow.js"
            )) {
                $requiredVersionedOutputs += "$versionDirectory/demo/hud/$demoFile"
            }
        }
        foreach ($required in $requiredVersionedOutputs) {
            if (-not (Test-Path (Join-Path $stage2 $required))) { throw "Missing versioned output: $required" }
        }
        foreach ($versionDirectory in @("0.1.0", "0.2.0", "dev")) {
            $assets = Join-Path $stage2 "$versionDirectory/assets"
            if (-not (Get-ChildItem $assets -Filter *.css -File -Recurse)) { throw "No CSS assets published for $versionDirectory." }
            if (-not (Get-ChildItem $assets -Filter *.js -File -Recurse)) { throw "No JavaScript assets published for $versionDirectory." }
        }
        $banner = if ($sourceIsSnapshot) { "Development documentation for" } else { "Documentation source for" }
        foreach ($html in Get-ChildItem (Join-Path $stage2 "dev") -Filter *.html -File -Recurse) {
            if ($html.FullName.Replace('\','/').Contains('/demo/')) { continue }
            $text = [IO.File]::ReadAllText($html.FullName)
            if (-not $text.Contains($banner)) { throw "Development banner missing: $($html.FullName)" }
            if (-not $text.Contains('class="tl-version-switcher"')) { throw "Version switcher missing from dev: $($html.FullName)" }
        }
        $devHome = [IO.File]::ReadAllText((Join-Path $stage2 "dev/index.html"))
        if (-not $devHome.Contains("edit/main/docs/index.md")) { throw "dev homepage edit link does not target main." }
        foreach ($versionDirectory in @("dev", "0.2.0", "latest")) {
            $versionHome = [IO.File]::ReadAllText((Join-Path $stage2 "$versionDirectory/index.html"))
            if (-not $versionHome.Contains('src="demo/hud/"')) { throw "Homepage iframe is not relative under $versionDirectory." }
            $demoHtml = [IO.File]::ReadAllText((Join-Path $stage2 "$versionDirectory/demo/hud/index.html"))
            foreach ($asset in @("demo.css", "demo.js", "runtime/hud-panel.js", "runtime/highlight.js", "runtime/scroll-arrow.js")) {
                if (-not $demoHtml.Contains($asset)) { throw "HUD demo under $versionDirectory does not reference relative asset $asset." }
            }
        }
        foreach ($runtimeFile in @("hud-panel.js", "highlight.js", "scroll-arrow.js")) {
            $developmentHash = (Get-FileHash (Join-Path $runtimeFixture $runtimeFile) -Algorithm SHA256).Hash
            $stableRuntimeHash = (Get-FileHash (Join-Path $stableRuntimeSource $runtimeFile) -Algorithm SHA256).Hash
            $publishedDevHash = (Get-FileHash (Join-Path $stage2 "dev/demo/hud/runtime/$runtimeFile") -Algorithm SHA256).Hash
            if ($publishedDevHash -ne $developmentHash) { throw "HUD demo runtime drift for dev/$runtimeFile." }
            foreach ($versionDirectory in @("0.2.0", "latest")) {
                $publishedHash = (Get-FileHash (Join-Path $stage2 "$versionDirectory/demo/hud/runtime/$runtimeFile") -Algorithm SHA256).Hash
                if ($publishedHash -ne $stableRuntimeHash) { throw "HUD demo runtime drift for $versionDirectory/$runtimeFile." }
            }
        }
        $demoCss = [IO.File]::ReadAllText((Join-Path $stage2 "dev/demo/hud/demo.css"))
        $demoJs = [IO.File]::ReadAllText((Join-Path $stage2 "dev/demo/hud/demo.js"))
        if (-not $demoCss.Contains("prefers-reduced-motion") -or -not $demoJs.Contains("prefers-reduced-motion")) {
            throw "HUD demo does not preserve reduced-motion behavior."
        }
        $devGuide = [IO.File]::ReadAllText((Join-Path $stage2 "dev/getting-started/index.html"))
        if (-not $devGuide.Contains("edit/main/docs/getting-started.md")) { throw "dev edit link does not target main." }
        foreach ($stableVersion in @("0.1.0", "0.2.0")) {
            foreach ($html in Get-ChildItem (Join-Path $stage2 $stableVersion) -Filter *.html -File -Recurse) {
                if ($html.FullName.Replace('\','/').Contains('/demo/')) { continue }
                $text = [IO.File]::ReadAllText($html.FullName)
                if ($text.Contains($banner)) { throw "Development banner leaked into stable docs." }
                if ($text.Contains("edit/main/docs/")) { throw "Stable edit link points to main." }
                if (-not $text.Contains('class="tl-version-switcher"')) { throw "Version switcher missing from stable docs." }
            }
        }
        $versions = Get-Content (Join-Path $stage2 "versions.json") -Raw | ConvertFrom-Json
        if (-not (($versions.version -contains "0.1.0") -and ($versions.version -contains "0.2.0") -and ($versions.version -contains "dev"))) {
            throw "mike metadata lacks historical, stable, or dev versions."
        }
        $stableVersion = $versions | Where-Object version -eq "0.2.0"
        if ($null -eq $stableVersion -or $stableVersion.aliases -notcontains "latest") { throw "latest does not point to 0.2.0 after repair." }
        $devVersion = $versions | Where-Object version -eq "dev"
        if ($null -eq $devVersion -or $devVersion.title -ne $developmentTitle) {
            throw "Pushed dev metadata did not preserve the exact title argument."
        }
        $future = Get-Content (Join-Path $stage3 "versions.json") -Raw | ConvertFrom-Json
        $futureRelease = $future | Where-Object version -eq $futureReleaseVersion
        if ($null -eq $futureRelease -or $futureRelease.aliases -notcontains "latest") { throw "Future release did not move latest." }
        if (-not (Test-Path (Join-Path $stage3 "0.1.0/index.html"))) { throw "Future release removed 0.1.0." }
        $futureGuide = [IO.File]::ReadAllText((Join-Path $stage3 "$futureReleaseVersion/getting-started/index.html"))
        if (-not $futureGuide.Contains("edit/v$futureReleaseVersion/docs/getting-started.md")) { throw "Tagged release edit link does not target its tag." }
        $rootRedirect = [IO.File]::ReadAllText((Join-Path $stage2 "index.html"))
        if (-not $rootRedirect.Contains('url=latest/')) { throw "Root default does not redirect to latest." }
        $latestHome = [IO.File]::ReadAllText((Join-Path $stage2 "latest/index.html"))
        if (-not $latestHome.Contains("validation-only stable homepage repair")) { throw "latest does not serve the repaired stable 0.2.0 homepage." }
        Write-Host "Versioned docs simulation OK: production repair worktree cleaned, 0.2.0 homepage repaired, dev/history/root preserved, future latest advanced."
        $ok = $true
    } finally { Pop-Location }
} finally {
    if ($ok -or -not $KeepWorkDirectoryOnFailure) { Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue }
    else { Write-Warning "Validation work directory retained: $work" }
}
