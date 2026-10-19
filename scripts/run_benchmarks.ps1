# ChangeImpact Automated Reproducible Benchmark Script
param (
    [string]$OutputCsv = "benchmarks/results/benchmark_results.csv",
    [string]$OutputReport = "benchmarks/results/benchmark_report.md",
    [switch]$Visualize
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " ChangeImpact: Reproducible Benchmark & Evaluation Suite" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Check Java version
Write-Host "`n[1/3] Verifying Java Runtime..." -ForegroundColor Yellow
java -version
if ($LASTEXITCODE -ne 0) {
    Write-Error "Java 21 or compatible LTS JDK is required."
    exit 1
}

# 2. Build jar if missing
if (-not (Test-Path "target/changeimpact-engine-1.0.0.jar")) {
    Write-Host "`n[2/3] Building ChangeImpact Engine with Maven..." -ForegroundColor Yellow
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven build failed."
        exit 1
    }
} else {
    Write-Host "`n[2/3] Found existing shaded JAR at target/changeimpact-engine-1.0.0.jar" -ForegroundColor Green
}

# 3. Execute Benchmarks
Write-Host "`n[3/3] Executing Benchmark Matrix..." -ForegroundColor Yellow
$visFlag = ""
if ($Visualize) {
    $visFlag = "--visualize"
}

java -jar target/changeimpact-engine-1.0.0.jar benchmark --output-csv "$OutputCsv" --output-report "$OutputReport" $visFlag

Write-Host "`nEvaluation complete! Output files:" -ForegroundColor Green
Write-Host "  * CSV Results:   $OutputCsv"
Write-Host "  * Markdown:      $OutputReport"
if ($Visualize) {
    Write-Host "  * Charts:        benchmarks/results/charts/"
}
