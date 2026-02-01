package net.uattest.sdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import net.uattest.service.IIntegrityTokenCallback;
import net.uattest.service.IUnifiedAttestationService;

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
            Log.i("UA-SDK", "Service connected: " + name.flattenToShortString());
            service = IUnifiedAttestationService.Stub.asInterface(binder);
            List<Runnable> actions = new ArrayList<>(pending);
            pending.clear();
            for (Runnable action : actions) {
                action.run();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w("UA-SDK", "Service disconnected: " + name.flattenToShortString());
            service = null;
        }
    };

    public boolean connect() {
        if (bound) return true;
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "net.uattest.service",
                "net.uattest.service.UnifiedAttestationService"
        ));
        bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        if (!bound) {
            Log.e("UA-SDK", "bindService failed; check service install and permissions");
        }
        return bound;
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
                byte[] requestHashBytes = HexUtil.decode(requestHash);
                String alias = "ua:" + projectId + ":" + backendId;
                List<String> chain = KeyAttestationManager.getAttestationChain(
                        context,
                        alias,
                        requestHashBytes
                );
                service.requestIntegrityTokenWithChain(backendId, projectId, requestHash, chain,
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
        if (!connect()) {
            pending.remove(action);
            action.run();
        }
    }
}
