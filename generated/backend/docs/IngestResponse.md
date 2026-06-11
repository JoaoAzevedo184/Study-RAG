

# IngestResponse


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**fileId** | **UUID** |  |  |
|**status** | [**StatusEnum**](#StatusEnum) |  |  |
|**chunkCount** | **Integer** | Número de chunks gerados (0 quando SKIPPED_UNCHANGED) |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| INGESTED | &quot;INGESTED&quot; |
| SKIPPED_UNCHANGED | &quot;SKIPPED_UNCHANGED&quot; |



