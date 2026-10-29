[CmdletBinding()]
param(
    [string]$SettingsPath,
    [ValidatePattern('^[A-Za-z0-9_,.*]+$')]
    [string[]]$IntegrationTests,
    [switch]$Online
)

$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$backendRoot = Join-Path $projectRoot 'backend'
if (-not $SettingsPath) {
    $SettingsPath = Join-Path $backendRoot '.local/postgres16/test-settings.json'
}
if (-not (Test-Path -LiteralPath $SettingsPath -PathType Leaf)) {
    throw 'Native test settings are missing. See docs/testing/NATIVE_POSTGRES.md; no dev-DB fallback is permitted.'
}

try {
    $settings = Get-Content -LiteralPath $SettingsPath -Raw | ConvertFrom-Json
}
catch {
    throw 'Cannot read the native test settings JSON. Its contents are not included in this error.'
}

$keys = @(
    'BAITI_TEST_DB_MODE', 'TEST_DB_URL', 'TEST_DB_APP_USER', 'TEST_DB_APP_PASSWORD',
    'TEST_DB_MIGRATOR_USER', 'TEST_DB_MIGRATOR_PASSWORD', 'TEST_DB_OWNER_TOKEN'
)
$previous = @{}
foreach ($key in $keys) {
    if ([string]::IsNullOrWhiteSpace([string]$settings.$key)) {
        throw "Required native test setting is missing: $key"
    }
    $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
}

$testExit = 1
try {
    foreach ($key in $keys) {
        [Environment]::SetEnvironmentVariable($key, [string]$settings.$key, 'Process')
    }
    $mavenArgs = @()
    if (-not $Online) { $mavenArgs += '-o' }
    if ($IntegrationTests) { $mavenArgs += ('-Dit.test=' + ($IntegrationTests -join ',')) }
    $mavenArgs += 'verify'
    Push-Location $backendRoot
    try {
        & mvn @mavenArgs
        $testExit = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
}
finally {
    foreach ($key in $keys) {
        [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process')
    }
}
exit $testExit
