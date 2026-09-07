$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
$consumer = Join-Path $repo "consumer-tests/gradle"
$jar = Join-Path $consumer "gradle/wrapper/gradle-wrapper.jar"
$propertiesPath = Join-Path $consumer "gradle/wrapper/gradle-wrapper.properties"
$expectedWrapperHash = "2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046"
$expectedDistributionHash = "31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26"

if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) { throw "Gradle wrapper JAR is missing" }
if (-not (Test-Path -LiteralPath $propertiesPath -PathType Leaf)) { throw "Gradle wrapper properties are missing" }
$actual = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne $expectedWrapperHash) {
    throw "Gradle wrapper JAR SHA-256 mismatch: expected $expectedWrapperHash, found $actual"
}
$properties = [IO.File]::ReadAllText($propertiesPath)
if (-not $properties.Contains("gradle-8.10.2-bin.zip")) { throw "Gradle wrapper must use exactly 8.10.2-bin" }
if (-not $properties.Contains("distributionSha256Sum=$expectedDistributionHash")) {
    throw "Gradle distribution SHA-256 is missing or incorrect"
}
Write-Output "Gradle Wrapper validation PASS: Gradle 8.10.2, wrapper SHA-256 $actual"
