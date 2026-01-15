#import <Cordova/CDV.h>

@interface ZebraPrinterPlugin : CDVPlugin
- (void)discoverNetworkPrinters:(CDVInvokedUrlCommand*)command;
- (void)printZpl:(CDVInvokedUrlCommand*)command;
- (void)getStatus:(CDVInvokedUrlCommand*)command;
@end
