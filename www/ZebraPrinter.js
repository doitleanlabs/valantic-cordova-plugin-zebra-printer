var exec = require('cordova/exec');

var ZebraPrinter = {

    /**
     * Discover network printers on the local subnet.
     * success: function(Array<string> ips)
     * error:   function(string errorMessage)
     */
    discoverNetworkPrinters: function (success, error) {
        exec(success, error, "ZebraPrinter", "discoverNetworkPrinters", []);
    },

    /**
     * Print ZPL to a network printer.
     * @param {string} ip   - printer IP
     * @param {string} zpl  - ZPL string
     */
    printZpl: function (ip, zpl, success, error) {
        exec(success, error, "ZebraPrinter", "printZpl", [ip, zpl]);
    },

    /**
     * Get basic printer status (online, paper, head, etc.).
     * success: function(object status)
     */
    getStatus: function (ip, success, error) {
        exec(success, error, "ZebraPrinter", "getStatus", [ip]);
    }

};

module.exports = ZebraPrinter;
