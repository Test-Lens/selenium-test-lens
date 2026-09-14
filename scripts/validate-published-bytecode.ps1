param(
    [Parameter(Mandatory)][string]$TestLensRepository,
    [Parameter(Mandatory)][string]$ReleaseVersion
)

$ErrorActionPreference = "Stop"
$repository = [IO.Path]::GetFullPath($TestLensRepository)
if (-not (Test-Path -LiteralPath $repository -PathType Container)) {
    throw "Test Lens repository does not exist: $repository"
}
$versionPath = Join-Path $repository ("io/github/test-lens")
$jars = @(Get-ChildItem -LiteralPath $versionPath -Recurse -Filter "*-$ReleaseVersion.jar" |
    Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' })
$expectedArtifactIds = @(
    "selenium-test-lens-core",
    "selenium-test-lens-overlay",
    "selenium-test-lens",
    "selenium-test-lens-react",
    "selenium-test-lens-junit5",
    "selenium-test-lens-testng",
    "selenium-test-lens-allure"
)
$actualArtifactIds = @($jars | ForEach-Object { $_.Directory.Parent.Name } | Sort-Object -Unique)
$missingArtifactIds = @($expectedArtifactIds | Where-Object { $_ -notin $actualArtifactIds })
$unexpectedArtifactIds = @($actualArtifactIds | Where-Object { $_ -notin $expectedArtifactIds })
if ($missingArtifactIds.Count -gt 0 -or $unexpectedArtifactIds.Count -gt 0) {
    throw "Published library JAR set mismatch. Missing: [$($missingArtifactIds -join ', ')]. " +
        "Unexpected: [$($unexpectedArtifactIds -join ', ')]. Actual: [$($actualArtifactIds -join ', ')]."
}
if ($jars.Count -ne $expectedArtifactIds.Count) {
    throw "Expected exactly one main JAR for each published library artifact; found $($jars.Count) JARs for " +
        "$($expectedArtifactIds.Count) artifactIds."
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$maximumMajor = 0
foreach ($jar in $jars) {
    $archive = [IO.Compression.ZipFile]::OpenRead($jar.FullName)
    try {
        foreach ($entry in $archive.Entries) {
            if (-not $entry.FullName.EndsWith(".class", [StringComparison]::Ordinal)) { continue }
            $stream = $entry.Open()
            try {
                $header = New-Object byte[] 8
                if ($stream.Read($header, 0, 8) -ne 8) { throw "Invalid class header: $($entry.FullName)" }
                $major = ($header[6] -shl 8) -bor $header[7]
                $maximumMajor = [Math]::Max($maximumMajor, $major)
                if ($major -gt 61) { throw "$($entry.FullName) targets class-file major $major; maximum is 61" }
                $buffer = New-Object byte[] 8192
                $memory = [IO.MemoryStream]::new()
                $memory.Write($header, 0, $header.Length)
                while (($read = $stream.Read($buffer, 0, $buffer.Length)) -gt 0) { $memory.Write($buffer, 0, $read) }
                $ascii = [Text.Encoding]::ASCII.GetString($memory.ToArray())
                if ($ascii.Contains("jdk/internal/")) { throw "$($entry.FullName) references forbidden jdk.internal API" }
            } finally { $stream.Dispose() }
        }
    } finally { $archive.Dispose() }
}
Write-Output "Published bytecode validation PASS: $($jars.Count) JARs, maximum class-file major $maximumMajor"
