#import "ZebraPrinterPlugin.h"

#import <LinkOS/LinkOS.h>  // Adjust import according to actual SDK

@implementation ZebraPrinterPlugin

#pragma mark - Discovery

- (void)discoverNetworkPrinters:(CDVInvokedUrlCommand *)command {
    // NOTE: Link-OS iOS discovery often uses SGD or specific discovery classes.
    // Here we just return an empty list placeholder for now, so plugin builds.
    // You can later wire it to real discovery APIs.
    
    CDVPluginResult *result =
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                          messageAsArray:@[]];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

#pragma mark - Print

- (void)printZpl:(CDVInvokedUrlCommand *)command {
    NSString *ip = [command.arguments objectAtIndex:0];
    NSString *zpl = [command.arguments objectAtIndex:1];

    [self.commandDelegate runInBackground:^{
        id<ZebraPrinterConnection, NSObject> connection =
            [[TcpPrinterConnection alloc] initWithAddress:ip andWithPort:9100];

        CDVPluginResult *pluginResult = nil;

        @try {
            NSError *error = nil;
            [connection open];

            NSData *data = [zpl dataUsingEncoding:NSUTF8StringEncoding];
            [connection write:data error:&error];

            if (error) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                 messageAsString:[error localizedDescription]];
            } else {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            }

            [connection close];
        }
        @catch (NSException *exception) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                             messageAsString:exception.reason];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

#pragma mark - Status

- (void)getStatus:(CDVInvokedUrlCommand *)command {
    NSString *ip = [command.arguments objectAtIndex:0];

    [self.commandDelegate runInBackground:^{
        id<ZebraPrinterConnection, NSObject> connection =
            [[TcpPrinterConnection alloc] initWithAddress:ip andWithPort:9100];

        CDVPluginResult *pluginResult = nil;

        @try {
            NSError *error = nil;
            [connection open];

            id<ZebraPrinter, NSObject> printer = [ZebraPrinterFactory getInstance:connection];
            PrinterStatus *status = [printer getCurrentStatus:&error];

            if (error) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                 messageAsString:[error localizedDescription]];
            } else {
                NSMutableDictionary *dict = [NSMutableDictionary dictionary];
                dict[@"isReadyToPrint"] = @(status.isReadyToPrint);
                dict[@"isPaused"]       = @(status.isPaused);
                dict[@"isHeadOpen"]     = @(status.isHeadOpen);
                dict[@"isPaperOut"]     = @(status.isPaperOut);
                dict[@"isRibbonOut"]    = @(status.isRibbonOut);

                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                             messageAsDictionary:dict];
            }

            [connection close];
        }
        @catch (NSException *exception) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                             messageAsString:exception.reason];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

@end
