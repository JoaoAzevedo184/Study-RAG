
# QueryMetrics


## Properties

Name | Type
------------ | -------------
`retrievalMs` | number
`generationMs` | number
`tokensUsed` | number
`chunksRetrieved` | number

## Example

```typescript
import type { QueryMetrics } from ''

// TODO: Update the object below with actual values
const example = {
  "retrievalMs": null,
  "generationMs": null,
  "tokensUsed": null,
  "chunksRetrieved": null,
} satisfies QueryMetrics

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as QueryMetrics
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


