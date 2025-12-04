var exec = require('cordova/exec');

var ZebraPrinter = {

    /**
     * Discover network printers on the local subnet.
     * success: function(Array<string> ips)
     * error:   function(string errorMessage)
     */
    discoverNetworkPrinters: function (success, error) {
        exec(success, error, "ZebraPrinterPlugin", "discoverNetworkPrinters", []);
    },

    /**
     * Print ZPL to a network printer.
     * @param {string} ip   - printer IP
     * @param {string} zpl  - ZPL string
     */
    printZpl: function (ip, zpl, success, error) {
        exec(success, error, "ZebraPrinterPlugin", "printZpl", [ip, zpl]);
    },

    /**
     * Get basic printer status (online, paper, head, etc.).
     * success: function(object status)
     */
    getStatus: function (ip, success, error) {
        exec(function(status) {
            // Convert the native status object to a concise string
            var str = 'UNKNOWN';
            if (status && typeof status === 'object') {
                if (status.isReadyToPrint) {
                    str = 'READY';
                } else if (status.isPaperOut) {
                    str = 'PAPER_OUT';
                } else if (status.isHeadOpen) {
                    str = 'HEAD_OPEN';
                } else if (status.isRibbonOut) {
                    str = 'RIBBON_OUT';
                } else if (status.isPaused) {
                    str = 'PAUSED';
                } else {
                    // Fallback to a JSON string if no boolean matched
                    try {
                        str = JSON.stringify(status);
                    } catch (e) {
                        str = 'UNKNOWN';
                    }
                }
            } else if (typeof status === 'string') {
                str = status;
            }
            success(str);
        }, error, "ZebraPrinterPlugin", "getStatus", [ip]);
    }

};

module.exports = ZebraPrinter;
