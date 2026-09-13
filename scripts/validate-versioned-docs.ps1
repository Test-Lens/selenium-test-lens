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
$compatibilityRedirects = [ordered]@{
    "integrations/react/index.html" = [pscustomobject]@{
        Target = "../../features/react-spa/"
        HistoricalPageSource = "features/react-spa.md"
    }
}
function TreeHash([string]$Path) {
    $items = Get-ChildItem $Path -File -Recurse | Sort-Object FullName | ForEach-Object {
        $rel = $_.FullName.Substring($Path.Length).Replace('\','/')
        "$rel=$((Get-FileHash $_.FullName -Algorithm SHA256).Hash)"
    }
    $bytes = [Text.Encoding]::UTF8.GetBytes(($items -join "`n"))
    $sha = [Security.Cryptography.SHA256]::Create()
    return ([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace("-", "")
}
function Test-IsCompatibilityRedirectPage([string]$RelativePath) {
    $normalizedRelativePath = $RelativePath.Replace('\','/')
    return $compatibilityRedirects.Contains($normalizedRelativePath) -and
        (Test-Path -LiteralPath (Join-Path $root "docs/$normalizedRelativePath") -PathType Leaf)
}
function Assert-CompatibilityRedirectPage(
    [string]$SiteRoot,
    [string]$VersionDirectory,
    [string]$RelativePath,
    [string]$ExpectedTarget
) {
    $normalizedRelativePath = $RelativePath.Replace('\','/')
    if (-not (Test-IsCompatibilityRedirectPage $normalizedRelativePath)) {
        throw "Unregistered compatibility redirect: $normalizedRelativePath"
    }

    $sourcePath = Join-Path $root "docs/$normalizedRelativePath"
    $publishedPath = Join-Path $SiteRoot "$VersionDirectory/$normalizedRelativePath"
    if (-not (Test-Path -LiteralPath $publishedPath -PathType Leaf)) {
        throw "Compatibility redirect missing: $VersionDirectory/$normalizedRelativePath"
    }
    if ((Get-FileHash $publishedPath -Algorithm SHA256).Hash -ne (Get-FileHash $sourcePath -Algorithm SHA256).Hash) {
        throw "Compatibility redirect was not copied unchanged: $VersionDirectory/$normalizedRelativePath"
    }

    $text = [IO.File]::ReadAllText($publishedPath)
    $requiredFragments = @(
        '<meta name="robots" content="noindex">',
        "<meta http-equiv=`"refresh`" content=`"0; url=$ExpectedTarget`">",
        "<link rel=`"canonical`" href=`"$ExpectedTarget`">",
        "<a href=`"$ExpectedTarget`">"
    )
    foreach ($fragment in $requiredFragments) {
        if (-not $text.Contains($fragment)) {
            throw "Compatibility redirect contract missing '$fragment': $VersionDirectory/$normalizedRelativePath"
        }
    }
    if ([regex]::Matches($text, '<link\s+rel="canonical"\s+href=').Count -ne 1) {
        throw "Compatibility redirect must contain exactly one canonical link: $VersionDirectory/$normalizedRelativePath"
    }
    if ([regex]::Matches($text, '<meta\s+http-equiv="refresh"\s+content=').Count -ne 1) {
        throw "Compatibility redirect must contain exactly one refresh directive: $VersionDirectory/$normalizedRelativePath"
    }

    $baseUri = [Uri]::new("https://docs.example.invalid/$VersionDirectory/$normalizedRelativePath")
    $resolvedPath = [Uri]::new($baseUri, $ExpectedTarget).AbsolutePath
    $expectedVersionLocalPath = "/$VersionDirectory/features/react-spa/"
    if ($resolvedPath -ne $expectedVersionLocalPath) {
        throw "Compatibility redirect escapes its documentation version: $resolvedPath (expected $expectedVersionLocalPath)"
    }
    if (-not (Test-Path -LiteralPath (Join-Path $SiteRoot "$VersionDirectory/features/react-spa/index.html") -PathType Leaf)) {
        throw "Compatibility redirect target missing: $expectedVersionLocalPath"
    }
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
    $runtimeFixture = Join-Path $repo "selenium-test-lens-overlay/src/main/resources/uitestlens/runtime"
    New-Item -ItemType Directory -Path $runtimeFixture -Force | Out-Null
    foreach ($runtimeFile in @("hud-panel.js", "highlight.js", "scroll-arrow.js")) {
        Copy-Item -LiteralPath (Join-Path $runtimeSource $runtimeFile) -Destination (Join-Path $runtimeFixture $runtimeFile)
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
        $developmentRedirectSources = @{}
        foreach ($relativePath in $compatibilityRedirects.Keys) {
            $redirectSource = Join-Path $repo "docs/$relativePath"
            if (-not (Test-Path -LiteralPath $redirectSource -PathType Leaf)) { continue }
            $developmentRedirectSources[$relativePath] = [IO.File]::ReadAllBytes($redirectSource)
            $historicalPage = Join-Path $repo ("docs/" + $relativePath.Replace('/index.html', '.md'))
            Copy-Item -LiteralPath (Join-Path $repo ("docs/" + $compatibilityRedirects[$relativePath].HistoricalPageSource)) -Destination $historicalPage
            Remove-Item -LiteralPath $redirectSource
        }
        & git add .
        & git commit -q -m release-source
        & git tag v0.2.0
        foreach ($relativePath in $developmentRedirectSources.Keys) {
            $historicalPage = Join-Path $repo ("docs/" + $relativePath.Replace('/index.html', '.md'))
            Remove-Item -LiteralPath $historicalPage
            $redirectSource = Join-Path $repo "docs/$relativePath"
            New-Item -ItemType Directory -Path (Split-Path -Parent $redirectSource) -Force | Out-Null
            [IO.File]::WriteAllBytes($redirectSource, $developmentRedirectSources[$relativePath])
        }
        [IO.File]::AppendAllText(
            $repoHomepage,
            "`n<!-- validation-only development source -->`n",
            [Text.UTF8Encoding]::new($false)
        )
        & git add docs
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
        foreach ($studioFile in @(
            "index.html", "studio.css", "studio.js", "preview.html", "preview.css", "preview.js",
            "runtime/hud-panel.js", "runtime/highlight.js", "runtime/scroll-arrow.js"
        )) {
            $requiredVersionedOutputs += "dev/demo/hud-studio/$studioFile"
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
        $activeCompatibilityRedirects = @($compatibilityRedirects.Keys | Where-Object { Test-IsCompatibilityRedirectPage $_ })
        foreach ($relativePath in $activeCompatibilityRedirects) {
            Assert-CompatibilityRedirectPage $stage2 "dev" $relativePath $compatibilityRedirects[$relativePath].Target
        }
        $devRoot = Join-Path $stage2 "dev"
        foreach ($html in Get-ChildItem $devRoot -Filter *.html -File -Recurse) {
            $relativePath = $html.FullName.Substring($devRoot.Length + 1).Replace('\','/')
            if ($relativePath.StartsWith('demo/', [StringComparison]::Ordinal)) { continue }
            if (Test-IsCompatibilityRedirectPage $relativePath) { continue }
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
            $publishedDevHash = (Get-FileHash (Join-Path $stage2 "dev/demo/hud/runtime/$runtimeFile") -Algorithm SHA256).Hash
            if ($publishedDevHash -ne $developmentHash) { throw "HUD demo runtime drift for dev/$runtimeFile." }
            $publishedStudioHash = (Get-FileHash (Join-Path $stage2 "dev/demo/hud-studio/runtime/$runtimeFile") -Algorithm SHA256).Hash
            if ($publishedStudioHash -ne $developmentHash) { throw "HUD Studio runtime drift for dev/$runtimeFile." }
            foreach ($versionDirectory in @("0.2.0", "latest")) {
                $publishedHash = (Get-FileHash (Join-Path $stage2 "$versionDirectory/demo/hud/runtime/$runtimeFile") -Algorithm SHA256).Hash
                if ($publishedHash -ne $developmentHash) { throw "HUD demo runtime drift for $versionDirectory/$runtimeFile." }
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
        if ($null -eq $stableVersion -or $stableVersion.aliases -notcontains "latest") { throw "latest does not point to 0.2.0." }
        $devVersion = $versions | Where-Object version -eq "dev"
        if ($null -eq $devVersion -or $devVersion.title -ne $developmentTitle) {
            throw "Pushed dev metadata did not preserve the exact title argument."
        }
        $future = Get-Content (Join-Path $stage3 "versions.json") -Raw | ConvertFrom-Json
        $futureRelease = $future | Where-Object version -eq $futureReleaseVersion
        if ($null -eq $futureRelease -or $futureRelease.aliases -notcontains "latest") { throw "Future release did not move latest." }
        if (-not (Test-Path (Join-Path $stage3 "0.1.0/index.html"))) { throw "Future release removed 0.1.0." }
        foreach ($relativePath in $activeCompatibilityRedirects) {
            Assert-CompatibilityRedirectPage $stage3 $futureReleaseVersion $relativePath $compatibilityRedirects[$relativePath].Target
        }
        $futureGuide = [IO.File]::ReadAllText((Join-Path $stage3 "$futureReleaseVersion/getting-started/index.html"))
        if (-not $futureGuide.Contains("edit/v$futureReleaseVersion/docs/getting-started.md")) { throw "Tagged release edit link does not target its tag." }
        $rootRedirect = [IO.File]::ReadAllText((Join-Path $stage2 "index.html"))
        if (-not $rootRedirect.Contains('url=latest/')) { throw "Root default does not redirect to latest." }
        if ((TreeHash (Join-Path $stage2 "latest")) -ne (TreeHash (Join-Path $stage2 "0.2.0"))) {
            throw "latest does not serve the published stable 0.2.0 documentation."
        }
        Write-Host "Versioned docs simulation OK: dev/history/root preserved and future latest advanced."
        $ok = $true
    } finally { Pop-Location }
} finally {
    if ($ok -or -not $KeepWorkDirectoryOnFailure) { Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue }
    else { Write-Warning "Validation work directory retained: $work" }
}
