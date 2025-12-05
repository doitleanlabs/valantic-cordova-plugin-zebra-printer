package com.valantic.zebraprinter;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.NetworkDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveryException;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.PrinterStatus;

public class ZebraPrinterPlugin extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        switch (action) {
            case "discoverNetworkPrinters":
                discoverNetworkPrinters(callbackContext);
                return true;

            case "printZpl":
                printZpl(args, callbackContext);
                return true;

            case "getStatus":
                getStatus(args, callbackContext);
                return true;

            default:
                return false;
        }
    }

    private boolean isPrinterReachable(String ip, int port, int timeoutMs) {
        java.net.Socket socket = new java.net.Socket();
        try {
            socket.connect(new java.net.InetSocketAddress(ip, port), timeoutMs);
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    // ------------- DISCOVERY CORRIGIDO -------------
    private void discoverNetworkPrinters(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(() -> {
        final List<JSONObject> printers = java.util.Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch latch = new CountDownLatch(1);

        DiscoveryHandler handler = new DiscoveryHandler() {
            @Override
            public void foundPrinter(DiscoveredPrinter dp) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("address", dp.address);
                    obj.put("name", dp.getDiscoveryDataMap().get("FRIENDLY_NAME"));
                    printers.add(obj);
                } catch (JSONException ignored) { }
            }

            @Override
            public void discoveryFinished() {
                latch.countDown();
            }

            @Override
            public void discoveryError(String error) {
                latch.countDown();
            }
        };

        try {
            // SAFEST DISCOVERY FOR ZEBRA DEVICES
            NetworkDiscoverer.findPrinters(handler);
        } catch (Exception ex) {
            // If SDK discovery fails → we still continue to fallback scan
            latch.countDown();
        }

        try {
            latch.await(7, TimeUnit.SECONDS); // max wait time
        } catch (Exception ignored) { }

        // FALLBACK → DIRECT TCP SCAN (guaranteed to never crash)
        if (printers.isEmpty()) {
            // Optional IP range — adjustable
            String baseIp = "192.168.1.";

            for (int i = 1; i <= 254; i++) {
                String testIp = baseIp + i;

                if (isPrinterReachable(testIp, 9100, 80)) {
                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("address", testIp);
                        obj.put("name", "Zebra Printer (detected)");
                        printers.add(obj);
                    } catch (Exception ignored) {}
                }
            }
        }

        // Return results
        try {
            JSONArray result = new JSONArray(printers);
            callbackContext.success(result);
        } catch (Exception e) {
            callbackContext.error("Discovery failed: " + e.getMessage());
        }
    });
}


    // ------------- PRINT CORRIGIDO -------------
    private void printZpl(JSONArray args, final CallbackContext callbackContext) throws JSONException {
        final String ip = args.getString(0);
        final String zplData = args.getString(1); // Renomeei para evitar confusão

        cordova.getThreadPool().execute(() -> {
            TcpConnection conn = null;
            try {
                conn = new TcpConnection(ip, 9100);
                conn.open();

                // Verificar se conectou
                if (!conn.isConnected()) {
                    throw new Exception("Could not connect to printer");
                }

                // Converter para bytes
                byte[] data = zplData.getBytes("UTF-8");
                conn.write(data);

                // --- A CORREÇÃO MÁGICA ---
                // Dar tempo para a impressora receber os dados antes de matar a conexão.
                // Impressoras reais precisam disso, simuladores também.
                Thread.sleep(500); 
                // -------------------------

                conn.close();
                callbackContext.success("Print Sent");

            } catch (Exception e) {
                try {
                    if (conn != null) conn.close();
                } catch (ConnectionException ignored) { }

                callbackContext.error("Print failed: " + e.getMessage());
            }
        });
    }   

    // ------------- STATUS -------------

    private void getStatus(JSONArray args, final CallbackContext callbackContext) throws JSONException {
        final String ip = args.getString(0);

        cordova.getThreadPool().execute(() -> {
            TcpConnection conn = null;
            try {
                conn = new TcpConnection(ip, 9100);
                conn.open();

                ZebraPrinter printer = ZebraPrinterFactory.getInstance(conn);
                PrinterStatus status = printer.getCurrentStatus();

                JSONObject json = new JSONObject();
                json.put("isReadyToPrint", status.isReadyToPrint);
                json.put("isPaused", status.isPaused);
                json.put("isHeadOpen", status.isHeadOpen);
                json.put("isPaperOut", status.isPaperOut);
                json.put("isRibbonOut", status.isRibbonOut);

                conn.close();

                callbackContext.success(json);
            } catch (Exception e) {
                try {
                    if (conn != null && conn.isConnected()) {
                        conn.close();
                    }
                } catch (ConnectionException ignored) { }

                callbackContext.error("Status failed: " + e.getMessage());
            }
        });
    }
}
