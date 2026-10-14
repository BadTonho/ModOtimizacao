param (
    [int]$DurationSeconds = 60,
    [string]$WorldName = ""
)

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  INICIANDO SUITE DE BENCHMARK AUTOMATIZADO - MINECRAFT 26.3    " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

# 1. Deteccao do Mundo de Teste
$benchmarkWorldDir = "benchmark/world"
$detectedWorld = $null

if ($WorldName -ne "") {
    $detectedWorld = $WorldName
} else {
    $worldCandidates = Get-ChildItem -Path $benchmarkWorldDir -Directory -ErrorAction SilentlyContinue | Where-Object {
        Test-Path (Join-Path $_.FullName "level.dat")
    }
    if ($worldCandidates) {
        $detectedWorld = $worldCandidates[0].Name
        Write-Host "[Benchmark] Mundo pesado detectado em benchmark/world: '$detectedWorld'" -ForegroundColor Green
    } elseif (Test-Path "run/saves") {
        $runSaves = Get-ChildItem -Path "run/saves" -Directory -ErrorAction SilentlyContinue | Where-Object {
            Test-Path (Join-Path $_.FullName "level.dat")
        }
        if ($runSaves) {
            $detectedWorld = $runSaves[0].Name
            Write-Host "[Benchmark] Usando mundo existente em run/saves: '$detectedWorld'" -ForegroundColor Yellow
        }
    }
}

if (-not $detectedWorld) {
    Write-Warning "[Benchmark] Nenhum mundo encontrado!"
    Write-Host "Por favor, cole a pasta do seu mundo de teste dentro de 'benchmark/world/' e execute novamente." -ForegroundColor Yellow
    Write-Host "Exemplo: benchmark/world/MeuMundoPesado/level.dat" -ForegroundColor Gray
    exit 1
}

# Se o mundo esta em benchmark/world, copia para run/saves se necessario
$sourceWorldPath = Join-Path "benchmark/world" $detectedWorld
$targetWorldPath = Join-Path "run/saves" $detectedWorld
if ((Test-Path $sourceWorldPath) -and -not (Test-Path $targetWorldPath)) {
    Write-Host "[Benchmark] Copiando mundo de teste para o ambiente de execucao..." -ForegroundColor Gray
    Copy-Item -Path $sourceWorldPath -Destination "run/saves" -Recurse -Force
}

