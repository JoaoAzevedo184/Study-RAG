

# IngestRequest


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**sourceType** | [**SourceTypeEnum**](#SourceTypeEnum) | Tipo de fonte (MVP suporta apenas pdf e markdown) |  |
|**sourceUri** | **String** | Caminho do arquivo sob o diretório de uploads |  |
|**collection** | **String** | Coleção destino (slug minúsculo, hífens permitidos) |  [optional] |
|**metadata** | **Map&lt;String, String&gt;** | Metadados livres associados à fonte |  [optional] |



## Enum: SourceTypeEnum

| Name | Value |
|---- | -----|
| PDF | &quot;pdf&quot; |
| MARKDOWN | &quot;markdown&quot; |



