# JDK 17 自動安裝腳本
# 這個腳本會自動下載並安裝 OpenJDK 17

Write-Host "🚀 開始安裝 OpenJDK 17..." -ForegroundColor Green

# 設置下載URL和路徑
$downloadUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B11/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_11.msi"
$tempDir = $env:TEMP
$msiFile = Join-Path $tempDir "OpenJDK17.msi"

Write-Host "📥 正在下載 OpenJDK 17..." -ForegroundColor Yellow
Write-Host "下載地址: $downloadUrl" -ForegroundColor Gray

try {
    # 下載MSI文件
    Invoke-WebRequest -Uri $downloadUrl -OutFile $msiFile -UseBasicParsing
    Write-Host "✅ 下載完成: $msiFile" -ForegroundColor Green
    
    Write-Host "🔧 正在安裝 OpenJDK 17..." -ForegroundColor Yellow
    
    # 靜默安裝MSI
    $arguments = @(
        "/i", $msiFile,
        "/quiet",
        "/norestart",
        "INSTALLDIR=C:\Program Files\Eclipse Adoptium\jdk-17.0.9.11-hotspot"
    )
    
    Start-Process -FilePath "msiexec.exe" -ArgumentList $arguments -Wait
    
    Write-Host "✅ OpenJDK 17 安裝完成!" -ForegroundColor Green
    
    # 設置環境變量
    Write-Host "🔧 正在設置環境變量..." -ForegroundColor Yellow
    
    $javaHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.11-hotspot"
    
    # 設置JAVA_HOME
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "Machine")
    Write-Host "✅ JAVA_HOME 設置為: $javaHome" -ForegroundColor Green
    
    # 更新PATH
    $currentPath = [Environment]::GetEnvironmentVariable("PATH", "Machine")
    $javaBin = Join-Path $javaHome "bin"
    
    if ($currentPath -notlike "*$javaBin*") {
        $newPath = $currentPath + ";$javaBin"
        [Environment]::SetEnvironmentVariable("PATH", $newPath, "Machine")
        Write-Host "✅ PATH 已更新，包含 JDK 17 bin 目錄" -ForegroundColor Green
    }
    
    # 清理臨時文件
    Remove-Item $msiFile -Force -ErrorAction SilentlyContinue
    
    Write-Host "🎉 JDK 17 安裝完成!" -ForegroundColor Green
    Write-Host "請重新啟動 PowerShell 或命令提示符以使用新的環境變量" -ForegroundColor Yellow
    Write-Host "或者運行以下命令刷新環境變量:" -ForegroundColor Yellow
    Write-Host "`$env:JAVA_HOME = `"$javaHome`"" -ForegroundColor Cyan
    Write-Host "`$env:PATH = `"`$env:PATH;$javaBin`"" -ForegroundColor Cyan
    
} catch {
    Write-Host "❌ 安裝失敗: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "請手動下載並安裝 JDK 17:" -ForegroundColor Yellow
    Write-Host "https://adoptium.net/temurin/releases/?version=17" -ForegroundColor Cyan
}

Write-Host "按任意鍵繼續..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
