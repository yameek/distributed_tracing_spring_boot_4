#!/bin/bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { createOrder(productId: \"produit-test\", quantity: 10) { orderId status } }"}' \
  http://localhost:8080/graphql > response.json
