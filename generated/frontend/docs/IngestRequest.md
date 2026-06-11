
# IngestRequest


## Properties

Name | Type
------------ | -------------
`sourceType` | string
`sourceUri` | string
`collection` | string
`metadata` | { [key: string]: string; }

## Example

```typescript
import type { IngestRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "sourceType": null,
  "sourceUri": /uploads/spring-ai-guide.pdf,
  "collection": bootcamp-ntt,
  "metadata": null,
} satisfies IngestRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as IngestRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


