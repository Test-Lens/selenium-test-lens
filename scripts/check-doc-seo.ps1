param(
    [Parameter(Mandatory=$true)][string]$SiteDirectory,
    [Parameter(Mandatory=$true)][string]$CurrentVersion,
    [string]$HistoricalVersion = "0.1.0",
    [string]$PublicBase = "https://test-lens.github.io/selenium-test-lens/",
    [switch]$SkipHttp
)

$ErrorActionPreference = "Stop"
$site = (Resolve-Path -LiteralPath $SiteDirectory).Path
$publicUri = [Uri]$PublicBase
$expectedHost = $publicUri.Host
$basePath = $publicUri.AbsolutePath.TrimEnd('/') + '/'

function Read-SiteFile([string]$RelativePath) {
    $path = Join-Path $site ($RelativePath.Replace('/', [IO.Path]::DirectorySeparatorChar))
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing generated documentation file: $RelativePath" }
    return [IO.File]::ReadAllText($path)
}

function Get-Canonical([string]$Html, [string]$RelativePath) {
    $match = [regex]::Match($Html, '<link\s+rel="canonical"\s+href="([^"]+)"', 'IgnoreCase')
    if (-not $match.Success) { throw "Missing canonical link: $RelativePath" }
    if ([regex]::Matches($Html, '<link\s+rel="canonical"\s+href=', 'IgnoreCase').Count -ne 1) {
        throw "Expected exactly one canonical link: $RelativePath"
    }
    return $match.Groups[1].Value
}

function Convert-PublicUrlToSitePath([string]$Url) {
    $uri = [Uri]$Url
    if ($uri.Scheme -ne 'https' -or $uri.Host -ne $expectedHost) { throw "Canonical must use HTTPS on ${expectedHost}: $Url" }
    if (-not $uri.AbsolutePath.StartsWith($basePath, [StringComparison]::Ordinal)) {
        throw "Canonical is outside the documentation root: $Url"
    }
    $relative = $uri.AbsolutePath.Substring($basePath.Length)
    if ([string]::IsNullOrEmpty($relative) -or $relative.EndsWith('/')) { $relative += 'index.html' }
    return [Uri]::UnescapeDataString($relative)
}

function Test-RedirectHtml([string]$Html) {
    return $Html -match 'window\.location\.replace\s*\(' -or $Html -match 'http-equiv\s*=\s*["'']refresh["'']'
}

function Assert-CanonicalPage([string]$RelativePath, [string]$ExpectedCanonical) {
    $html = Read-SiteFile $RelativePath
    if (Test-RedirectHtml $html) { throw "Indexable page is a client redirect: $RelativePath" }
    $canonical = Get-Canonical $html $RelativePath
    if ($canonical -ne $ExpectedCanonical) {
        throw "Canonical mismatch for ${RelativePath}: expected '$ExpectedCanonical', found '$canonical'."
    }
    $target = Convert-PublicUrlToSitePath $canonical
    $targetHtml = Read-SiteFile $target
    if (Test-RedirectHtml $targetHtml) { throw "Canonical resolves to a client redirect: $canonical" }
    foreach ($required in @('<title>', 'name="description"', 'property="og:url"')) {
        if (-not $html.Contains($required)) { throw "SEO metadata '$required' missing from $RelativePath" }
    }
}

$rootCanonical = $PublicBase
$latestCanonical = "${PublicBase}latest/"
Assert-CanonicalPage 'index.html' $rootCanonical
Assert-CanonicalPage 'latest/index.html' $latestCanonical
Assert-CanonicalPage 'latest/getting-started/index.html' "${PublicBase}latest/getting-started/"

foreach ($version in @($CurrentVersion, $HistoricalVersion)) {
    Assert-CanonicalPage "$version/index.html" $latestCanonical
}

$devHome = Read-SiteFile 'dev/index.html'
if ((Get-Canonical $devHome 'dev/index.html') -ne $latestCanonical) { throw "dev canonical must point to latest." }
if ($devHome -notmatch '<meta\s+name="robots"\s+content="noindex,follow"') { throw "dev documentation must be noindex,follow." }

[xml]$rootSitemap = Read-SiteFile 'sitemap.xml'
$rootLocations = @($rootSitemap.sitemapindex.sitemap.loc | ForEach-Object { [string]$_ })
if ($rootLocations.Count -ne 1 -or $rootLocations[0] -ne "${PublicBase}latest/sitemap.xml") {
    throw "Root sitemap index must reference only the latest sitemap."
}
$robots = Read-SiteFile 'robots.txt'
if (-not $robots.Contains('Disallow: /selenium-test-lens/dev/')) { throw "robots.txt must exclude dev documentation." }
if (-not $robots.Contains("Sitemap: ${PublicBase}sitemap.xml")) { throw "robots.txt must advertise the root sitemap." }

