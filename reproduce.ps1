$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
mvn test
if ($LASTEXITCODE -ne 0) { throw "Tests failed" }
java -Xmx2g -cp target/classes benchmark.Benchmark
if ($LASTEXITCODE -ne 0) { throw "Benchmark failed" }
python scripts/plots.py
if ($LASTEXITCODE -ne 0) { throw "Plot generation failed" }
python scripts/report.py
if ($LASTEXITCODE -ne 0) { throw "Report generation failed" }
