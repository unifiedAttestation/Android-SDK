# Android-SDK

## Overview
Client wrapper around the Unified Attestation system service (AIDL). No networking.

## Usage
```kotlin
val client = UnifiedAttestationClient(context)
client.getProviderSet(projectId, object : UnifiedAttestationClient.ProviderSetCallback {
  override fun onSuccess(backends: List<String>) { /* ... */ }
  override fun onError(code: Int, message: String) { /* ... */ }
})

client.requestIntegrityToken(backendId, projectId, requestHash, object : UnifiedAttestationClient.TokenCallback {
  override fun onSuccess(token: String) { /* ... */ }
  override fun onError(code: Int, message: String) { /* ... */ }
})
```

## Build
```bash
cd Android-SDK
./gradlew assembleRelease
```
