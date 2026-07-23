# 一键发布脚本：打 tag、推送 tag、打包并创建 GitHub Release
#
# 使用前准备（需手动完成）：
#   1. 修改 build.gradle 中的 version（如 '2.9.9'）
#   2. 在 README.md 的 change 章节添加对应版本的改动记录（格式：- v2.9.9 换行后 * 条目）
#   3. 提交代码
#
# 用法：
#   .\release.ps1            # 正常发布
#   .\release.ps1 -DryRun    # 演练模式，只打印将要执行的操作和 Release notes，不实际执行
#   .\release.ps1 -Version 2.9.9   # 手动指定版本号（默认从 build.gradle 读取）

param(
    [string]$Version,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Set-Location $PSScriptRoot

# 1. 读取版本号（默认从 build.gradle 解析，匹配行首的 version 行，避免误匹配 plugins 块）
if (-not $Version) {
    $gradle = Get-Content build.gradle -Raw -Encoding UTF8
    if ($gradle -match "(?m)^version '([^']+)'") {
        $Version = $Matches[1]
    } else {
        throw "无法从 build.gradle 解析版本号，请使用 -Version 参数指定"
    }
}
$tag = "v$Version"
Write-Host "==> 发布版本: $tag"

# 2. 从 README.md 的 change 章节提取该版本的改动记录，作为 Release notes
$readme = Get-Content README.md -Encoding UTF8
$notes = @()
$inSection = $false
foreach ($line in $readme) {
    if ($line -match ("^- " + [regex]::Escape($tag) + "\s*$")) { $inSection = $true; continue }
    if ($inSection -and $line -match '^- v') { break }
    if ($inSection -and $line -match '^\s*\*\s+(.+)$') {
        $text = ($Matches[1] -replace '\s+from \[.*$', '').Trim()
        if ($text) { $notes += $text }
    }
}
if ($notes.Count -eq 0) { throw "README.md 的 change 章节中未找到 `- $tag` 的改动记录" }
$body = ($notes | ForEach-Object -Begin { $i = 1 } -Process { "{0}. {1}" -f $i++, $_ }) -join "`n"
Write-Host "==> Release notes:"
Write-Host $body

# 3. 检查是否有未提交的改动（忽略未跟踪文件）
$dirty = git status --porcelain --untracked-files=no
if ($dirty) { throw "有未提交的改动，请先提交：`n$dirty" }

# 4. 与远端同步检查：落后则报错，领先则推送
git fetch origin | Out-Null
$local = git rev-parse master
$remote = git rev-parse origin/master
if ($local -ne $remote) {
    $base = git merge-base master origin/master
    if ($local -eq $base) { throw "本地落后于远端，请先 pull/rebase 再发布" }
    if ($remote -ne $base) { throw "本地与远端已分叉，请先手动处理" }
    Write-Host "==> 推送 master"
    if (-not $DryRun) { git push origin master }
}

# 5. 检查 tag 是否已存在
if (git tag -l $tag) { throw "tag $tag 已存在，如需重发请先删除: git tag -d $tag; git push origin :refs/tags/$tag" }

# 6. 打包
$zip = "build\distributions\leeks-$Version.zip"
Write-Host "==> 打包: gradlew buildPlugin"
if (-not $DryRun) {
    & .\gradlew.bat buildPlugin --console=plain
    if ($LASTEXITCODE -ne 0) { throw "打包失败" }
    if (-not (Test-Path $zip)) { throw "未找到打包产物 $zip" }
}

# 7. 打 tag 并推送
Write-Host "==> 创建并推送 tag $tag"
if (-not $DryRun) {
    git tag -a $tag -m $tag
    git push origin $tag
}

# 8. 准备 gh：优先使用已登录的 gh，否则复用 git 凭据管理器中的 token
$gh = (Get-Command gh -ErrorAction SilentlyContinue).Source
if (-not $gh) { $gh = "C:\Program Files\GitHub CLI\gh.exe" }
& $gh auth status 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    $credOut = "protocol=https`nhost=github.com`n`n" | git credential fill
    $env:GH_TOKEN = ($credOut | Where-Object { $_ -like 'password=*' }) -replace '^password=',''
}

# 9. 创建 GitHub Release 并上传 zip
Write-Host "==> 创建 GitHub Release V$Version"
if (-not $DryRun) {
    & $gh release create $tag --title "V$Version" --notes $body $zip
    if ($LASTEXITCODE -ne 0) { throw "Release 创建失败" }
}

Write-Host "==> 发布完成: $tag"
