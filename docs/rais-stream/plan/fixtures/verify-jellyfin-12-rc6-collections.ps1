param(
    [Parameter(Mandatory)] [string] $ServerUrl,
    [Parameter(Mandatory)] [string] $ApiKey,
    [Parameter(Mandatory)] [guid] $UserId,
    [Parameter(Mandatory)] [guid] $LibraryId,
    [Parameter(Mandatory)] [guid] $TvCollectionId,
    [Parameter(Mandatory)] [guid] $MixedCollectionId,
    [Parameter(Mandatory)] [guid] $UnrelatedCollectionId
)

$headers = @{
    Authorization = "MediaBrowser Client=`"WPHN005Fixture`", Device=`"PowerShell`", DeviceId=`"wphn005-fixture`", Version=`"1.0`", Token=`"$ApiKey`""
}
$query = [uri]::EscapeDataString('BoxSet')
$url = "$($ServerUrl.TrimEnd('/'))/Items?UserId=$UserId&ParentId=$LibraryId&Recursive=true&IncludeItemTypes=$query"
$items = (Invoke-RestMethod -Uri $url -Headers $headers).Items
$ids = @($items | ForEach-Object { [guid] $_.Id })

if ($TvCollectionId -notin $ids) { throw "TV collection was not returned for library $LibraryId" }
if ($MixedCollectionId -notin $ids) { throw "Mixed collection was not returned for library $LibraryId" }
if ($UnrelatedCollectionId -in $ids) { throw "Unrelated collection was returned for library $LibraryId" }

Write-Output "PASS: scoped BOX_SET query retained TV and mixed collections and excluded the unrelated collection."
