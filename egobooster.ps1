$src = Join-Path $PSScriptRoot 'src'

Get-ChildItem -Recurse -Path $src -Filter *.java |
    Get-Content |
    Measure-Object -Line
