[CmdletBinding()]
param(
    [switch]$SkipDocker,
    [switch]$E2E
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory)]
        [string]$Executable,

        [Parameter(Mandatory)]
        [string[]]$Arguments
    )

    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Executable failed with exit code $LASTEXITCODE."
    }
}
Push-Location $repoRoot
try {
    Write-Host '[1/3] Verifying backend'
    Invoke-CheckedCommand -Executable '.\backend\mvnw.cmd' -Arguments @(
        '-B', '-ntp', '-f', 'backend\pom.xml', 'clean', 'verify'
    )

    Write-Host '[2/3] Verifying frontend'
    Invoke-CheckedCommand -Executable 'npm.cmd' -Arguments @('ci', '--prefix', 'frontend')
    Invoke-CheckedCommand -Executable 'npm.cmd' -Arguments @('run', 'format:check', '--prefix', 'frontend')
    Invoke-CheckedCommand -Executable 'npm.cmd' -Arguments @('run', 'lint', '--prefix', 'frontend')
    Invoke-CheckedCommand -Executable 'npm.cmd' -Arguments @('run', 'test', '--prefix', 'frontend')
    Invoke-CheckedCommand -Executable 'npm.cmd' -Arguments @('run', 'build', '--prefix', 'frontend')

    if ($E2E) {
        Write-Host 'Running Playwright against PLAYWRIGHT_BASE_URL (default: http://localhost:8080)'
        Invoke-CheckedCommand -Executable 'npm.cmd' -Arguments @('run', 'test:e2e', '--prefix', 'frontend')
    }

    if ($SkipDocker) {
        Write-Host '[3/3] Docker verification skipped by request'
    }
    else {
        Write-Host '[3/3] Validating Compose and production image'
        Invoke-CheckedCommand -Executable 'docker' -Arguments @('compose', 'config', '--quiet')
        Invoke-CheckedCommand -Executable 'docker' -Arguments @(
            'build', '--tag', 'walletwise-platform:verify', '.'
        )
    }

    Write-Host 'WalletWise verification completed successfully.'
}
finally {
    Pop-Location
}