# 2. Prepara diretorio de resultados
$resultsDir = "benchmark/results"
if (-not (Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir -Force | Out-Null
}

function Run-BenchmarkInstance {
    param (
        [string]$TestName,
        [string]$OutputFile,
        [scriptblock]$BeforeLaunch,
        [scriptblock]$AfterLaunch
    )

    Write-Host "`n----------------------------------------------------------------" -ForegroundColor Magenta
    Write-Host "  EXECUTANDO INSTANCIA: $TestName" -ForegroundColor Magenta
    Write-Host "----------------------------------------------------------------" -ForegroundColor Magenta

    try {
        if ($BeforeLaunch) {
            & $BeforeLaunch
        }

        Write-Host "[Benchmark] Iniciando Minecraft 26.3 (--quickPlaySingleplayer '$detectedWorld')..." -ForegroundColor Cyan
        $gradleArgs = "runClient --args=""--quickPlaySingleplayer `"$detectedWorld`""""
        $proc = Start-Process -FilePath ".\gradlew.bat" -ArgumentList $gradleArgs -PassThru -NoNewWindow

        Write-Host "[Benchmark] Aguardando o cliente carregar e entrar no mundo..." -ForegroundColor Gray
        $javaProc = $null
        for ($w = 0; $w -lt 40; $w++) {
            Start-Sleep -Seconds 1
            $candidates = Get-Process java -ErrorAction SilentlyContinue | Sort-Object WorkingSet64 -Descending
            if ($candidates) {
                $javaProc = $candidates[0]
                if ($javaProc.WorkingSet64 -gt 500MB) {
                    break
                }
            }
        }

        if (-not $javaProc) {
            Write-Error "[Benchmark] Falha ao detectar processo Java do Minecraft."
            return
        }

        Write-Host "[Benchmark] Jogo pronto (PID: $($javaProc.Id)). Iniciando coleta por $DurationSeconds segundos..." -ForegroundColor Green
        
        & powershell -ExecutionPolicy Bypass -File "benchmark/collector.ps1" -ProcessId $javaProc.Id -OutputFile $OutputFile -DurationSeconds $DurationSeconds

        Write-Host "[Benchmark] Finalizando instancia $TestName..." -ForegroundColor Gray
        Stop-Process -Id $javaProc.Id -Force -ErrorAction SilentlyContinue
        if ($proc -and -not $proc.HasExited) {
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Seconds 3
    } finally {
        if ($AfterLaunch) {
            & $AfterLaunch
        }
    }
}

try {
    # ==============================================================
    # TESTE 1: VANILLA
    # ==============================================================
    $vanillaOutput = "$resultsDir/telemetry_vanilla.csv"
    Run-BenchmarkInstance -TestName "1. Minecraft Vanilla Puro" -OutputFile $vanillaOutput -BeforeLaunch {
        if (Test-Path "src/main/resources/fabric.mod.json") {
            Rename-Item -Path "src/main/resources/fabric.mod.json" -NewName "fabric.mod.json.disabled" -Force
        }
        if (Test-Path "run/mods") {
            Remove-Item -Path "run/mods/*" -Recurse -Force -ErrorAction SilentlyContinue
        }
    } -AfterLaunch {
        if (Test-Path "src/main/resources/fabric.mod.json.disabled") {
            Rename-Item -Path "src/main/resources/fabric.mod.json.disabled" -NewName "fabric.mod.json" -Force
        }
    }

    # ==============================================================
    # TESTE 2: MODS EXTERNOS (SODIUM / OUTROS, SE PRESENTES)
    # ==============================================================
    $externalMods = Get-ChildItem -Path "benchmark/mods" -Filter "*.jar" -ErrorAction SilentlyContinue
    $sodiumOutput = "$resultsDir/telemetry_sodium.csv"

    if ($externalMods) {
        $modNames = ($externalMods | ForEach-Object { $_.BaseName }) -join ", "
        Run-BenchmarkInstance -TestName "2. Minecraft + Mods ($modNames)" -OutputFile $sodiumOutput -BeforeLaunch {
            if (Test-Path "src/main/resources/fabric.mod.json") {
                Rename-Item -Path "src/main/resources/fabric.mod.json" -NewName "fabric.mod.json.disabled" -Force
            }
            if (-not (Test-Path "run/mods")) {
                New-Item -ItemType Directory -Path "run/mods" -Force | Out-Null
            }
            foreach ($m in $externalMods) {
                Copy-Item -Path $m.FullName -Destination "run/mods" -Force
            }
        } -AfterLaunch {
            if (Test-Path "run/mods") {
                Remove-Item -Path "run/mods/*" -Recurse -Force -ErrorAction SilentlyContinue
            }
            if (Test-Path "src/main/resources/fabric.mod.json.disabled") {
                Rename-Item -Path "src/main/resources/fabric.mod.json.disabled" -NewName "fabric.mod.json" -Force
            }
        }
    } else {
        Write-Host "`n[Benchmark] Nenhum mod externo encontrado em benchmark/mods/. Pulando teste comparativo de mods externos." -ForegroundColor Yellow
    }

    # ==============================================================
    # TESTE 3: NOSSO MOD DE OTIMIZACAO
    # ==============================================================
    $modOutput = "$resultsDir/telemetry_modotimizacao.csv"
    Run-BenchmarkInstance -TestName "3. Minecraft + Nosso Mod de Otimizacao" -OutputFile $modOutput -BeforeLaunch {
        if (Test-Path "src/main/resources/fabric.mod.json.disabled") {
            Rename-Item -Path "src/main/resources/fabric.mod.json.disabled" -NewName "fabric.mod.json" -Force
        }
        if (Test-Path "run/mods") {
            Remove-Item -Path "run/mods/*" -Recurse -Force -ErrorAction SilentlyContinue
        }
    } -AfterLaunch {
        # Mantem ativo
    }
} finally {
    # Garante 100% que o mod sempre volta ao estado normal
    if (Test-Path "src/main/resources/fabric.mod.json.disabled") {
        Rename-Item -Path "src/main/resources/fabric.mod.json.disabled" -NewName "fabric.mod.json" -Force
    }
}

# ==============================================================
# PROCESSAMENTO DE DADOS E RELATORIO FINAL
# ==============================================================
Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "  PROCESSANDO DADOS E GERANDO BENCHMARK_RESULTS.md...           " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

function Get-Averages {
    param ([string]$CsvPath)
    if (-not (Test-Path $CsvPath)) { return $null }
    $data = Import-Csv -Path $CsvPath -Encoding UTF8
    if (-not $data -or $data.Count -eq 0) { return $null }

    $avgCpu = [math]::Round(($data | Measure-Object -Property CpuPercent -Average).Average, 1)
    $maxRam = [math]::Round(($data | Measure-Object -Property RamMB -Maximum).Maximum, 1)
    $avgGpu = [math]::Round(($data | Measure-Object -Property GpuPercent -Average).Average, 1)
    $maxVram = [math]::Round(($data | Measure-Object -Property VramMB -Maximum).Maximum, 1)

    return [PSCustomObject]@{
        AvgCpu  = $avgCpu
        MaxRam  = $maxRam
        AvgGpu  = $avgGpu
        MaxVram = $maxVram
        Samples = $data.Count
    }
}

$vanillaStats = Get-Averages -CsvPath $vanillaOutput
$sodiumStats  = Get-Averages -CsvPath $sodiumOutput
$modStats     = Get-Averages -CsvPath $modOutput

$gpuName = "NVIDIA GeForce GTX 1660 SUPER (6GB)"
try {
    $smiName = & nvidia-smi --query-gpu=name --format=csv,noheader 2>$null
    if ($smiName) { $gpuName = $smiName.Trim() }
} catch {}

$report = @"
# 🏆 Relatorio Comparativo de Benchmarks de Desempenho

* **Data da Execucao:** $(Get-Date -Format "dd/MM/yyyy HH:mm:ss")
* **Placa de Video (GPU):** $gpuName
* **Versao do Jogo:** Minecraft 26.3 (Fabric Loader 0.19.5, Java 25 LTS)
* **Mundo de Teste:** \`$detectedWorld\`
* **Duracao por Instancia:** $DurationSeconds segundos

---

## 📊 Tabela Comparativa de Hardware

| Configuracao | Amostras | CPU Media (%) | RAM Maxima (MB) | GPU Media (%) | VRAM Maxima (MB) |
| :--- | :---: | :---: | :---: | :---: | :---: |
"@

if ($vanillaStats) {
    $report += "`n| **1. Minecraft Vanilla Puro** | $($vanillaStats.Samples)s | **$($vanillaStats.AvgCpu)%** | **$($vanillaStats.MaxRam) MB** | **$($vanillaStats.AvgGpu)%** | **$($vanillaStats.MaxVram) MB** |"
}
if ($sodiumStats) {
    $report += "`n| **2. Minecraft + Sodium** | $($sodiumStats.Samples)s | **$($sodiumStats.AvgCpu)%** | **$($sodiumStats.MaxRam) MB** | **$($sodiumStats.AvgGpu)%** | **$($sodiumStats.MaxVram) MB** |"
}
if ($modStats) {
    $report += "`n| **3. Minecraft + Nosso Mod** | $($modStats.Samples)s | **$($modStats.AvgCpu)%** | **$($modStats.MaxRam) MB** | **$($modStats.AvgGpu)%** | **$($modStats.MaxVram) MB** |"
}

$report += @"

---

## 💡 Analise Tecnica e Conclusoes
* **CPU:** Medicao em tempo real da JVM. Menor uso de CPU indica menos ciclos desperdicados na main render thread.
* **Memoria RAM:** Pico de consumo de memoria fisica ativa (*Working Set*). Menor consumo representa menor pressao no Garbage Collector (GC).
* **GPU & VRAM:** Amostrado diretamente pelo utilitario oficial \`nvidia-smi\` da NVIDIA. Taxa de ocupacao dos nucleos CUDA e uso de memoria grafica dedicada.

*Relatorio gerado automaticamente pela suite de benchmark de \`BadTonho/ModOtimizacao\`.*
"@

Set-Content -Path "BENCHMARK_RESULTS.md" -Value $report -Encoding UTF8

Write-Host "`n================================================================" -ForegroundColor Green
Write-Host "  BENCHMARK CONCLUIDO COM SUCESSO!                              " -ForegroundColor Green
Write-Host "  Resultados salvos em: BENCHMARK_RESULTS.md                    " -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