[xml]$latestSitemap = Read-SiteFile 'latest/sitemap.xml'
$latestLocations = @($latestSitemap.urlset.url.loc | ForEach-Object { [string]$_ })
if ($latestLocations.Count -eq 0) { throw "Latest sitemap is empty." }
$seen = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach ($location in $latestLocations) {
    if (-not $seen.Add($location)) { throw "Duplicate URL in latest sitemap: $location" }
    if (-not $location.StartsWith("${PublicBase}latest/", [StringComparison]::Ordinal)) {
        throw "Non-latest URL in production sitemap: $location"
    }
    $relative = Convert-PublicUrlToSitePath $location
    $html = Read-SiteFile $relative
    if (Test-RedirectHtml $html) { throw "Sitemap URL is a client redirect: $location" }
    if ((Get-Canonical $html $relative) -ne $location) { throw "Sitemap URL is not self-canonical: $location" }
}

foreach ($htmlFile in Get-ChildItem -LiteralPath $site -Filter *.html -File -Recurse) {
    $relative = $htmlFile.FullName.Substring($site.Length + 1).Replace('\','/')
    $html = [IO.File]::ReadAllText($htmlFile.FullName)
    if (-not (Test-RedirectHtml $html)) { continue }
    if ($relative -eq 'index.html') { throw "Documentation root must not redirect." }
    $canonical = Get-Canonical $html $relative
    if (-not [Uri]::IsWellFormedUriString($canonical, [UriKind]::Absolute)) {
        $sourceUrl = [Uri]::new([Uri]$PublicBase, $relative)
        $canonical = [Uri]::new($sourceUrl, $canonical).AbsoluteUri
    }
    $target = Convert-PublicUrlToSitePath $canonical
    $targetHtml = Read-SiteFile $target
    if (Test-RedirectHtml $targetHtml) { throw "Redirect chain detected: $relative -> $target" }
}

if (-not $SkipHttp) {
    $python = $null
    foreach ($candidate in @('python', 'python3')) {
        $command = Get-Command $candidate -ErrorAction SilentlyContinue
        if ($null -ne $command) { $python = $command.Source; break }
    }
    if ($null -eq $python) { throw "Python is required for the local generated-site HTTP validation." }

    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $listener.Start()
    $port = ([Net.IPEndPoint]$listener.LocalEndpoint).Port
    $listener.Stop()
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $python
    $start.WorkingDirectory = $site
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.ArgumentList.Add('-m')
    $start.ArgumentList.Add('http.server')
    $start.ArgumentList.Add([string]$port)
    $start.ArgumentList.Add('--bind')
    $start.ArgumentList.Add('127.0.0.1')
    $server = [Diagnostics.Process]::Start($start)
    try {
        $handler = [Net.Http.HttpClientHandler]::new()
        $handler.AllowAutoRedirect = $false
        $client = [Net.Http.HttpClient]::new($handler)
        $base = "http://127.0.0.1:$port/"
        $ready = $false
        for ($attempt = 0; $attempt -lt 50; $attempt++) {
            try {
                $probe = $client.GetAsync($base).GetAwaiter().GetResult()
                if ([int]$probe.StatusCode -eq 200) { $ready = $true; break }
            } catch { Start-Sleep -Milliseconds 100 }
        }
        if (-not $ready) { throw "Local documentation HTTP server did not become ready." }
        foreach ($path in @('', 'latest/', 'latest/getting-started/', "$CurrentVersion/", "$HistoricalVersion/", 'sitemap.xml', 'latest/sitemap.xml')) {
            $response = $client.GetAsync($base + $path).GetAwaiter().GetResult()
            if ([int]$response.StatusCode -ne 200) { throw "Local HTTP validation failed for /$path with $($response.StatusCode)." }
            if ($response.Headers.Location) { throw "Local HTTP endpoint unexpectedly redirected: /$path" }
        }
        $client.Dispose()
        $handler.Dispose()
    } finally {
        if (-not $server.HasExited) { $server.Kill($true) }
        $server.WaitForExit(5000) | Out-Null
        $server.Dispose()
    }
}

Write-Host "Documentation SEO contract PASS: crawlable root, latest canonicals, sitemap, robots, and redirect graph."
