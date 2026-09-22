param(
    [string]$ServerAddress = '127.0.0.1',
    [int]$ServerPort = 9000
)

$ErrorActionPreference = 'Stop'

$hexMessage = @'
600070940a6f12003030470020e29a1d00101000000000360070
9227231020093411591260201010700000000140353535353535
3535353535353535353535353535353535353535353535353535
3535353535353535303038393135393200101695481300010800
101695481300010105005001017092264f4b0000000000000000
0000000020231020092905ffff010019312020203031314f4b30
202020202020014f4b0006020170922626002305053981998912
00010000315855294e2020200000012600180670922610163501
12390001002020200008098652525252525252521103fffff170
0068a060031c0126012407820239009f2701809f2608835973b1
0868d1b29f3602004e950500000480009f34030203009f370407
cb34b29f3303e0f8c85f280200769f10120110a0400322000000
0000000000000000ff9a032310205f3401005f201a4b454c4c59
2053494c564120414e4452414445202020202020208407a00000
00041010001700151254699703fffff1700168a060069c008300
1301434931364e53503933343054001402124a39413530333137
38313038000708020200000000000314100000311605ffffffff
ffffffff20383935353035333237333030313939353433313900
0328000200640062057092262310200929053030300039950502
000480009f37049d4b22589f2701409f26085405f0aef3332917
9f100706010a036000000000383632303035078f2e32b303a240
'@ -replace '\s', ''

function Read-Exactly {
    param(
        [System.IO.Stream]$Stream,
        [byte[]]$Buffer
    )

    $offset = 0
    while ($offset -lt $Buffer.Length) {
        $read = $Stream.Read($Buffer, $offset, $Buffer.Length - $offset)
        if ($read -eq 0) {
            throw 'A conexão foi encerrada antes do recebimento completo.'
        }
        $offset += $read
    }
}

function Convert-HexToBytes {
    param([string]$Hex)

    if (($Hex.Length % 2) -ne 0) {
        throw 'A mensagem hexadecimal possui quantidade ímpar de caracteres.'
    }

    $bytes = [byte[]]::new($Hex.Length / 2)
    for ($index = 0; $index -lt $bytes.Length; $index++) {
        $bytes[$index] = [System.Convert]::ToByte($Hex.Substring($index * 2, 2), 16)
    }
    return $bytes
}

function Write-HexDump {
    param(
        [string]$Title,
        [byte[]]$Bytes
    )

    Write-Host ""
    Write-Host "$Title ($($Bytes.Length) bytes)"
    Write-Host 'OFFSET    HEX                                              ASCII'

    for ($offset = 0; $offset -lt $Bytes.Length; $offset += 16) {
        $count = [Math]::Min(16, $Bytes.Length - $offset)
        $hexParts = @()
        $ascii = ''

        for ($index = 0; $index -lt $count; $index++) {
            $value = $Bytes[$offset + $index]
            $hexParts += $value.ToString('X2')
            if ($value -ge 32 -and $value -le 126) {
                $ascii += [char]$value
            }
            else {
                $ascii += '.'
            }
        }

        $hex = ($hexParts -join ' ').PadRight(47)
        Write-Host ('{0:X8}  {1}  {2}' -f $offset, $hex, $ascii)
    }
}

[byte[]]$payload = Convert-HexToBytes -Hex $hexMessage
if ($payload.Length -gt 0xFFFF) {
    throw 'O payload excede o limite do prefixo binário de 2 bytes.'
}

$frame = [byte[]]::new($payload.Length + 2)
$frame[0] = [byte](($payload.Length -shr 8) -band 0xFF)
$frame[1] = [byte]($payload.Length -band 0xFF)
[System.Array]::Copy($payload, 0, $frame, 2, $payload.Length)

Write-HexDump -Title 'Payload enviado' -Bytes $payload

$client = [System.Net.Sockets.TcpClient]::new($ServerAddress, $ServerPort)
try {
    $stream = $client.GetStream()
    $stream.Write($frame, 0, $frame.Length)
    $stream.Flush()
    Write-Host "Enviado: $($payload.Length) bytes de payload."

    $responseHeader = [byte[]]::new(2)
    Read-Exactly -Stream $stream -Buffer $responseHeader
    $responseLength = (([int]$responseHeader[0]) -shl 8) -bor ([int]$responseHeader[1])

    $responsePayload = [byte[]]::new($responseLength)
    Read-Exactly -Stream $stream -Buffer $responsePayload

    Write-HexDump -Title 'Payload recebido' -Bytes $responsePayload
}
finally {
    $client.Close()
}
