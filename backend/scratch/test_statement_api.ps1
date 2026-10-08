$authJson = '{"email":"anuragsingh1839@gmail.com","storeName":"Anshuman"}'
$req1 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/request-otp" -Method Post -Body $authJson -ContentType "application/json"

$verifyJson = '{"email":"anuragsingh1839@gmail.com","otp":"123456"}'
$resp2 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/verify-otp" -Method Post -Body $verifyJson -ContentType "application/json"
$token = $resp2.data.accessToken

$headers = @{ Authorization = "Bearer $token" }
$stmtResp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/reports/customers/2/statement" -Headers $headers

Write-Host "================ STATEMENT RESPONSE FOR MERCHANT 2 (SHIVANSH ID 2) ================"
Write-Host "Customer Name:         " $stmtResp.data.customerName
Write-Host "Opening Balance:       " $stmtResp.data.openingBalance
Write-Host "Current Outstanding:   " $stmtResp.data.currentOutstanding
Write-Host "Closing Balance:       " $stmtResp.data.closingBalance
Write-Host "Total Historical Jama: " $stmtResp.data.totalJama
Write-Host "Total Historical Udhaar:" $stmtResp.data.totalUdhaar
Write-Host "================ TRANSACTION ROWS ================"
foreach ($t in $stmtResp.data.transactions) {
    Write-Host "  Tx ID:" $t.id "Type:" $t.type "Amount:" $t.amount "BalanceAfter:" $t.balanceAfter "ExplicitBaki:" $t.explicitOutstanding "Desc:" $t.description
}
