<#
    CareerPilot end-to-end smoke test (Windows / PowerShell).

    Run AFTER every service is up. Mirrors scripts/smoke-test.sh.

    Usage:  powershell -ExecutionPolicy Bypass -File scripts\smoke-test.ps1 [gatewayUrl]
#>
param([string]$Gateway = "http://localhost:8080")

$ErrorActionPreference = "Continue"
$Api = "$Gateway/api"

$AdminEmail    = if ($env:ADMIN_EMAIL)    { $env:ADMIN_EMAIL }    else { "admin@careerpilot.com" }
$AdminPassword = if ($env:ADMIN_PASSWORD) { $env:ADMIN_PASSWORD } else { "CareerPilot@123" }

$stamp         = [int][double]::Parse((Get-Date -UFormat %s))
$SeekerEmail   = "seeker.$stamp@example.com"
$EmployerEmail = "employer.$stamp@example.com"
$Password      = 'Test@12345'

$script:Pass = 0; $script:Fail = 0; $script:Skip = 0

function Check($name, $expected, $actual, $detail) {
    if ("$expected" -eq "$actual") {
        Write-Host "  PASS  $name" -ForegroundColor Green; $script:Pass++
    } else {
        Write-Host "  FAIL  $name (expected $expected, got $actual) $detail" -ForegroundColor Red; $script:Fail++
    }
}
function Skip($name, $why) { Write-Host "  SKIP  $name - $why" -ForegroundColor Yellow; $script:Skip++ }

# Returns a hashtable: @{ Status = <int>; Body = <object> }
function Request($method, $path, $body, $token) {
    $headers = @{ "Content-Type" = "application/json" }
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    try {
        $params = @{ Uri = "$Api$path"; Method = $method; Headers = $headers; UseBasicParsing = $true }
        if ($body) { $params["Body"] = $body }
        $r = Invoke-WebRequest @params
        return @{ Status = [int]$r.StatusCode; Body = ($r.Content | ConvertFrom-Json) }
    } catch {
        $status = 0
        if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
        $content = $null
        try { $content = ($_.ErrorDetails.Message | ConvertFrom-Json) } catch { }
        return @{ Status = $status; Body = $content }
    }
}

Write-Host ""
Write-Host "CareerPilot smoke test against $Gateway"
Write-Host "======================================================================"

Write-Host ""; Write-Host "Infrastructure"
foreach ($probe in @(@{n="API Gateway";u="$Gateway/actuator/health"},
                     @{n="Eureka Server";u="http://localhost:8761/"},
                     @{n="Config Server";u="http://localhost:8888/actuator/health"})) {
    try { $c = [int](Invoke-WebRequest -Uri $probe.u -UseBasicParsing).StatusCode } catch { $c = 0 }
    Check "$($probe.n) is reachable" 200 $c ""
}

Write-Host ""; Write-Host "Authentication"
$r = Request POST "/auth/register" "{`"email`":`"$SeekerEmail`",`"password`":`"$Password`",`"firstName`":`"Smoke`",`"lastName`":`"Seeker`",`"phone`":`"9990000001`",`"role`":`"JobSeeker`"}" $null
Check "Job seeker registration" 200 $r.Status $r.Body.message

$r = Request POST "/auth/register" "{`"email`":`"$EmployerEmail`",`"password`":`"$Password`",`"firstName`":`"Smoke`",`"lastName`":`"Employer`",`"phone`":`"9990000002`",`"role`":`"Employer`"}" $null
Check "Employer registration" 200 $r.Status $r.Body.message

$r = Request POST "/auth/register" "{`"email`":`"evil.$stamp@example.com`",`"password`":`"$Password`",`"firstName`":`"E`",`"lastName`":`"V`",`"phone`":`"9990000003`",`"role`":`"Admin`"}" $null
Check "Self-registering as Admin is refused" 400 $r.Status ""

$r = Request POST "/auth/login" "{`"email`":`"$SeekerEmail`",`"password`":`"$Password`"}" $null
Check "Job seeker login" 200 $r.Status ""; $SeekerToken = $r.Body.data.accessToken

$r = Request POST "/auth/login" "{`"email`":`"$EmployerEmail`",`"password`":`"$Password`"}" $null
Check "Employer login" 200 $r.Status ""; $EmployerToken = $r.Body.data.accessToken

$r = Request POST "/auth/login" "{`"email`":`"$AdminEmail`",`"password`":`"$AdminPassword`"}" $null
Check "Admin login" 200 $r.Status $r.Body.message
$AdminToken = $r.Body.data.accessToken
Check "Admin token carries the Admin role" "Admin" $r.Body.data.user.role ""

$r = Request POST "/auth/login" "{`"email`":`"$SeekerEmail`",`"password`":`"wrong-password`"}" $null
Check "Wrong password is rejected" 401 $r.Status ""

