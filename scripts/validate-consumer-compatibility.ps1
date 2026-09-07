param(
    [string]$ReleaseVersion,
    [switch]$KeepWorkDirectoryOnFailure
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
Import-Module (Join-Path $PSScriptRoot "CleanRoomRelease.psm1") -Force

$prepared = $null
$succeeded = $false
try {
    $prepared = New-TestLensCleanRoomRelease `
        -RepositoryRoot $repo `
        -ReleaseVersion $ReleaseVersion

    & (Join-Path $PSScriptRoot "validate-clean-room-consumer.ps1") `
        -ReleaseVersion $prepared.ReleaseVersion `
        -TestLensRepository $prepared.StagingDirectory
    if ($LASTEXITCODE -ne 0) {
        throw "Maven clean-room consumer failed with exit code $LASTEXITCODE."
    }

    & (Join-Path $PSScriptRoot "validate-gradle-consumer.ps1") `
        -ReleaseVersion $prepared.ReleaseVersion `
        -TestLensRepository $prepared.StagingDirectory `
        -KeepWorkDirectoryOnFailure:$KeepWorkDirectoryOnFailure
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle clean-room consumer failed with exit code $LASTEXITCODE."
    }

    $succeeded = $true
    Write-Host "Consumer compatibility validation passed for release $($prepared.ReleaseVersion)."
}
finally {
    if ($null -ne $prepared -and ($succeeded -or -not $KeepWorkDirectoryOnFailure)) {
        Remove-TestLensTemporaryDirectory `
            -Path $prepared.WorkDirectory `
            -RequiredNamePrefix "selenium-test-lens-clean-room-"
    } elseif ($null -ne $prepared) {
        Write-Warning "Preserved compatibility work directory: $($prepared.WorkDirectory)"
    }
}
