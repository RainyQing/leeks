# 一键发布说明

通过 `release.ps1` 脚本自动完成：打包 → 打 tag → 推送 → 创建 GitHub Release（自动上传 zip）。

## 发布步骤

**第 1 步：修改版本号**

`build.gradle` 第 7 行：

```groovy
version '2.9.9'   // 改成新版本号
```

**第 2 步：写改动记录**

`README.md` 最下方的 `## change` 章节末尾追加（格式必须严格如下）：

```markdown
- v2.9.9
    * 改动内容1
    * 改动内容2  from [RainyQing](https://github.com/RainyQing)
```

> 脚本会自动提取 `*` 条目，去掉 `from [xxx]` 署名，转成 `1. 2. 3.` 编号作为 Release notes。

**第 3 步：提交代码**

```powershell
git add .
git commit -m "fix : 修复插件兼容IDEA xxxx"
```

**第 4 步：运行发布脚本**

```powershell
.\release.ps1
```

完事。Release 地址：https://github.com/RainyQing/leeks/releases

## 其他用法

```powershell
.\release.ps1 -DryRun          # 演练模式：只打印版本号和 Release notes，不实际执行（发布前建议先跑一遍检查）
.\release.ps1 -Version 2.9.9   # 手动指定版本号（默认自动从 build.gradle 读取）
```

## 常见问题

| 报错 | 原因 | 解决 |
|---|---|---|
| 有未提交的改动 | 工作区不干净 | 先提交；`.idea/` 被 IDE 自动改写的话用 `git restore .idea/` 还原 |
| 本地落后于远端 | 远端有新提交（如合并了 PR） | `git pull --rebase` 后重新运行 |
| tag vx.x.x 已存在 | 重复发布 | 确需重发：`git tag -d vx.x.x` 然后 `git push origin :refs/tags/vx.x.x`，再到 GitHub 删掉对应 Release |
| README 中未找到改动记录 | 第 2 步格式不对 | 检查是不是 `- v2.9.9`（带 v）加缩进的 `*` 条目 |
| 打包失败 | 环境问题 | 检查 build.gradle 中 `localPath` 是否指向本机 IDEA 安装目录 |

## 注意事项

- 首次运行如提示执行策略限制，用：`powershell -ExecutionPolicy Bypass -File .\release.ps1`
- gh 未登录时会自动复用 git 的 GitHub 凭据，无需单独配置
- 脚本只发布，不提交代码；提交必须手动完成（第 3 步）
