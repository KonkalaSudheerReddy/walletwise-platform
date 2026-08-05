[CmdletBinding()]
param(
    [switch]$Detached
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker is required. Install Docker Desktop or Docker Engine with Compose v2.'
}

Push-Location $repoRoot
try {
    & docker compose version | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Compose v2 is not available.'
    }

    $composeArguments = @('compose', 'up', '--build')
    if ($Detached) {
        $composeArguments += '--detach'
    }

    & docker @composeArguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

