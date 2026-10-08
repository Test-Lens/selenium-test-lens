Set-StrictMode -Version Latest

function Resolve-TestLensApiReleaseLine {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [string]$CurrentVersion,

        [Parameter(Mandatory)]
        [string[]]$ReleaseTags
    )

    if ($CurrentVersion -notmatch '^(\d+)\.(\d+)\.(\d+)(?:-SNAPSHOT)?$') {
        throw "Current version '$CurrentVersion' is not a supported release-line version"
    }
    $current = [version]::new([int]$Matches[1], [int]$Matches[2], [int]$Matches[3])
    $releases = @($ReleaseTags | ForEach-Object {
        $tag = $_.ToString().Trim()
        if ($tag -match '^v(\d+)\.(\d+)\.(\d+)$') {
            [pscustomobject]@{
                Tag = $tag
                Version = [version]::new([int]$Matches[1], [int]$Matches[2], [int]$Matches[3])
            }
        }
    } | Where-Object { $null -ne $_ -and $_.Version -lt $current } | Sort-Object Version -Descending)
    if ($releases.Count -eq 0) {
        throw "No previous release tag exists before $current"
    }

    return [pscustomobject]@{
        BaselineTag = $releases[0].Tag
        ExpectedSince = $current.ToString(3)
    }
}

Export-ModuleMember -Function Resolve-TestLensApiReleaseLine
