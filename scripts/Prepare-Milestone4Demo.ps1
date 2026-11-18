param(
    [string]$BackendUrl = "http://localhost:8062"
)

$ErrorActionPreference = "Stop"
# Use the existing provider API so all ownership and duration checks still apply.
$login = Invoke-RestMethod -Method Post -Uri "$BackendUrl/api/auth/login" `
    -ContentType "application/json" `
    -Body '{"username":"alice@sjsu.edu","password":"1234"}' -SessionVariable providerSession
$services = Invoke-RestMethod -Uri "$BackendUrl/api/services"
$algebra = $services | Where-Object { $_.providerId -eq $login.providerId -and $_.name -eq "Algebra" } | Select-Object -First 1
if (-not $algebra) { throw "Alice's Algebra service was not found. Check seed.sql sample accounts." }
$start = (Get-Date).AddHours(2)
$end = $start.AddMinutes($algebra.durationMinutes)
$body = @{
    serviceId = $algebra.id
    startTime = $start.ToString("yyyy-MM-ddTHH:mm:ss")
    endTime = $end.ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$BackendUrl/api/provider/slots" `
    -ContentType "application/json" -Body $body -WebSession $providerSession | Out-Null
Write-Output "Created Alice's Algebra slot at $($start.ToString('yyyy-MM-dd HH:mm'))."
Write-Output "Log in as John in the browser. Ask the assistant to book this time, then press Confirm."
Write-Output "Keep it booked while you show the reminder agent. Cancel it afterward if desired."
