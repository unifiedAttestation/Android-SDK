package net.uattest.service;

interface IIntegrityTokenCallback {
    void onSuccess(String token);
    void onError(int code, String message);
}
