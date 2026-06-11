# QueryApi

All URIs are relative to *http://localhost/api/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**query**](QueryApi.md#queryoperation) | **POST** /query | Pergunta ao acervo |



## query

> QueryResponse query(queryRequest)

Pergunta ao acervo

Recupera os chunks mais similares à pergunta (opcionalmente restritos a uma coleção) e gera uma resposta fundamentada exclusivamente neles, retornando as fontes citadas. 

### Example

```ts
import {
  Configuration,
  QueryApi,
} from '';
import type { QueryOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new QueryApi();

  const body = {
    // QueryRequest
    queryRequest: ...,
  } satisfies QueryOperationRequest;

  try {
    const data = await api.query(body);
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
| **queryRequest** | [QueryRequest](QueryRequest.md) |  | |

### Return type

[**QueryResponse**](QueryResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Resposta gerada com fontes |  -  |
| **400** | Requisição inválida (validação de campos falhou) |  -  |
| **422** | Coleção informada não existe ou está vazia |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

