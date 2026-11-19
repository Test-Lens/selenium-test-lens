param([string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot))
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'ReleaseReactor.psm1') -Force
$policyPath = Join-Path $PSScriptRoot 'config/publication-policy.json'
$policy = Read-TestLensPublicationPolicy $policyPath
Get-TestLensReactorModel -RepositoryRoot $RepositoryRoot -IncludeBrowserIt -PublicationPolicyPath $policyPath | Out-Null

$testRoot = Join-Path $RepositoryRoot ('target/prepared-policy-test/' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot -Force | Out-Null
try {
    # Historical policy scenarios must not inherit real release tags or live metadata.
    & git -C $testRoot init -q
    if ($LASTEXITCODE -ne 0) { throw 'Cannot initialize prepared-policy fixture' }
    if (@(& git -C $testRoot tag --list).Count -ne 0) { throw 'Prepared-policy fixture must have no release tags' }
    $preparedPath = Join-Path $testRoot 'prepared-policy.json'
    $releasedPath = Join-Path $testRoot 'released-policy.json'
    $raw = [IO.File]::ReadAllText($policyPath) | ConvertFrom-Json
    $raw.latestReleasedVersion = '0.5.1'
    $raw | Add-Member -NotePropertyName preparedReleaseVersion -NotePropertyValue '0.5.2' -Force
    [IO.File]::WriteAllText($preparedPath, ($raw | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
    $prepared = Read-TestLensPublicationPolicy $preparedPath
    if ($prepared.LatestReleasedVersion -ne '0.5.1' -or $prepared.PreparedReleaseVersion -ne '0.5.2') {
        throw 'Prepared fixture must preserve the released version and explicitly declare the candidate'
    }
    foreach ($version in @('0.5.1', '0.5.2', '0.5.2-SNAPSHOT')) {
        Assert-TestLensDevelopmentVersion -RepositoryRoot $testRoot -Version $version -PublicationPolicyPath $preparedPath
    }
    try {
        Assert-TestLensDevelopmentVersion -RepositoryRoot $testRoot -Version '0.5.3' -PublicationPolicyPath $preparedPath
        throw 'Expected an undeclared candidate to fail'
    } catch {
        if ($_.Exception.Message -notlike '*must equal publication policy*') { throw }
    }
    foreach ($invalid in @('0.5.1', '0.5.0', '0.5.2-SNAPSHOT', 'invalid')) {
        $raw.preparedReleaseVersion = $invalid
        $fixture = Join-Path $testRoot 'invalid-policy.json'
        [IO.File]::WriteAllText($fixture, ($raw | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
        try {
            Read-TestLensPublicationPolicy $fixture | Out-Null
            throw 'Expected invalid prepared release policy to fail'
        } catch {
            if ($_.Exception.Message -notlike '*preparedReleaseVersion must be an exact version newer*') { throw }
        }
    }
    $raw.latestReleasedVersion = '0.5.2'
    $raw.PSObject.Properties.Remove('preparedReleaseVersion')
    [IO.File]::WriteAllText($releasedPath, ($raw | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
    $released = Read-TestLensPublicationPolicy $releasedPath
    if ($released.LatestReleasedVersion -ne '0.5.2' -or $released.PreparedReleaseVersion -ne '') {
        throw 'Released fixture must have no prepared candidate'
    }
    foreach ($version in @('0.5.2', '0.5.3-SNAPSHOT')) {
        Assert-TestLensDevelopmentVersion -RepositoryRoot $testRoot -Version $version -PublicationPolicyPath $releasedPath
    }
    foreach ($version in @('0.5.1', '0.5.3')) {
        try {
            Assert-TestLensDevelopmentVersion -RepositoryRoot $testRoot -Version $version -PublicationPolicyPath $releasedPath
            throw 'Expected an undeclared release to fail'
        } catch {
            if ($_.Exception.Message -notlike '*must equal publication policy*') { throw }
        }
    }
    try {
        Assert-TestLensDevelopmentVersion -RepositoryRoot $testRoot -Version '0.5.2-SNAPSHOT' -PublicationPolicyPath $releasedPath
        throw 'Expected a snapshot of the released version to fail'
    } catch {
        if ($_.Exception.Message -notlike "*must be newer than declared released version '0.5.2'*") { throw }
    }
    Write-Output 'Prepared release policy PASS: live reactor validated; isolated prepared/released policies allow declared versions and reject undeclared/invalid versions'
} finally {
    $fixturePath = [IO.Path]::GetFullPath($testRoot)
    $fixtureParent = [IO.Path]::GetFullPath((Join-Path $RepositoryRoot 'target/prepared-policy-test')) + [IO.Path]::DirectorySeparatorChar
    if (-not $fixturePath.StartsWith($fixtureParent, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe prepared-policy fixture cleanup path' }
    if (Test-Path -LiteralPath $fixturePath) { Remove-Item -LiteralPath $fixturePath -Recurse -Force }
}