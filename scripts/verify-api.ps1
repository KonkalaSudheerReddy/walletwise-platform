[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    throw 'Node.js 20 or newer is required for API verification.'
}

$env:WALLETWISE_BASE_URL = $BaseUrl
try {
    & node (Join-Path $repoRoot 'scripts\verify-api.mjs')
    if ($LASTEXITCODE -ne 0) {
        throw "API verification failed with exit code $LASTEXITCODE."
    }
}
finally {
    Remove-Item Env:WALLETWISE_BASE_URL -ErrorAction SilentlyContinue
}

