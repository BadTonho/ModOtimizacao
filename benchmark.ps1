param (
    [int]$DurationSeconds = 60,
    [string]$WorldName = ""
)

# Garante que todos os caminhos relativos funcionem independentemente do diretorio de execucao
Set-Location -Path $PSScriptRoot

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
    } | Sort-Object Name
    if ($worldCandidates) {
        $detectedWorld = $worldCandidates[0].Name
        Write-Host "[Benchmark] Mundo pesado detectado em benchmark/world: '$detectedWorld'" -ForegroundColor Green
    } elseif (Test-Path "run/saves") {
        $runSaves = Get-ChildItem -Path "run/saves" -Directory -ErrorAction SilentlyContinue | Where-Object {
            Test-Path (Join-Path $_.FullName "level.dat") -and $_.Name -ne "benchmark_world"
        } | Sort-Object Name
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

# Normaliza o nome do mundo para 'benchmark_world' em run/saves (sem espacos)
# para evitar que o Gradle confunda nomes com espaco como tarefas separadas
$cleanWorldName = "benchmark_world"
$sourceWorldPath = Join-Path "benchmark/world" $detectedWorld
$targetWorldPath = Join-Path "run/saves" $cleanWorldName

Write-Host "[Benchmark] Sincronizando mundo para 'run/saves/$cleanWorldName'..." -ForegroundColor Gray
if (Test-Path $targetWorldPath) {
    Remove-Item -Path $targetWorldPath -Recurse -Force -ErrorAction SilentlyContinue
}
if (Test-Path $sourceWorldPath) {
    Copy-Item -Path $sourceWorldPath -Destination $targetWorldPath -Recurse -Force
    $effectiveSource = $sourceWorldPath
} else {
    $fallbackSource = Join-Path "run/saves" $detectedWorld
    if (Test-Path $fallbackSource) {
        Copy-Item -Path $fallbackSource -Destination $targetWorldPath -Recurse -Force
        $effectiveSource = $fallbackSource
    } else {
        $effectiveSource = $sourceWorldPath
    }
}

# 2. Prepara diretorio de resultados e ferramentas
$resultsDir = "benchmark/results"
if (-not (Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir -Force | Out-Null
}

$fpsRecorderJar = "benchmark/tools/fps-recorder-1.0.0.jar"
if (-not (Test-Path $fpsRecorderJar)) {
    Write-Host "[Benchmark] Compilando ferramenta neutra de telemetria de FPS..." -ForegroundColor Cyan
    & .\gradlew.bat buildFpsRecorder
}

function Run-BenchmarkInstance {
    param (
        [string]$TestName,
        [string]$OutputFile,
        [string]$FpsOutputFile,
        [scriptblock]$BeforeLaunch,
        [scriptblock]$AfterLaunch
    )

    Write-Host "`n----------------------------------------------------------------" -ForegroundColor Magenta
    Write-Host "  EXECUTANDO INSTANCIA: $TestName" -ForegroundColor Magenta
    Write-Host "----------------------------------------------------------------" -ForegroundColor Magenta

    try {
        # Mata qualquer processo anterior de Minecraft orfao (protegendo extensoes da IDE)
        $staleProcs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue | Where-Object {
            $_.CommandLine -notlike "*redhat.java*" -and ($_.CommandLine -like "*devlaunchinjector*" -or $_.CommandLine -like "*net.minecraft*" -or $_.CommandLine -like "*KnotClient*")
        }
        if ($staleProcs) {
            foreach ($sp in $staleProcs) {
                Stop-Process -Id $sp.ProcessId -Force -ErrorAction SilentlyContinue
            }
            Start-Sleep -Seconds 1
        }

        # Remove dados de FPS da execucao anterior no diretorio run
        if (Test-Path "run/benchmark_fps.csv") {
            Remove-Item -Path "run/benchmark_fps.csv" -Force -ErrorAction SilentlyContinue
        }

        # Restaura o estado limpo e consistente do mundo para a instancia
        if (Test-Path $effectiveSource) {
            Copy-Item -Path $effectiveSource -Destination $targetWorldPath -Recurse -Force -ErrorAction SilentlyContinue
        }

        if ($BeforeLaunch) {
            & $BeforeLaunch
        }

        # Garante que o gravador neutro de FPS esta presente em run/mods para medicao 100% igual
        if (-not (Test-Path "run/mods")) {
            New-Item -ItemType Directory -Path "run/mods" -Force | Out-Null
        }
        Copy-Item -Path $fpsRecorderJar -Destination "run/mods/fps-recorder-1.0.0.jar" -Force

        Write-Host "[Benchmark] Iniciando Minecraft 26.3 (--quickPlaySingleplayer $cleanWorldName)..." -ForegroundColor Cyan
        $launchTimestamp = Get-Date

        # O build.gradle ja injeta --quickPlaySingleplayer benchmark_world diretamente no Loom
        $proc = Start-Process -FilePath ".\gradlew.bat" -ArgumentList "runClient" -PassThru -NoNewWindow

        Write-Host "[Benchmark] Aguardando o cliente carregar e entrar no mundo..." -ForegroundColor Gray
        $javaProc = $null
        for ($w = 0; $w -lt 65; $w++) {
            Start-Sleep -Seconds 1
            if ($proc.HasExited -and $proc.ExitCode -ne 0) {
                Write-Error "[Benchmark] O Gradle encerrou com erro (Codigo: $($proc.ExitCode))."
                return
            }

            # Procura o processo Java exclusivo do cliente Minecraft
            if (-not $javaProc) {
                $candidates = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue | Where-Object {
                    $_.CommandLine -notlike "*redhat.java*" -and ($_.CommandLine -like "*devlaunchinjector*" -or $_.CommandLine -like "*net.minecraft*" -or $_.CommandLine -like "*KnotClient*")
                }
                if ($candidates) {
                    $javaProc = Get-Process -Id $candidates[0].ProcessId -ErrorAction SilentlyContinue
                }
            }

            # Verifica se o cliente ja esta no mundo renderizando frames
            if (Test-Path "run/benchmark_fps.csv") {
                $fpsLines = Get-Content "run/benchmark_fps.csv" -ErrorAction SilentlyContinue
                if ($fpsLines -and $fpsLines.Count -ge 2) {
                    Write-Host "[Benchmark] Cliente renderizando no mundo! Telemetria de FPS ativa." -ForegroundColor Green
                    break
                }
            }
        }

        # Aborta imediatamente se o cliente nunca renderizou no mundo (evita medir menus e gerar dado vazio)
        if (-not (Test-Path "run/benchmark_fps.csv")) {
            Write-Error "[Benchmark] O cliente nao entrou no mundo em 65s. Abortando esta instancia para nao coletar dados de menus."
            if ($javaProc) {
                Stop-Process -Id $javaProc.Id -Force -ErrorAction SilentlyContinue
            }
            if ($proc -and -not $proc.HasExited) {
                Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            }
            return
        }

        if (-not $javaProc) {
            # Fallback: apenas java.exe com marcadores claros do proprio jogo (nunca daemon/IDE)
            $candidates = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue | Where-Object {
                $_.CommandLine -notlike "*redhat.java*" -and $_.CommandLine -like "*-Dfabric*"
            }
            if ($candidates) {
                $javaProc = Get-Process -Id $candidates[0].ProcessId -ErrorAction SilentlyContinue
            }
        }

        if (-not $javaProc) {
            Write-Error "[Benchmark] Falha ao detectar o processo do Minecraft."
            return
        }

        Write-Host "[Benchmark] Jogo pronto no mundo (PID: $($javaProc.Id)). Iniciando coleta por $DurationSeconds segundos..." -ForegroundColor Green
        
        & powershell -ExecutionPolicy Bypass -File "benchmark/collector.ps1" -ProcessId $javaProc.Id -OutputFile $OutputFile -DurationSeconds $DurationSeconds

        Write-Host "[Benchmark] Finalizando instancia $TestName..." -ForegroundColor Gray
        Stop-Process -Id $javaProc.Id -Force -ErrorAction SilentlyContinue
        if ($proc -and -not $proc.HasExited) {
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Seconds 3

        # Salva a telemetria de FPS gravada internamente pelo cliente no mundo
        if (Test-Path "run/benchmark_fps.csv") {
            Move-Item -Path "run/benchmark_fps.csv" -Destination $FpsOutputFile -Force
            Write-Host "[Benchmark] Telemetria de FPS salva com sucesso em '$FpsOutputFile'." -ForegroundColor Green
        } else {
            Write-Warning "[Benchmark] Nao foi possivel encontrar run/benchmark_fps.csv para esta instancia."
        }
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
    $vanillaFpsOutput = "$resultsDir/fps_vanilla.csv"
    Run-BenchmarkInstance -TestName "1. Minecraft Vanilla Puro" -OutputFile $vanillaOutput -FpsOutputFile $vanillaFpsOutput -BeforeLaunch {
        if (Test-Path "src/main/resources/fabric.mod.json") {
            Rename-Item -Path "src/main/resources/fabric.mod.json" -NewName "fabric.mod.json.disabled" -Force
        }
        # Residuo compilado (task Copy do Gradle nao apaga saidas orfas):
        # sem isto, o teste "vanilla" pode carregar o mod salvo em execucoes anteriores.
        if (Test-Path "build/resources/main/fabric.mod.json") {
            Rename-Item -Path "build/resources/main/fabric.mod.json" -NewName "fabric.mod.json.disabled" -Force
        }
        if (Test-Path "run/mods") {
            Remove-Item -Path "run/mods/*" -Recurse -Force -ErrorAction SilentlyContinue
        }
    } -AfterLaunch {
        if (Test-Path "src/main/resources/fabric.mod.json.disabled") {
            Rename-Item -Path "src/main/resources/fabric.mod.json.disabled" -NewName "fabric.mod.json" -Force
        }
        if (Test-Path "build/resources/main/fabric.mod.json.disabled") {
            Rename-Item -Path "build/resources/main/fabric.mod.json.disabled" -NewName "fabric.mod.json" -Force
        }
    }

    # ==============================================================
    # TESTE 2: MODS EXTERNOS (SODIUM / OUTROS, SE PRESENTES)
    # ==============================================================
    $externalMods = Get-ChildItem -Path "benchmark/mods" -Filter "*.jar" -ErrorAction SilentlyContinue
    $sodiumOutput = "$resultsDir/telemetry_sodium.csv"
    $sodiumFpsOutput = "$resultsDir/fps_sodium.csv"

    if ($externalMods) {
        $modNames = ($externalMods | ForEach-Object { $_.BaseName }) -join ", "
        Run-BenchmarkInstance -TestName "2. Minecraft + Mods ($modNames)" -OutputFile $sodiumOutput -FpsOutputFile $sodiumFpsOutput -BeforeLaunch {
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
            if (Test-Path "build/resources/main/fabric.mod.json.disabled") {
                Rename-Item -Path "build/resources/main/fabric.mod.json.disabled" -NewName "fabric.mod.json" -Force
            }
        }
    } else {
        Write-Host "`n[Benchmark] Nenhum mod externo encontrado em benchmark/mods/. Pulando teste comparativo de mods externos." -ForegroundColor Yellow
    }

    # ==============================================================
    # TESTE 3: NOSSO MOD DE OTIMIZACAO
    # ==============================================================
    $modOutput = "$resultsDir/telemetry_modotimizacao.csv"
    $modFpsOutput = "$resultsDir/fps_modotimizacao.csv"
    Run-BenchmarkInstance -TestName "3. Minecraft + Nosso Mod de Otimizacao" -OutputFile $modOutput -FpsOutputFile $modFpsOutput -BeforeLaunch {
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
    if (Test-Path "build/resources/main/fabric.mod.json.disabled") {
        Rename-Item -Path "build/resources/main/fabric.mod.json.disabled" -NewName "fabric.mod.json" -Force
    }

    # Limpeza de residuos: evita que sessoes seguintes do usuario carreguem
    # o fps-recorder ou o mundo de benchmark sem saber (contaminacao de benchmark anterior)
    if (Test-Path "run/mods/fps-recorder-1.0.0.jar") {
        Remove-Item -Path "run/mods/fps-recorder-1.0.0.jar" -Force -ErrorAction SilentlyContinue
    }
    if (Test-Path "run/saves/benchmark_world") {
        Remove-Item -Path "run/saves/benchmark_world" -Recurse -Force -ErrorAction SilentlyContinue
    }
    if (Test-Path "run/benchmark_fps.csv") {
        Remove-Item -Path "run/benchmark_fps.csv" -Force -ErrorAction SilentlyContinue
    }
    Write-Host "[Benchmark] Limpeza de residuos concluida (run/mods e run/saves restaurados)." -ForegroundColor Gray
}

# ==============================================================
# PROCESSAMENTO DE DADOS E RELATORIO FINAL
# ==============================================================
Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "  PROCESSANDO DADOS E GERANDO BENCHMARK_RESULTS.md...           " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

function Get-Averages {
    param (
        [string]$CsvPath,
        [string]$FpsCsvPath
    )
    if (-not (Test-Path $CsvPath)) { return $null }
    $data = Import-Csv -Path $CsvPath -Encoding UTF8
    if (-not $data -or $data.Count -eq 0) { return $null }

    $avgCpu = [math]::Round(($data | Measure-Object -Property CpuPercent -Average).Average, 1)
    $maxRam = [math]::Round(($data | Measure-Object -Property RamMB -Maximum).Maximum, 1)
    $avgGpu = [math]::Round(($data | Measure-Object -Property GpuPercent -Average).Average, 1)
    $maxVram = [math]::Round(($data | Measure-Object -Property VramMB -Maximum).Maximum, 1)

    $avgFps = "-"
    $onePercentLow = "-"
    if ($FpsCsvPath -and (Test-Path $FpsCsvPath)) {
        $fpsData = Import-Csv -Path $FpsCsvPath -Encoding UTF8
        if ($fpsData -and $fpsData.Count -gt 0) {
            $fpsList = $fpsData | ForEach-Object {
                $val = 0.0
                if ([double]::TryParse($_.InstantFps, [System.Globalization.NumberStyles]::Any, [System.Globalization.CultureInfo]::InvariantCulture, [ref]$val) -and $val -gt 0) {
                    $val
                } elseif ([double]::TryParse($_.Fps, [System.Globalization.NumberStyles]::Any, [System.Globalization.CultureInfo]::InvariantCulture, [ref]$val) -and $val -gt 0) {
                    $val
                }
            } | Where-Object { $_ -gt 0 } | Sort-Object

            if ($fpsList -and $fpsList.Count -gt 0) {
                $avgFps = [math]::Round(($fpsList | Measure-Object -Average).Average, 1)
                # 1% Low = media dos 1% piores momentos (lista ordenada crescente)
                $lowCount = [math]::Max(1, [math]::Floor($fpsList.Count * 0.01))
                $lowSamples = @($fpsList | Select-Object -First $lowCount)
                $onePercentLow = [math]::Round(($lowSamples | Measure-Object -Average).Average, 1)
            }
        }
    }

    return [PSCustomObject]@{
        AvgCpu        = $avgCpu
        MaxRam        = $maxRam
        AvgGpu        = $avgGpu
        MaxVram       = $maxVram
        AvgFps        = $avgFps
        OnePercentLow = $onePercentLow
        Samples       = $data.Count
    }
}

$vanillaStats = Get-Averages -CsvPath $vanillaOutput -FpsCsvPath $vanillaFpsOutput
$sodiumStats  = Get-Averages -CsvPath $sodiumOutput  -FpsCsvPath $sodiumFpsOutput
$modStats     = Get-Averages -CsvPath $modOutput     -FpsCsvPath $modFpsOutput

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

## 📊 Tabela Comparativa de Desempenho Completo

| Configuracao | Amostras | FPS Medio | 1% Lows (FPS) | CPU Media (%) | RAM Maxima (MB) | GPU Media (%) | VRAM Maxima (MB) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
"@

if ($vanillaStats) {
    $report += "`n| **1. Minecraft Vanilla Puro** | $($vanillaStats.Samples)s | **$($vanillaStats.AvgFps)** | **$($vanillaStats.OnePercentLow)** | $($vanillaStats.AvgCpu)% | $($vanillaStats.MaxRam) MB | $($vanillaStats.AvgGpu)% | $($vanillaStats.MaxVram) MB |"
}
if ($sodiumStats) {
    $report += "`n| **2. Minecraft + Mods ($modNames)** | $($sodiumStats.Samples)s | **$($sodiumStats.AvgFps)** | **$($sodiumStats.OnePercentLow)** | $($sodiumStats.AvgCpu)% | $($sodiumStats.MaxRam) MB | $($sodiumStats.AvgGpu)% | $($sodiumStats.MaxVram) MB |"
}
if ($modStats) {
    $report += "`n| **3. Minecraft + Nosso Mod** | $($modStats.Samples)s | **$($modStats.AvgFps)** | **$($modStats.OnePercentLow)** | $($modStats.AvgCpu)% | $($modStats.MaxRam) MB | $($modStats.AvgGpu)% | $($modStats.MaxVram) MB |"
}

$report += @"


---

## 💡 Analise Tecnica e Conclusoes
* **FPS Medio:** Taxa media de quadros por segundo renderizados pelo cliente no mundo. Maior valor indica maior capacidade de processamento grafico.
* **1% Lows (Estabilidade):** Metrica fundamental de suavidade visual. Representa a media dos 1% piores momentos de renderizacao. Valores mais altos significam eliminacao de engasgos (*stuttering*) e frametimes consistentes.
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
