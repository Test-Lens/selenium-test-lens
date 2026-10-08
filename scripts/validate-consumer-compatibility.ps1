param(
    [string]$ReleaseVersion,
    [switch]$KeepWorkDirectoryOnFailure,
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"
$repo = [IO.Path]::GetFullPath($RepositoryRoot)
Import-Module (Join-Path $PSScriptRoot "CleanRoomRelease.psm1") -Force

$prepared = $null
$succeeded = $false
try {
    $prepared = New-TestLensCleanRoomRelease `
        -RepositoryRoot $repo `
        -ReleaseVersion $ReleaseVersion

    & (Join-Path $PSScriptRoot "validate-clean-room-consumer.ps1") `
        -RepositoryRoot $repo `
        -ReleaseVersion $prepared.ReleaseVersion `
        -TestLensRepository $prepared.StagingRoot
    if ($LASTEXITCODE -ne 0) {
        throw "Maven clean-room consumer failed with exit code $LASTEXITCODE."
    }

    & (Join-Path $PSScriptRoot "validate-gradle-consumer.ps1") `
        -RepositoryRoot $repo `
        -ReleaseVersion $prepared.ReleaseVersion `
        -TestLensRepository $prepared.StagingRoot `
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
            -RequiredNamePrefix "tlcr-"
    } elseif ($null -ne $prepared) {
        Write-Warning "Preserved compatibility work directory: $($prepared.WorkDirectory)"
    }
}
