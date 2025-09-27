# JDK 17 安裝指南

## 🎯 推薦安裝方式

### 1. Oracle JDK 17 (官方版本)
- **下載地址**: https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
- **版本**: JDK 17.0.9 (LTS)
- **安裝**: 下載Windows x64 Installer，雙擊安裝

### 2. OpenJDK 17 (開源版本) - 推薦
- **下載地址**: https://adoptium.net/temurin/releases/?version=17
- **版本**: Eclipse Temurin 17.0.9+11 (LTS)
- **安裝**: 下載.msi文件，雙擊安裝

### 3. 使用包管理器安裝

#### 使用 Chocolatey:
```powershell
# 安裝Chocolatey (如果沒有)
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# 安裝OpenJDK 17
choco install openjdk17
```

#### 使用 Scoop:
```powershell
# 安裝Scoop (如果沒有)
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# 安裝OpenJDK 17
scoop install openjdk17
```

## 🔧 安裝後配置

### 1. 設置JAVA_HOME環境變量
```powershell
# 設置JAVA_HOME (替換為實際安裝路徑)
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.11-hotspot", "Machine")

# 更新PATH
$currentPath = [Environment]::GetEnvironmentVariable("PATH", "Machine")
$newPath = $currentPath + ";C:\Program Files\Eclipse Adoptium\jdk-17.0.9.11-hotspot\bin"
[Environment]::SetEnvironmentVariable("PATH", $newPath, "Machine")
```

### 2. 驗證安裝
```powershell
java -version
# 應該顯示: openjdk version "17.0.9" 2023-10-17
```

### 3. 配置Gradle使用JDK 17
在項目根目錄創建 `gradle.properties`:
```properties
org.gradle.java.home=C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.9.11-hotspot
```

## 🚀 安裝完成後的測試步驟

1. **驗證Java版本**:
   ```powershell
   java -version
   javac -version
   ```

2. **測試項目編譯**:
   ```powershell
   .\gradlew clean build
   ```

3. **運行測試**:
   ```powershell
   .\gradlew test
   ```

4. **運行應用程序**:
   ```powershell
   .\gradlew run
   ```

## 🔄 多版本JDK管理

### 使用 jEnv (推薦)
```powershell
# 安裝jEnv
git clone https://github.com/jenv/jenv.git ~/.jenv

# 添加JDK版本
jenv add "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.11-hotspot"
jenv add "C:\Program Files\Java\jdk1.8.0_202"

# 設置項目JDK版本
jenv local 17
```

### 使用 SDKMAN
```powershell
# 安裝SDKMAN
curl -s "https://get.sdkman.io" | bash

# 安裝JDK 17
sdk install java 17.0.9-tem

# 設置默認版本
sdk default java 17.0.9-tem
```

## ⚠️ 注意事項

1. **備份當前項目**: 確保當前項目已提交到Git
2. **IDE配置**: 更新IDE (IntelliJ IDEA, Eclipse, VS Code) 的JDK設置
3. **環境變量**: 確保JAVA_HOME和PATH正確設置
4. **防火牆**: 某些企業環境可能需要配置防火牆規則

## 🆘 故障排除

### 問題1: "java不是內部或外部命令"
- **解決方案**: 檢查PATH環境變量是否包含JDK的bin目錄

### 問題2: Gradle找不到JDK 17
- **解決方案**: 在gradle.properties中設置org.gradle.java.home

### 問題3: IDE無法識別JDK 17
- **解決方案**: 在IDE設置中重新配置Project SDK

### 問題4: 編譯錯誤
- **解決方案**: 清理並重新構建項目
  ```powershell
  .\gradlew clean
  .\gradlew build
  ```
