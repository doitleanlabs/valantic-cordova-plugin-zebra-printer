#import <Cordova/CDVPlugin.h>

@interface ZebraPrinter : CDVPlugin

- (void)discoverNetworkPrinters:(CDVInvokedUrlCommand *)command;
- (void)printZpl:(CDVInvokedUrlCommand *)command;
- (void)getStatus:(CDVInvokedUrlCommand *)command;

@end
