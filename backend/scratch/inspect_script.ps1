$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/debug/inspect-db"
foreach ($item in $response.data) {
    Write-Host "=========================================="
    Write-Host "CUSTOMER ID:" $item.customer.id "MERCHANT ID:" $item.customer.merchantId "NAME:" $item.customer.name "BALANCE:" $item.customer.currentBalance
    Write-Host "TRANSACTIONS:"
    foreach ($tx in $item.transactions) {
        Write-Host "  Tx ID:" $tx.id "Type:" $tx.type "Amount:" $tx.amount "BalAfter:" $tx.balanceAfter "ExplicitBaki:" $tx.explicitOutstanding "Desc:" $tx.description
    }
}
