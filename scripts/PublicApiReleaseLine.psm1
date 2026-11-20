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

function Read-TestLensApiCompatibilityProjection {
    param([string[]]$ManifestLines, [object[]]$ClassificationRows, [string]$Label)
    # Newer manifests are already generated AFTER classification-policy overrides.
    # Reapplying the baseline CSV would demote promoted nested/tooling types again.
    $alreadyProjected = @($ManifestLines | Where-Object { $_ -eq '# Generated stable/advanced API compatibility manifest. Do not edit by hand.' }).Count -gt 0
    $compatibility = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    @($ClassificationRows | Where-Object { $_.Classification -in @('USER_API', 'ADVANCED_API') }) |
        ForEach-Object { [void]$compatibility.Add($_.Type) }
    if (-not $alreadyProjected -and $compatibility.Count -eq 0) { throw "$Label classification has no USER_API or ADVANCED_API types" }
    $types = [Collections.Generic.List[string]]::new()
    $callables = 0
    $included = $false
    foreach ($line in $ManifestLines) {
        if ($line -like 'TYPE *') {
            $type = $line.Substring(5)
            $included = $alreadyProjected -or $compatibility.Contains($type)
            if ($included) { $types.Add($type) }
        } elseif ($included -and $line -match '^\s+public .*\(') { $callables++ }
    }
    return [pscustomobject]@{ Types = $types.ToArray(); Callables = $callables }
}

Export-ModuleMember -Function Resolve-TestLensApiReleaseLine, Read-TestLensApiCompatibilityProjection
