$authJson = '{"email":"anuragsingh1839@gmail.com","storeName":"Anshuman"}'
$req1 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/request-otp" -Method Post -Body $authJson -ContentType "application/json"

$resp2 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/verify-otp" -Method Post -Body '{"email":"anuragsingh1839@gmail.com","otp":"123456"}' -ContentType "application/json"
$token = $resp2.data.accessToken

$headers = @{ Authorization = "Bearer $token" }
$nlpJson = '{"transcript":"Shivansh ka udhar kitna hai?"}'
$nlpResp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/nlp/extract" -Headers $headers -Method Post -Body $nlpJson -ContentType "application/json"

Write-Host "================ VOICE QUERY RESPONSE ================"
Write-Host "Intent:            " $nlpResp.data.intent
Write-Host "Customer Name:     " $nlpResp.data.customerName
Write-Host "Outstanding Amount:" $nlpResp.data.outstandingAmount
Write-Host "Response Message:  " $nlpResp.data.responseMessage
