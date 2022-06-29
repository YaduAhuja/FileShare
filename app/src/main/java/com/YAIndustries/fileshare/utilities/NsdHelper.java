package com.YAIndustries.fileshare.utilities;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.YAIndustries.fileshare.interfaces.NsdTaskInvoker;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

public class NsdHelper {
    private final NsdManager nsdManager;
    private final NsdTaskInvoker taskInvoker;
    private final String DEVICE_NAME = Utils.getDeviceName();
    private final String SERVICE_NAME = "FileShareService";
    private final String SERVICE_TYPE = "_ftp-data._tcp";
    private final String TAG = "NsdHelper";
    private NsdServiceInfo fileShareService;
    private NsdManager.RegistrationListener registrationListener;
    private NsdManager.ResolveListener resolveListener;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdServiceInfo serviceInfo;

    public NsdHelper(NsdTaskInvoker taskInvoker, NsdManager nsdManager) {
        this.taskInvoker = taskInvoker;
        this.nsdManager = nsdManager;
    }

    public void registerService(int port) {
        if (registrationListener == null)
            initializeRegistrationListener();

        if (serviceInfo == null) {
            // Create the NsdServiceInfo object, and populate it.
            serviceInfo = new NsdServiceInfo();

            // The name is subject to change based on conflicts
            // with other services advertised on the same network.
            serviceInfo.setServiceName(DEVICE_NAME + "_" + SERVICE_NAME);
            serviceInfo.setServiceType(SERVICE_TYPE);
            serviceInfo.setPort(port);
        }

        nsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    public void discoverServices() {
        if (discoveryListener == null)
            initializeDiscoveryListener();
        nsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void suspend() {
        if (registrationListener != null)
            nsdManager.unregisterService(registrationListener);
        if (discoveryListener != null)
            nsdManager.stopServiceDiscovery(discoveryListener);
    }

    public void resume() {
        if (registrationListener != null)
            registerService(0);
        if (discoveryListener != null)
            discoverServices();

    }


    private void initializeRegistrationListener() {
        registrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                // Save the service name. Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                fileShareService = nsdServiceInfo;
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed! Put debugging code here to determine why.
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered. This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed. Put debugging code here to determine why.
            }
        };
    }


    private void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        discoveryListener = new NsdManager.DiscoveryListener() {

            // Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success: " + service);
                if (service.getServiceName().contains(SERVICE_NAME)) {
                    initializeResolveListener();
                    nsdManager.resolveService(service, resolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.d(TAG, "service lost: " + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }
        };
    }

    private void initializeResolveListener() {
        resolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                Log.d(TAG, "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Resolve Succeeded. " + serviceInfo);


                fileShareService = serviceInfo;
                int port = fileShareService.getPort();
                InetAddress host = fileShareService.getHost();
                Log.d(TAG, "onServiceResolved: Host" + host);
                Log.d(TAG, "onServiceResolved: Port" + port);
                CompletableFuture.runAsync(() -> taskInvoker.onNsdTaskSuccessful(host, port));
            }
        };
    }
}