Write-Host ""; Write-Host "Job flow"
$r = Request GET "/jobs" $null $null
Check "Public job search needs no token" 200 $r.Status ""
$r = Request POST "/jobs" '{"title":"x","description":"d","requirements":"r","location":"l","jobType":0,"experienceLevel":0}' $SeekerToken
Check "Job seeker cannot create a job" 403 $r.Status ""

Write-Host ""; Write-Host "Admin module"
$r = Request GET "/admin/stats" $null $AdminToken
Check "Admin dashboard stats" 200 $r.Status ""

$r = Request GET "/admin/users" $null $AdminToken
Check "Admin can list users" 200 $r.Status ""
$target = $r.Body.data | Where-Object { $_.email -eq $SeekerEmail } | Select-Object -First 1

if ($target) {
    $r = Request PUT "/admin/users/$($target.id)/deactivate" $null $AdminToken
    Check "Admin can deactivate a user" 200 $r.Status ""
    $r = Request POST "/auth/login" "{`"email`":`"$SeekerEmail`",`"password`":`"$Password`"}" $null
    Check "Deactivated user cannot log in" 401 $r.Status ""
    $r = Request PUT "/admin/users/$($target.id)/activate" $null $AdminToken
    Check "Admin can reactivate a user" 200 $r.Status ""

    $r = Request PUT "/admin/users/$($target.id)/block" '{"reason":"smoke test"}' $AdminToken
    Check "Admin can block a user" 200 $r.Status ""
    $r = Request POST "/auth/login" "{`"email`":`"$SeekerEmail`",`"password`":`"$Password`"}" $null
    Check "Blocked user cannot log in" 401 $r.Status ""
    $r = Request PUT "/admin/users/$($target.id)/unblock" $null $AdminToken
    Check "Admin can unblock a user" 200 $r.Status ""

    $r = Request PUT "/admin/users/$($target.id)/ai/disable" $null $AdminToken
    Check "Admin can revoke a user's AI access" 200 $r.Status ""
    $r = Request PUT "/admin/users/$($target.id)/ai/enable" $null $AdminToken
    Check "Admin can restore a user's AI access" 200 $r.Status ""
} else {
    Skip "Admin user lifecycle checks" "could not resolve the test seeker"
}

$me = Request GET "/users/me" $null $AdminToken
if ($me.Body.data.id) {
    $r = Request PUT "/admin/users/$($me.Body.data.id)/deactivate" $null $AdminToken
    Check "Admin cannot deactivate their own account" 400 $r.Status ""
}

foreach ($p in @("/admin/subscriptions", "/admin/ai-settings", "/applications/admin/all", "/jobs/admin/all", "/companies/admin/all")) {
    $r = Request GET $p $null $AdminToken
    Check "Admin can GET $p" 200 $r.Status ""
}

Write-Host ""; Write-Host "Role isolation"
foreach ($p in @("/admin/stats", "/admin/users", "/admin/subscriptions", "/admin/ai-settings")) {
    $r = Request GET $p $null $SeekerToken;   Check "Job seeker is refused GET $p" 403 $r.Status ""
    $r = Request GET $p $null $EmployerToken; Check "Employer is refused GET $p"   403 $r.Status ""
}
$r = Request GET "/admin/stats" $null $null
Check "Anonymous is refused GET /admin/stats" 401 $r.Status ""

Write-Host ""; Write-Host "AI features"
if (-not $env:GEMINI_API_KEY) {
    Skip "Gemini key check" "GEMINI_API_KEY is not set in this shell"
} else {
    $model = if ($env:GEMINI_MODEL) { $env:GEMINI_MODEL } else { "gemini-3.5-flash" }
    try {
        $g = Invoke-WebRequest -Uri "https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent" `
             -Method POST -UseBasicParsing `
             -Headers @{ "x-goog-api-key" = $env:GEMINI_API_KEY; "Content-Type" = "application/json" } `
             -Body '{"contents":[{"role":"user","parts":[{"text":"Reply with the single word OK"}]}]}'
        $code = [int]$g.StatusCode
    } catch {
        $code = 0
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
    }
    Check "Gemini accepts the configured key (x-goog-api-key header)" 200 $code ""
}

$r = Request POST "/ai/resume-feedback" $null $SeekerToken
if ($r.Status -eq 400) {
    Write-Host "  PASS  AI resume feedback is gated (400: $($r.Body.message))" -ForegroundColor Green; $script:Pass++
} else {
    Write-Host "  FAIL  AI resume feedback returned $($r.Status)" -ForegroundColor Red; $script:Fail++
}
$r = Request POST "/ai/candidate-screening" '{"jobId":1}' $SeekerToken
Check "Job seeker cannot run candidate screening" 403 $r.Status ""

Write-Host ""
Write-Host "======================================================================"
Write-Host "PASS: $script:Pass   FAIL: $script:Fail   SKIP: $script:Skip"
if ($script:Fail -eq 0) { Write-Host "Smoke test passed." -ForegroundColor Green; exit 0 }
else { Write-Host "Smoke test FAILED." -ForegroundColor Red; exit 1 }
