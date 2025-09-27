# 設置JDK 17環境變量
Write-Host "🔧 設置JDK 17環境變量..." -ForegroundColor Yellow

$javaHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
$javaBin = "$javaHome\bin"

# 設置JAVA_HOME
$env:JAVA_HOME = $javaHome
Write-Host "✅ JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Green

# 更新PATH
$env:PATH = "$javaBin;$env:PATH"
Write-Host "✅ PATH已更新，包含JDK 17 bin目錄" -ForegroundColor Green

# 驗證安裝
Write-Host "🔍 驗證JDK 17安裝..." -ForegroundColor Yellow
Write-Host "Java版本:" -ForegroundColor Cyan
java -version

Write-Host "`n🔍 驗證javac編譯器:" -ForegroundColor Yellow
javac -version

Write-Host "`n✅ JDK 17環境設置完成！" -ForegroundColor Green
