@echo off
echo ========================================================
echo  Iniciando Minecraft 26.3 no modo 100%% VANILLA (Sem Mod)
echo ========================================================

rem Auto-reparo: se uma execucao anterior travou antes de restaurar, restaura agora
if exist src\main\resources\fabric.mod.json.disabled if not exist src\main\resources\fabric.mod.json (
    ren src\main\resources\fabric.mod.json.disabled fabric.mod.json
    echo [runVanilla] Sessao anterior interrompida foi reparada automaticamente.
)
if exist build\resources\main\fabric.mod.json.disabled if not exist build\resources\main\fabric.mod.json (
    ren build\resources\main\fabric.mod.json.disabled fabric.mod.json
)

if exist src\main\resources\fabric.mod.json (
    ren src\main\resources\fabric.mod.json fabric.mod.json.disabled
)
rem Residuo compilado: sem isto, o cliente dev pode carregar o mod mesmo em modo vanilla
if exist build\resources\main\fabric.mod.json (
    ren build\resources\main\fabric.mod.json fabric.mod.json.disabled
)

call .\gradlew.bat runClient

if exist src\main\resources\fabric.mod.json.disabled (
    ren src\main\resources\fabric.mod.json.disabled fabric.mod.json
)
if exist build\resources\main\fabric.mod.json.disabled (
    ren build\resources\main\fabric.mod.json.disabled fabric.mod.json
)

echo ========================================================
echo  Sessao vanilla encerrada. Mod restaurado com sucesso!
echo ========================================================
