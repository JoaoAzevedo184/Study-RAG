# SourcesApi

All URIs are relative to *http://localhost/api/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteSource**](SourcesApi.md#deletesource) | **DELETE** /sources/{fileId} | Remove uma fonte e seus vetores |
| [**listSources**](SourcesApi.md#listsources) | **GET** /sources | Lista fontes ingeridas |



## deleteSource

> deleteSource(fileId)

Remove uma fonte e seus vetores

Remove o registro da fonte e todos os chunks associados no vector store.

### Example

```ts
import {
  Configuration,
  SourcesApi,
} from '';
import type { DeleteSourceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new SourcesApi();

  const body = {
    // string
    fileId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies DeleteSourceRequest;

  try {
    const data = await api.deleteSource(body);
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
| **fileId** | `string` |  | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Removido com sucesso |  -  |
| **404** | Fonte não encontrada |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listSources

> Array&lt;Source&gt; listSources(collection)

Lista fontes ingeridas

### Example

```ts
import {
  Configuration,
  SourcesApi,
} from '';
import type { ListSourcesRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new SourcesApi();

  const body = {
    // string | Filtra fontes por coleção (optional)
    collection: collection_example,
  } satisfies ListSourcesRequest;

  try {
    const data = await api.listSources(body);
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
| **collection** | `string` | Filtra fontes por coleção | [Optional] [Defaults to `undefined`] |

### Return type

[**Array&lt;Source&gt;**](Source.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Lista de fontes |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

