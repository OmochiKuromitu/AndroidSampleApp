#Requires -Version 7.0
<##
ローカル解析をまとめて実行する。MobSF は先に起動し、MOBSF_API_KEY を設定する。
使い方: pwsh -File scripts/static-analysis.ps1
Gradle の解析だけ: pwsh -File scripts/static-analysis.ps1 -SkipMobSF
##>
[CmdletBinding()]
param([switch]$SkipMobSF)

$ErrorActionPreference = 'Stop'
# 各ツールの終了コードを確認し、失敗しても独立した解析は続ける。
$PSNativeCommandUseErrorActionPreference = $false
$failures = [System.Collections.Generic.List[string]]::new()
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    $gradle = if ($IsWindows) { '.\gradlew.bat' } else { './gradlew' }
    & $gradle lintMockDebug detekt jacocoMockDebugUnitTestReport --continue --console=plain
    if ($LASTEXITCODE -ne 0) { $failures.Add('Lint / detekt / 単体テスト・JaCoCo（Gradle のログを確認）') }

    if (-not $SkipMobSF) {
        & $gradle assembleProductRelease --console=plain
        if ($LASTEXITCODE -ne 0) {
            $failures.Add('productRelease のビルド（MobSF は実行できませんでした）')
        } else {
            try {
                & "$PSScriptRoot/mobsf-scan.ps1" -ApkPath 'app/build/outputs/apk/product/release/app-product-release-unsigned.apk'
            } catch {
                $failures.Add("MobSF: $($_.Exception.Message)")
            }
        }
    }

    Write-Host 'Gradle レポート: app/build/reports/'
    if (-not $SkipMobSF) { Write-Host 'MobSF レポート: build/reports/mobsf/' }
    if ($failures.Count -gt 0) {
        foreach ($failure in $failures) { Write-Warning $failure }
        exit 1
    }
    Write-Host '指定した解析が完了しました。'
} finally {
    Pop-Location
}
