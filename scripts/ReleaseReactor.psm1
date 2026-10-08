Set-StrictMode -Version 2.0

function Read-TestLensPom([string]$Path) {
    try { return [xml][IO.File]::ReadAllText($Path) }
    catch { throw "Cannot read Maven project '$Path': $($_.Exception.Message)" }
}

function Get-TestLensPomText([xml]$Pom, [string]$XPath) {
    $namespace = New-Object System.Xml.XmlNamespaceManager($Pom.NameTable)
    $namespace.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
    $node = $Pom.SelectSingleNode($XPath, $namespace)
    if ($null -eq $node) { return "" }
    return $node.InnerText.Trim()
}

function Get-TestLensPomTexts([xml]$Pom, [string]$XPath) {
    $namespace = New-Object System.Xml.XmlNamespaceManager($Pom.NameTable)
    $namespace.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
    return @($Pom.SelectNodes($XPath, $namespace) | ForEach-Object { $_.InnerText.Trim() })
}

function Read-TestLensPublicationPolicy([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Publication policy does not exist: $Path"
    }
    try { $policy = [IO.File]::ReadAllText($Path) | ConvertFrom-Json }
    catch { throw "Cannot read publication policy '$Path': $($_.Exception.Message)" }
    if ($policy.schemaVersion -ne 2) {
        throw "Unsupported publication policy schemaVersion '$($policy.schemaVersion)'"
    }
    $latestReleasedVersion = $policy.latestReleasedVersion.ToString().Trim()
    if ($latestReleasedVersion -notmatch '^\d+\.\d+\.\d+$') {
        throw "Publication policy latestReleasedVersion '$latestReleasedVersion' is not an exact semantic version"
    }
    $allowedRoles = @("PUBLISHED_STABLE", "PUBLISHED_TOOLING", "INTERNAL", "TEST_ONLY", "DEMO", "FUTURE")
    $declaredRoles = @($policy.roles.PSObject.Properties.Name)
    $unknownRoles = @($declaredRoles | Where-Object { $_ -notin $allowedRoles })
    $missingRoles = @($allowedRoles | Where-Object { $_ -notin $declaredRoles })
    if ($unknownRoles.Count -gt 0 -or $missingRoles.Count -gt 0) {
        throw "Publication policy roles are invalid. Unknown: $($unknownRoles -join ', '); missing: $($missingRoles -join ', ')"
    }
    $artifactsByRole = [ordered]@{}
    foreach ($role in $allowedRoles) {
        $artifactsByRole[$role] = @($policy.roles.$role | ForEach-Object { $_.ToString().Trim() } | Where-Object { $_ })
    }
    $published = @($artifactsByRole.PUBLISHED_STABLE + $artifactsByRole.PUBLISHED_TOOLING)
    $nonpublished = @($artifactsByRole.INTERNAL + $artifactsByRole.TEST_ONLY + $artifactsByRole.DEMO + $artifactsByRole.FUTURE)
    $classified = @($published + $nonpublished)
    $duplicates = @($classified | Group-Object | Where-Object Count -gt 1 | ForEach-Object Name)
    if ($duplicates.Count -gt 0) {
        throw "Publication policy classifies artifacts more than once: $($duplicates -join ', ')"
    }
    $nonReactorPomPaths = @($policy.nonReactorPomPaths | ForEach-Object { $_.ToString().Trim().Replace('\','/') } | Where-Object { $_ })
    foreach ($path in $nonReactorPomPaths) {
        if ($path -notmatch '^[^/].*/pom\.xml$' -or $path.Contains('../') -or [IO.Path]::IsPathRooted($path)) {
            throw "Publication policy nonReactorPomPaths contains an unsafe path: $path"
        }
    }
    $duplicatePomPaths = @($nonReactorPomPaths | Group-Object | Where-Object Count -gt 1 | ForEach-Object Name)
    if ($duplicatePomPaths.Count -gt 0) {
        throw "Publication policy classifies non-reactor POM paths more than once: $($duplicatePomPaths -join ', ')"
    }
    return [pscustomobject]@{
        Published = @($published)
        Nonpublished = @($nonpublished)
        PublishedStable = @($artifactsByRole.PUBLISHED_STABLE)
        PublishedTooling = @($artifactsByRole.PUBLISHED_TOOLING)
        Internal = @($artifactsByRole.INTERNAL)
        TestOnly = @($artifactsByRole.TEST_ONLY)
        Demo = @($artifactsByRole.DEMO)
        Future = @($artifactsByRole.FUTURE)
        ArtifactsByRole = $artifactsByRole
        LatestReleasedVersion = $latestReleasedVersion
        Classified = @($classified)
        CentralExcluded = @($nonpublished | Sort-Object -Unique)
        NonReactorPomPaths = @($nonReactorPomPaths)
    }
}

function Assert-TestLensDevelopmentVersion {
    param(
        [Parameter(Mandatory = $true)][string]$RepositoryRoot,
        [Parameter(Mandatory = $true)][string]$Version,
        [string]$PublicationPolicyPath = (Join-Path $PSScriptRoot "config/publication-policy.json")
    )
    if ($Version -notmatch '^(\d+)\.(\d+)\.(\d+)(-SNAPSHOT)?$') {
        throw "Development version '$Version' is not a supported semantic Maven version"
    }
    $developmentBase = [version]::new([int]$Matches[1], [int]$Matches[2], [int]$Matches[3])
    $isSnapshot = -not [string]::IsNullOrWhiteSpace($Matches[4])
    $policy = Read-TestLensPublicationPolicy $PublicationPolicyPath
    $declaredRelease = [version]$policy.LatestReleasedVersion
    $tagOutput = @(& git -C $RepositoryRoot tag --list "v*.*.*" 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "Cannot read local release tags: $($tagOutput -join ' ')"
    }
    $releases = @($tagOutput | ForEach-Object {
        $tag = $_.ToString().Trim()
        if ($tag -match '^v(\d+)\.(\d+)\.(\d+)$') {
            [pscustomobject]@{ Tag = $tag; Version = [version]::new([int]$Matches[1], [int]$Matches[2], [int]$Matches[3]) }
        }
    } | Where-Object { $null -ne $_ } | Sort-Object Version -Descending)
    $latestRelease = $declaredRelease
    $latestLabel = "declared released version '$($policy.LatestReleasedVersion)'"
    if ($releases.Count -gt 0 -and $releases[0].Version -gt $declaredRelease) {
        throw "Publication policy latestReleasedVersion '$($policy.LatestReleasedVersion)' is older than local release tag '$($releases[0].Tag)'"
    }
    if ($releases.Count -gt 0 -and $releases[0].Version -eq $declaredRelease) {
        $latestLabel = "latest local release tag '$($releases[0].Tag)'"
    }
    if ($isSnapshot) {
        if ($developmentBase -le $latestRelease) {
            throw "Development version '$Version' must be newer than $latestLabel"
        }
    } elseif ($developmentBase -ne $declaredRelease) {
        throw "Release version '$Version' must equal publication policy latestReleasedVersion '$($policy.LatestReleasedVersion)'"
    }
}

function Get-TestLensReactorModel {
    param(
        [Parameter(Mandatory = $true)][string]$RepositoryRoot,
        [string]$ReleaseSourceRoot,
        [switch]$IncludeBrowserIt,
        [string]$PublicationPolicyPath = (Join-Path $PSScriptRoot "config/publication-policy.json")
    )
    if ([string]::IsNullOrWhiteSpace($RepositoryRoot)) {
        throw "RepositoryRoot must identify the source Git worktree"
    }
    $repository = [IO.Path]::GetFullPath($RepositoryRoot)
    if (-not (Test-Path -LiteralPath $repository -PathType Container)) {
        throw "RepositoryRoot does not exist: $repository"
    }
    $repositoryPom = Join-Path $repository "pom.xml"
    if (-not (Test-Path -LiteralPath $repositoryPom -PathType Leaf)) {
        throw "RepositoryRoot does not contain root pom.xml: $repositoryPom"
    }
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        # Windows PowerShell promotes native stderr to ErrorRecord when the
        # caller uses Stop. Capture Git's diagnostic so this boundary can
        # expose the stable release-tooling error below on every host.
        $ErrorActionPreference = "Continue"
        $gitRootOutput = @(& git -C $repository rev-parse --show-toplevel 2>&1)
        $gitRootExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($gitRootExitCode -ne 0) {
        throw "RepositoryRoot is not a Git worktree: $repository ($($gitRootOutput -join ' '))"
    }
    $gitRoot = [IO.Path]::GetFullPath(($gitRootOutput | Select-Object -Last 1).ToString()).TrimEnd(
        [IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar)
    $normalizedRepository = $repository.TrimEnd(
        [IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar)
    if (-not $gitRoot.Equals($normalizedRepository, [StringComparison]::OrdinalIgnoreCase)) {
        throw "RepositoryRoot must be the Git worktree root '$gitRoot', but was '$repository'"
    }

    $root = if ([string]::IsNullOrWhiteSpace($ReleaseSourceRoot)) {
        $repository
    } else {
        [IO.Path]::GetFullPath($ReleaseSourceRoot)
    }
    if (-not (Test-Path -LiteralPath $root -PathType Container)) {
        throw "ReleaseSourceRoot does not exist: $root"
    }
    $rootPomPath = Join-Path $root "pom.xml"
    if (-not (Test-Path -LiteralPath $rootPomPath -PathType Leaf)) {
        throw "ReleaseSourceRoot does not contain root pom.xml: $rootPomPath"
    }
    $rootPom = Read-TestLensPom $rootPomPath
    $normalModules = @(Get-TestLensPomTexts $rootPom "/m:project/m:modules/m:module")
    $profileModules = @(Get-TestLensPomTexts $rootPom "/m:project/m:profiles/m:profile/m:modules/m:module")
    $browserModules = @(Get-TestLensPomTexts $rootPom "/m:project/m:profiles/m:profile[m:id='browser-it']/m:modules/m:module")
    $knownModules = @($normalModules + $profileModules | Sort-Object -Unique)
    $selectedModules = @($normalModules)
    if ($IncludeBrowserIt) { $selectedModules = @($selectedModules + $browserModules | Sort-Object -Unique) }

    $trackedOutput = @(& git -C $repository ls-files -- "pom.xml" ":(glob)**/pom.xml" 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "git ls-files failed while enumerating reactor POMs from RepositoryRoot '$repository': $($trackedOutput -join ' ')"
    }
    $tracked = @($trackedOutput | ForEach-Object { $_.ToString().Replace('\','/') } | Sort-Object -Unique)
    $expectedKnown = @("pom.xml") + @($knownModules | ForEach-Object { $_.TrimEnd('/','\').Replace('\','/') + "/pom.xml" })
    $expectedKnown = @($expectedKnown | Sort-Object -Unique)
    $publicationPolicy = Read-TestLensPublicationPolicy $PublicationPolicyPath
    $nonReactorPomPaths = @($publicationPolicy.NonReactorPomPaths)
    $missingNonReactorPoms = @($nonReactorPomPaths | Where-Object { $_ -notin $tracked })
    if ($missingNonReactorPoms.Count -gt 0) {
        throw "Publication policy non-reactor POM paths are missing or untracked: $($missingNonReactorPoms -join ', ')"
    }
    $missing = @($expectedKnown | Where-Object { $_ -notin $tracked })
    $unexpected = @($tracked | Where-Object { $_ -notin $expectedKnown -and $_ -notin $nonReactorPomPaths })
    if ($missing.Count -gt 0) { throw "Reactor POMs are missing or untracked: $($missing -join ', ')" }
    if ($unexpected.Count -gt 0) { throw "Tracked POMs are not classified by the root Maven module graph: $($unexpected -join ', ')" }

    $selected = @("pom.xml") + @($selectedModules | ForEach-Object { $_.TrimEnd('/','\').Replace('\','/') + "/pom.xml" })
    $rootVersion = Get-TestLensPomText $rootPom "/m:project/m:version"
    Assert-TestLensDevelopmentVersion -RepositoryRoot $repository -Version $rootVersion -PublicationPolicyPath $PublicationPolicyPath
    $rootDeploySkip = Get-TestLensPomText $rootPom "/m:project/m:properties/m:maven.deploy.skip"
    $projects = foreach ($relative in @($selected | Sort-Object -Unique)) {
        $full = Join-Path $root ($relative.Replace('/', [IO.Path]::DirectorySeparatorChar))
        if (-not [IO.File]::Exists($full)) { throw "Reactor module POM is missing: $relative" }
        $pom = Read-TestLensPom $full
        $declared = Get-TestLensPomText $pom "/m:project/m:version"
        $parent = Get-TestLensPomText $pom "/m:project/m:parent/m:version"
        $version = if ([string]::IsNullOrWhiteSpace($declared)) { $parent } else { $declared }
        $packaging = Get-TestLensPomText $pom "/m:project/m:packaging"
        if ([string]::IsNullOrWhiteSpace($packaging)) { $packaging = "jar" }
        $localSkip = Get-TestLensPomText $pom "/m:project/m:properties/m:maven.deploy.skip"
        $effectiveSkip = if (-not [string]::IsNullOrWhiteSpace($localSkip)) { $localSkip } else { $rootDeploySkip }
        $modulePath = if ($relative -eq "pom.xml") { "" } else { $relative.Substring(0, $relative.Length - 8).TrimEnd('/') }
        [pscustomobject]@{
            RelativePom = $relative; Directory = Split-Path -Parent $full
            ArtifactId = Get-TestLensPomText $pom "/m:project/m:artifactId"
            Packaging = $packaging; Version = $version; ParentVersion = $parent
            EffectiveDeploySkip = ($effectiveSkip -eq "true")
            ProfileOnly = ($relative -ne "pom.xml" -and $modulePath -in $profileModules)
        }
    }
    return [pscustomobject]@{
        RepositoryRoot = $repository; ReleaseSourceRoot = $root
        RootVersion = $rootVersion; Projects = @($projects)
        NormalProjectCount = 1 + $normalModules.Count
        BrowserProjectCount = 1 + (@($normalModules + $browserModules | Sort-Object -Unique)).Count
        TrackedPomPaths = $tracked
    }
}

Export-ModuleMember -Function Read-TestLensPom, Get-TestLensPomText, Get-TestLensPomTexts, Read-TestLensPublicationPolicy, Assert-TestLensDevelopmentVersion, Get-TestLensReactorModel
