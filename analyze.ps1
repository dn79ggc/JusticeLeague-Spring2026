$rows = Import-Csv data/rooms.csv
$ids = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
foreach ($r in $rows) { [void]$ids.Add($r.RoomID.Trim()) }
"RoomCount=$($rows.Count)"
"FirstRoom=$($rows[0].RoomID)"
$invalid = @()
foreach ($r in $rows) {
  foreach ($dir in @('North','South','East','West')) {
    $v = ($r.$dir + '').Trim()
    if ($v -and $v -ne 'null') {
      if (-not $ids.Contains($v)) {
        $invalid += "$($r.RoomID).$dir->$v"
      }
    }
  }
}
"InvalidExitCount=$($invalid.Count)"
if ($invalid.Count -gt 0) { $invalid | Select-Object -First 20 }
$adj = @{}
foreach ($r in $rows) {
  $list = New-Object System.Collections.Generic.List[string]
  foreach ($dir in @('North','South','East','West')) {
    $v = ($r.$dir + '').Trim()
    if ($v -and $v -ne 'null') { $list.Add($v) }
  }
  $adj[$r.RoomID.Trim()] = $list
}
$start = 'CH-01'
$visited = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
$q = New-Object System.Collections.Generic.Queue[string]
$q.Enqueue($start)
while ($q.Count -gt 0) {
  $cur = $q.Dequeue()
  if (-not $visited.Add($cur)) { continue }
  if ($adj.ContainsKey($cur)) {
    foreach ($n in $adj[$cur]) {
      if (-not $visited.Contains($n)) { $q.Enqueue($n) }
    }
  }
}
"Reachable=$($visited.Count)"
"Unreachable=$([Math]::Max(0, $rows.Count - $visited.Count))"
if ($visited.Count -ne $rows.Count) {
  $missing = @()
  foreach ($r in $rows) { if (-not $visited.Contains($r.RoomID.Trim())) { $missing += $r.RoomID.Trim() } }
  'MissingRooms:'
  $missing
}
