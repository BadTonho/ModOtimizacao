param (
    [Parameter(Mandatory=$true)]
    [int]$ProcessId,
    [Parameter(Mandatory=$true)]
    [string]$OutputFile,
    [int]$DurationSeconds = 60
)

# Garante diretório de destino
$parentDir = Split-Path -Parent $OutputFile
if ($parentDir -and -not (Test-Path $parentDir)) {
    New-Item -ItemType Directory -Path $parentDir -Force | Out-Null
}

# Inicializa arquivo com cabeçalho
$header = "Timestamp,ElapsedSec,CpuPercent,RamMB,GpuPercent,VramMB"
Set-Content -Path $OutputFile -Value $header -Encoding UTF8

$cpuCores = [Environment]::ProcessorCount
$process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
if (-not $process) {
    Write-Warning "[Collector] Processo $ProcessId não encontrado."
    exit 1
}

$prevCpuTime = $process.CPU
$prevTime = [DateTime]::UtcNow

Write-Host "[Collector] Monitorando PID $ProcessId por $DurationSeconds segundos..." -ForegroundColor Cyan

for ($i = 1; $i -le $DurationSeconds; $i++) {
    Start-Sleep -Seconds 1

    $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if (-not $process -or $process.HasExited) {
        Write-Warning "[Collector] Processo encerrou antes do término planejado."
        break
    }

    $now = [DateTime]::UtcNow
    $timeDelta = ($now - $prevTime).TotalSeconds
    $curCpuTime = $process.CPU
    $cpuDelta = $curCpuTime - $prevCpuTime

    $cpuPercent = 0.0
    if ($timeDelta -gt 0) {
        $cpuPercent = [math]::Round(($cpuDelta / ($timeDelta * $cpuCores)) * 100, 1)
        if ($cpuPercent -gt 100.0) { $cpuPercent = 100.0 }
        if ($cpuPercent -lt 0.0) { $cpuPercent = 0.0 }
    }

    $ramMB = [math]::Round($process.WorkingSet64 / 1MB, 1)

    # Consulta GPU e VRAM via nvidia-smi
    $gpuPercent = 0
    $vramMB = 0
    try {
        $smiOutput = & nvidia-smi --query-gpu=utilization.gpu,memory.used --format=csv,noheader,nounits 2>$null
        if ($smiOutput) {
            $parts = $smiOutput.Trim().Split(',')
            if ($parts.Length -ge 2) {
                $gpuPercent = [int]$parts[0].Trim()
                $vramMB = [int]$parts[1].Trim()
            }
        }
    } catch {
        # Fallback silencioso caso nvidia-smi oscile
    }

    $timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $line = "$timestamp,$i,$cpuPercent,$ramMB,$gpuPercent,$vramMB"
    Add-Content -Path $OutputFile -Value $line -Encoding UTF8

    $prevCpuTime = $curCpuTime
    $prevTime = $now

    # Feedback formatado em tempo real no terminal
    Write-Host ("[{0,2}s/{1}s] CPU: {2,5}% | RAM: {3,6} MB | GPU: {4,3}% | VRAM: {5,5} MB" -f $i, $DurationSeconds, $cpuPercent, $ramMB, $gpuPercent, $vramMB) -ForegroundColor Gray
}

Write-Host "[Collector] Coleta finalizada: $OutputFile" -ForegroundColor Green
