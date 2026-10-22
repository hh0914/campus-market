# 停止项目专用的 MySQL 实例（端口 3307）
$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
$port = 3307

& (Join-Path $mysqlBin 'mysqladmin.exe') "-u" "root" "-h" "127.0.0.1" "-P" "$port" "shutdown"
if ($LASTEXITCODE -eq 0) {
    Write-Host "MySQL（端口 $port）已停止。"
}
else {
    Write-Host "未检测到运行中的实例，或停止失败。"
}
