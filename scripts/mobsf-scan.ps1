#Requires -Version 7.0
# 起動済みのローカル MobSF に APK を送り、レポートを保存する。
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$ApkPath,
    [string]$OutputDirectory = 'build/reports/mobsf',
    [string]$Url = $(if ($env:MOBSF_URL) { $env:MOBSF_URL } else { 'http://localhost:8000' }),
    [ValidateRange(1, 3600)][int]$WaitSeconds = 300
)

$ErrorActionPreference = 'Stop'
if (-not $env:MOBSF_API_KEY) { throw 'MOBSF_API_KEY を設定してください。' }
$apk = Get-Item -LiteralPath $ApkPath
if ($apk.PSIsContainer) { throw 'APK ファイルを指定してください。' }
$Url = $Url.TrimEnd('/')
$headers = @{ Authorization = $env:MOBSF_API_KEY }
$deadline = [DateTime]::UtcNow.AddSeconds($WaitSeconds)
$ready = $false
Write-Host "MobSF の起動を待っています: $Url"
while ([DateTime]::UtcNow -lt $deadline) {
    try {
        $null = Invoke-WebRequest -Uri "$Url/" -TimeoutSec 5
        $ready = $true
        break
    } catch {
        Start-Sleep -Seconds 5
    }
}
if (-not $ready) { throw "MobSF が $WaitSeconds 秒以内に起動しませんでした。" }

# 実行ごとに保存先を分け、過去の成功レポートと今回の失敗を混同しないようにする。
$runDirectory = Join-Path $OutputDirectory ([DateTime]::Now.ToString('yyyyMMdd-HHmmss-fff'))
$null = New-Item -ItemType Directory -Path $runDirectory -Force
$upload = Invoke-RestMethod -Uri "$Url/api/v1/upload" -Method Post -Headers $headers -Form @{ file = $apk } -TimeoutSec 120
if (-not $upload.hash) { throw 'APK のアップロードに失敗しました（hash がありません）。' }
$body = @{ hash = $upload.hash }
Write-Host 'MobSF で解析しています。'
$scan = Invoke-RestMethod -Uri "$Url/api/v1/scan" -Method Post -Headers $headers -Body $body -TimeoutSec 900
if ($scan.error) { throw "MobSF の解析に失敗しました: $($scan.error)" }
foreach ($report in @('report_json', 'scorecard')) {
    $result = Invoke-RestMethod -Uri "$Url/api/v1/$report" -Method Post -Headers $headers -Body $body -TimeoutSec 120
    if ($result.error) { throw "MobSF のレポート取得に失敗しました: $($result.error)" }
    $name = if ($report -eq 'report_json') { 'report.json' } else { 'scorecard.json' }
    $result | ConvertTo-Json -Depth 100 | Set-Content -LiteralPath (Join-Path $runDirectory $name) -Encoding utf8
}
try {
    Invoke-WebRequest -Uri "$Url/api/v1/download_pdf" -Method Post -Headers $headers -Body $body -TimeoutSec 120 -OutFile (Join-Path $runDirectory 'report.pdf') | Out-Null
} catch {
    Write-Warning 'PDF を取得できませんでした。JSON レポートは保存済みです。'
}
$scorecard = Get-Content -LiteralPath (Join-Path $runDirectory 'scorecard.json') -Raw | ConvertFrom-Json
$score = if ($null -ne $scorecard.security_score) { $scorecard.security_score } else { '?' }
Write-Host "セキュリティスコア: $score / 100（high: $(@($scorecard.high).Count) 件、warning: $(@($scorecard.warning).Count) 件）"
Write-Host "MobSF レポート: $runDirectory"
