$ErrorActionPreference = 'Stop'
$classes = Join-Path $PSScriptRoot 'target/regression-classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null

$sourceFolders = @(
    (Join-Path $PSScriptRoot 'src/main/java'),
    (Join-Path $PSScriptRoot 'src/test/java')
)
$sources = @(Get-ChildItem -LiteralPath $sourceFolders -Recurse -Filter '*.java' |
    ForEach-Object { $_.FullName })

& javac --release 17 -encoding UTF-8 -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }

$resources = Join-Path $PSScriptRoot 'src/main/resources'
$classPath = $classes + [IO.Path]::PathSeparator + $resources
$tests = @(
    'hearthstone.engine.EngineSmokeTest',
    'hearthstone.engine.DefenseRuleSmokeTest',
    'hearthstone.engine.ExhaustionSmokeTest',
    'hearthstone.ui.EndGameSmokeTest'
)
foreach ($test in $tests) {
    & java -cp $classPath $test
    if ($LASTEXITCODE -ne 0) { throw "Test failed: $test" }
}

$data = Join-Path $PSScriptRoot ('target/persistence-test-' + [guid]::NewGuid().ToString('N'))
& java -cp $classPath hearthstone.persistence.PersistenceSmokeTest $data
if ($LASTEXITCODE -ne 0) { throw 'Persistence test failed' }
Write-Output 'ALL_TESTS_PASSED'
