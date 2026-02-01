package net.uattest.service;

import net.uattest.service.IIntegrityTokenCallback;

interface IUnifiedAttestationService {
    List<String> getProviderSet(String projectId);
    void requestIntegrityToken(String backendId, String projectId, String requestHash, IIntegrityTokenCallback callback);
    void requestIntegrityTokenWithChain(String backendId, String projectId, String requestHash, in List<String> attestationChain, IIntegrityTokenCallback callback);
}
