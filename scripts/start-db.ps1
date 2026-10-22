# 启动项目专用的 MySQL 实例（端口 3307，数据目录不在中文路径下）
# 说明：Windows 服务 MySQL80 指向缺失的 my.ini 且启动需要管理员权限，故使用独立实例。

$ErrorActionPreference = 'Stop'

$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
$dataDir = 'C:\campus-market-mysql\data'
$port = 3307

if (-not (Test-Path (Join-Path $mysqlBin 'mysqld.exe'))) {
    throw "未找到 mysqld.exe，请确认 MySQL 安装路径：$mysqlBin"
}

if (-not (Test-Path $dataDir)) {
    Write-Host "数据目录不存在，正在初始化 $dataDir ..."
    New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
    & (Join-Path $mysqlBin 'mysqld.exe') "--initialize-insecure" "--datadir=$dataDir"
}

$listening = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
if ($listening) {
    Write-Host "MySQL 已在端口 $port 运行。"
    exit 0
}

Write-Host "启动 MySQL（端口 $port）..."
$stdout = 'C:\campus-market-mysql\stdout.log'
$stderr = 'C:\campus-market-mysql\stderr.log'
$proc = Start-Process -FilePath (Join-Path $mysqlBin 'mysqld.exe') `
    -WorkingDirectory $mysqlBin `
    -ArgumentList "--datadir=$dataDir", "--port=$port", "--skip-mysqlx" `
    -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr `
    -PassThru -WindowStyle Hidden

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    & (Join-Path $mysqlBin 'mysqladmin.exe') "-u" "root" "-h" "127.0.0.1" "-P" "$port" "ping" 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "MySQL 已就绪：127.0.0.1:$port（PID $($proc.Id)）"
        exit 0
    }
}

Write-Host "启动失败，请检查 $stderr"
exit 1
