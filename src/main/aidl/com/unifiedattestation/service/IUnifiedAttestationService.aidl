package com.unifiedattestation.service;

import com.unifiedattestation.service.IIntegrityTokenCallback;

interface IUnifiedAttestationService {
    List<String> getProviderSet(String projectId);
    void requestIntegrityToken(String backendId, String projectId, String requestHash, IIntegrityTokenCallback callback);
}
