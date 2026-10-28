#!/usr/bin/env bash
# MobSF（Mobile Security Framework）に APK を送って静的解析し、レポートを保存する。
#
# 使い方:
#   MOBSF_API_KEY=<API キー> scripts/mobsf-scan.sh <APK のパス> [出力先ディレクトリ]
#
# 環境変数:
#   MOBSF_API_KEY  MobSF の REST API キー（必須）。Docker で起動するときに同じ値を -e MOBSF_API_KEY で渡す
#   MOBSF_URL      MobSF の URL（既定 http://localhost:8000）
#   MOBSF_WAIT_SEC MobSF が起動するまで待つ秒数（既定 300）
#
# 出力:
#   report.json     解析結果のすべて
#   report.pdf      人が読む用のレポート
#   scorecard.json  セキュリティスコアと、重要度ごとの指摘
#
# 詳しくは docs/static-analysis.md の「MobSF」。
set -euo pipefail

apk="${1:?APK のパスを指定してください（例: app/build/outputs/apk/mock/debug/app-mock-debug.apk）}"
out="${2:-build/reports/mobsf}"
url="${MOBSF_URL:-http://localhost:8000}"
key="${MOBSF_API_KEY:?MOBSF_API_KEY を設定してください}"
wait_sec="${MOBSF_WAIT_SEC:-300}"

[[ -f "$apk" ]] || { echo "APK が見つかりません: $apk" >&2; exit 1; }
command -v jq >/dev/null || { echo "jq が必要です" >&2; exit 1; }
mkdir -p "$out"

# 起動直後は応答しないので、トップページが返るまで待つ。
echo "MobSF の起動を待っています: $url"
for ((i = 0; i < wait_sec; i += 5)); do
  code="$(curl -s -o /dev/null -w '%{http_code}' "$url/" || true)"
  [[ "$code" == "200" || "$code" == "302" ]] && break
  sleep 5
done
[[ "$code" == "200" || "$code" == "302" ]] || { echo "MobSF が ${wait_sec} 秒以内に起動しませんでした" >&2; exit 1; }

api() {
  # $1: API のパス、残り: curl に渡す引数
  local path="$1"
  shift
  curl -fsS -H "Authorization: $key" "$url/api/v1/$path" "$@"
}

echo "アップロード: $apk"
hash="$(api upload -F "file=@$apk" | jq -r '.hash')"
[[ -n "$hash" && "$hash" != "null" ]] || { echo "アップロードに失敗しました" >&2; exit 1; }

echo "解析中（数分かかります）: $hash"
api scan --data "hash=$hash" >/dev/null

api report_json --data "hash=$hash" -o "$out/report.json"
api scorecard --data "hash=$hash" -o "$out/scorecard.json"
# PDF は MobSF の中で wkhtmltopdf を使って作る。作れなくても JSON は残っているので止めない。
api download_pdf --data "hash=$hash" -o "$out/report.pdf" || echo "PDF を作れませんでした（JSON は保存済み）" >&2

score="$(jq -r '.security_score // "?"' "$out/scorecard.json")"
high="$(jq -r '(.high // []) | length' "$out/scorecard.json")"
warning="$(jq -r '(.warning // []) | length' "$out/scorecard.json")"
echo "セキュリティスコア: $score / 100（high: $high 件、warning: $warning 件）"
echo "レポート: $out"
