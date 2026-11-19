param([string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot))
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'ReleaseReactor.psm1') -Force
$policyPath = Join-Path $PSScriptRoot 'config/publication-policy.json'
$policy = Read-TestLensPublicationPolicy $policyPath
if ($policy.LatestReleasedVersion -ne '0.5.1' -or $policy.PreparedReleaseVersion -ne '0.5.2') {
    throw 'Preparation must preserve the actual released version and explicitly declare the candidate'
}
foreach ($version in @('0.5.1', '0.5.2', '0.5.2-SNAPSHOT')) {
    Assert-TestLensDevelopmentVersion -RepositoryRoot $RepositoryRoot -Version $version -PublicationPolicyPath $policyPath
}
try {
    Assert-TestLensDevelopmentVersion -RepositoryRoot $RepositoryRoot -Version '0.5.3' -PublicationPolicyPath $policyPath
    throw 'Expected an undeclared candidate to fail'
} catch {
    if ($_.Exception.Message -notlike '*must equal publication policy*') { throw }
}
$testRoot = Join-Path $RepositoryRoot ('target/prepared-policy-test/' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot -Force | Out-Null
foreach ($invalid in @('0.5.1', '0.5.0', '0.5.2-SNAPSHOT', 'invalid')) {
    $raw = Get-Content -LiteralPath $policyPath -Raw | ConvertFrom-Json
    $raw.preparedReleaseVersion = $invalid
    $fixture = Join-Path $testRoot 'policy.json'
    $raw | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $fixture
    try {
        Read-TestLensPublicationPolicy $fixture | Out-Null
        throw 'Expected invalid prepared release policy to fail'
    } catch {
        if ($_.Exception.Message -notlike '*preparedReleaseVersion must be an exact version newer*') { throw }
    }
}
Write-Output 'Prepared release policy PASS: candidate allowed explicitly; released version unchanged; undeclared/invalid versions rejected'
