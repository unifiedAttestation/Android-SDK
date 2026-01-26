package com.unifiedattestation.service;

interface IIntegrityTokenCallback {
    void onSuccess(String token);
    void onError(int code, String message);
}
