

# Source


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**fileId** | **UUID** |  |  |
|**sourceType** | [**SourceTypeEnum**](#SourceTypeEnum) |  |  |
|**sourceUri** | **String** |  |  |
|**collection** | **String** |  |  |
|**title** | **String** |  |  [optional] |
|**chunkCount** | **Integer** |  |  [optional] |
|**status** | [**StatusEnum**](#StatusEnum) |  |  |
|**ingestedAt** | **OffsetDateTime** |  |  [optional] |



## Enum: SourceTypeEnum

| Name | Value |
|---- | -----|
| PDF | &quot;pdf&quot; |
| MARKDOWN | &quot;markdown&quot; |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| INGESTED | &quot;INGESTED&quot; |
| PROCESSING | &quot;PROCESSING&quot; |
| FAILED | &quot;FAILED&quot; |



