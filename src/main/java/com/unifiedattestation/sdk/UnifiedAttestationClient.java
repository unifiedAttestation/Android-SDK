package com.unifiedattestation.sdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.unifiedattestation.service.IIntegrityTokenCallback;
import com.unifiedattestation.service.IUnifiedAttestationService;

import java.util.ArrayList;
import java.util.List;

public class UnifiedAttestationClient {
    public static final int ERROR_NOT_CONNECTED = -1;

    public interface ProviderSetCallback {
        void onSuccess(List<String> backends);
        void onError(int code, String message);
    }

    public interface TokenCallback {
        void onSuccess(String token);
        void onError(int code, String message);
    }

    private final Context context;
    private IUnifiedAttestationService service;
    private final List<Runnable> pending = new ArrayList<>();
    private boolean bound = false;

    public UnifiedAttestationClient(Context context) {
        this.context = context.getApplicationContext();
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = IUnifiedAttestationService.Stub.asInterface(binder);
            List<Runnable> actions = new ArrayList<>(pending);
            pending.clear();
            for (Runnable action : actions) {
                action.run();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    };

    public void connect() {
        if (bound) return;
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.unifiedattestation.service",
                "com.unifiedattestation.service.UnifiedAttestationService"
        ));
        bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    public void disconnect() {
        if (!bound) return;
        context.unbindService(connection);
        bound = false;
        service = null;
    }

    public void getProviderSet(String projectId, ProviderSetCallback callback) {
        Runnable action = () -> {
            if (service == null) {
                callback.onError(ERROR_NOT_CONNECTED, "Service not connected");
                return;
            }
            try {
                List<String> result = service.getProviderSet(projectId);
                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onError(ERROR_NOT_CONNECTED, e.getMessage() == null ? "Service error" : e.getMessage());
            }
        };
        ensureService(action);
    }

    public void requestIntegrityToken(
            String backendId,
            String projectId,
            String requestHash,
            TokenCallback callback
    ) {
        Runnable action = () -> {
            if (service == null) {
                callback.onError(ERROR_NOT_CONNECTED, "Service not connected");
                return;
            }
            try {
                service.requestIntegrityToken(backendId, projectId, requestHash,
                        new IIntegrityTokenCallback.Stub() {
                            @Override
                            public void onSuccess(String token) {
                                callback.onSuccess(token);
                            }

                            @Override
                            public void onError(int code, String message) {
                                callback.onError(code, message);
                            }
                        });
            } catch (Exception e) {
                callback.onError(ERROR_NOT_CONNECTED, e.getMessage() == null ? "Service error" : e.getMessage());
            }
        };
        ensureService(action);
    }

    private void ensureService(Runnable action) {
        if (service != null) {
            action.run();
            return;
        }
        pending.add(action);
        connect();
    }
}
