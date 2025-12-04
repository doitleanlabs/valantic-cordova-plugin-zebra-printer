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

    // ------------- DISCOVERY -------------

    private void discoverNetworkPrinters(final CallbackContext callbackContext) {

        JSONArray result = new JSONArray();
        cordova.getThreadPool().execute(() -> {
            try {
                /*List<DiscoveredPrinter> printers = NetworkDiscoverer.localBroadcast();

                for (DiscoveredPrinter p : printers) {
                    // Often p.address is the IP
                    result.put(p.address);
                }*/
                
                callbackContext.success(result);
            } catch (DiscoveryException e) {
                callbackContext.error("Discovery failed: " + e.getMessage());
            }
        });

    }

    // ------------- PRINT -------------

    private void printZpl(JSONArray args, final CallbackContext callbackContext) throws JSONException {
        final String ip = args.getString(0);
        final String zpl = args.getString(1);

        cordova.getThreadPool().execute(() -> {
            TcpConnection conn = null;
            try {
                conn = new TcpConnection(ip, 9100);
                conn.open();
                conn.write(zpl.getBytes("UTF-8"));
                conn.close();

                callbackContext.success();
            } catch (Exception e) {
                try {
                    if (conn != null && conn.isConnected()) {
                        conn.close();
                    }
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
