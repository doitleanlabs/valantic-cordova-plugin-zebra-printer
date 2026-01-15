#import "ZebraPrinterPluginStub.h"

@implementation ZebraPrinterPlugin

- (void)discoverNetworkPrinters:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *r =
      [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                        messageAsString:@"Zebra printer plugin is not supported on iOS in this app."];
    [self.commandDelegate sendPluginResult:r callbackId:command.callbackId];
}

- (void)printZpl:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *r =
      [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                        messageAsString:@"Zebra printer plugin is not supported on iOS in this app."];
    [self.commandDelegate sendPluginResult:r callbackId:command.callbackId];
}

- (void)getStatus:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *r =
      [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                        messageAsString:@"Zebra printer plugin is not supported on iOS in this app."];
    [self.commandDelegate sendPluginResult:r callbackId:command.callbackId];
}

@end
