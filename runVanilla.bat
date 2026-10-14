@echo off
echo ========================================================
echo  Iniciando Minecraft 26.3 no modo 100%% VANILLA (Sem Mod)
echo ========================================================

if exist src\main\resources\fabric.mod.json (
    ren src\main\resources\fabric.mod.json fabric.mod.json.disabled
)

call .\gradlew.bat runClient

if exist src\main\resources\fabric.mod.json.disabled (
    ren src\main\resources\fabric.mod.json.disabled fabric.mod.json
)

echo ========================================================
echo  Sessao vanilla encerrada. Mod restaurado com sucesso!
echo ========================================================
