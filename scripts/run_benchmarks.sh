#!/usr/bin/env bash
set -e

echo "=========================================================="
echo " ChangeImpact: Reproducible Benchmark & Evaluation Suite"
echo "=========================================================="

OUTPUT_CSV="${1:-benchmarks/results/benchmark_results.csv}"
OUTPUT_REPORT="${2:-benchmarks/results/benchmark_report.md}"

echo -e "\n[1/3] Verifying Java Runtime..."
java -version

if [ ! -f "target/changeimpact-engine-1.0.0.jar" ]; then
    echo -e "\n[2/3] Building ChangeImpact Engine with Maven..."
    mvn clean package -DskipTests
else
    echo -e "\n[2/3] Found existing shaded JAR at target/changeimpact-engine-1.0.0.jar"
fi

echo -e "\n[3/3] Executing Benchmark Matrix..."
java -jar target/changeimpact-engine-1.0.0.jar benchmark --output-csv "$OUTPUT_CSV" --output-report "$OUTPUT_REPORT" --visualize

echo -e "\nEvaluation complete!"
echo "  * CSV Results:   $OUTPUT_CSV"
echo "  * Markdown:      $OUTPUT_REPORT"
echo "  * Charts:        benchmarks/results/charts/"
