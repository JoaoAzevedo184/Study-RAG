# IngestionApi

All URIs are relative to *http://localhost/api/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**ingestDocument**](IngestionApi.md#ingestdocument) | **POST** /ingest | Ingere um documento (PDF ou Markdown) |



## ingestDocument

> IngestResponse ingestDocument(ingestRequest)

Ingere um documento (PDF ou Markdown)

Indexa um arquivo já presente no diretório de uploads. Operação idempotente: se o conteúdo (hash SHA-256) já foi ingerido, retorna status SKIPPED_UNCHANGED sem reprocessar. 

### Example

```ts
import {
  Configuration,
  IngestionApi,
} from '';
import type { IngestDocumentRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IngestionApi();

  const body = {
    // IngestRequest
    ingestRequest: ...,
  } satisfies IngestDocumentRequest;

  try {
    const data = await api.ingestDocument(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **ingestRequest** | [IngestRequest](IngestRequest.md) |  | |

### Return type

[**IngestResponse**](IngestResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Documento aceito para ingestão (ou pulado se inalterado) |  -  |
| **400** | Requisição inválida (validação de campos falhou) |  -  |
| **404** | Arquivo não encontrado no diretório de uploads |  -  |
| **415** | Tipo de arquivo não suportado no MVP |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

