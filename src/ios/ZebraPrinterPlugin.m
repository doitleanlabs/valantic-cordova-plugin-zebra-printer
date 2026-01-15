#import "ZebraPrinterPlugin.h"

#import "TcpPrinterConnection.h"
#import "ZebraPrinterFactory.h"
#import "PrinterStatus.h"
#import "NetworkDiscoverer.h"
#import "DiscoveredPrinter.h"

@implementation ZebraPrinterPlugin

#pragma mark - Discovery


- (void)discoverNetworkPrinters:(CDVInvokedUrlCommand *)command {
    [self.commandDelegate runInBackground:^{
        NSError *error = nil;

        // Try local broadcast first (fast)
        NSArray *found = [NetworkDiscoverer localBroadcastWithTimeout:3000 error:&error];

        // Fallback: multicast (often more reliable depending on network)
        if ((found == nil || found.count == 0) && error == nil) {
            found = [NetworkDiscoverer multicastWithHops:1 andWaitForResponsesTimeout:3000 error:&error];
        }

        if (error) {
            CDVPluginResult *result =
                [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                  messageAsString:error.localizedDescription];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            return;
        }

        NSMutableArray *ips = [NSMutableArray array];
        for (DiscoveredPrinter *p in found) {
            if (p.address) [ips addObject:p.address];
        }

        CDVPluginResult *result =
            [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:ips];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
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
