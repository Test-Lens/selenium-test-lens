Set-StrictMode -Version Latest

function Wait-TestLensStudioActionAvailable {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [string]$Action,

        [Parameter(Mandatory)]
        [scriptblock]$ReadState,

        [TimeSpan]$Timeout = [TimeSpan]::FromSeconds(30),

        [ValidateRange(1, 1000)]
        [int]$PollMilliseconds = 100
    )

    if ([string]::IsNullOrWhiteSpace($Action)) {
        throw "Action must not be blank"
    }
    if ($Timeout -le [TimeSpan]::Zero) {
        throw "Timeout must be positive"
    }

    $deadline = [DateTimeOffset]::UtcNow.Add($Timeout)
    $lastRunning = $null
    $lastActions = @()
    do {
        $state = & $ReadState
        if ($null -eq $state) {
            throw "Studio readiness state reader returned no state"
        }
        $lastRunning = [bool]$state.OperationRunning
        $lastActions = @($state.AvailableActions | Where-Object { $null -ne $_ } | ForEach-Object { [string]$_ })
        if (-not $lastRunning -and $lastActions -contains $Action) {
            return $state
        }

        $remaining = $deadline - [DateTimeOffset]::UtcNow
        if ($remaining -le [TimeSpan]::Zero) { break }
        $delay = [Math]::Min($PollMilliseconds, [Math]::Max(1, [int][Math]::Ceiling($remaining.TotalMilliseconds)))
        Start-Sleep -Milliseconds $delay
    } while ([DateTimeOffset]::UtcNow -lt $deadline)

    $actions = if ($lastActions.Count -eq 0) { "<none>" } else { [string]::Join(",", $lastActions) }
    throw "Timed out waiting for Studio action '$Action'; operationRunning=$lastRunning; availableActions=$actions; timeoutMs=$([long]$Timeout.TotalMilliseconds)"
}

Export-ModuleMember -Function Wait-TestLensStudioActionAvailable
