#!/bin/bash

# Generate API OpenAPI code.
rm -rf src/tanagra-api
openapi-generator-cli generate -i ../service/src/main/resources/api/service_openapi.yaml -g typescript-fetch -t ./openapi-templates --additional-properties=typescriptThreePlus=true -o src/tanagra-api

# Generate plugin proto code.
rm -rf src/proto
mkdir src/proto
npx protoc --proto_path=../underlay/src/main/proto/ --plugin=./node_modules/.bin/protoc-gen-ts_proto --ts_proto_out=./src/proto/ --ts_proto_opt=esModuleInterop=true --ts_proto_opt=outputClientImpl=false ../underlay/src/main/proto/criteriaselector/dataschema/*.proto ../underlay/src/main/proto/criteriaselector/configschema/*.proto
