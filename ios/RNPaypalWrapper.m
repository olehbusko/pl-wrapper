
#import "RNPaypalWrapper.h"
#import "BraintreeCore.h"

@inferface RNPaypalWrapper () <BTAppSwitchDelegate, BTViewControllerPresentingDelegate>

@property (nonatomic, strong) BTAPIClient *braintreeClient;
@property (nonatomic, strong) BTPayPalDriver *payPalDriver;

@end

@implementation RNPaypalWrapper

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE()

- (void)fetchClientToken {
    NSURL *clientTokenURL = [NSURL URLWithString:@"https://braintree-sample-merchant.herokuapp.com/client_token"];
    NSMutableURLRequest *clientTokenRequest = [NSMutableURLRequest requestWithURL:clientTokenURL];
    [clientTokenRequest setValue:@"text/plain" forHTTPHeaderField:@"Accept"];

    [[[NSURLSession sharedSession] dataTaskWithRequest:clientTokenRequest completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        NSString *clientToken = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    }] resume];
}

- (void)startCheckout {
    self.braintreeClient = [[BTAPIClient alloc] initWithAuthorization:@"<#CLIENT_AUTHORIZATION#>"];
    BTPayPalDriver *payPalDriver = [[BTPayPalDriver alloc] initWithAPIClient:self.braintreeClient];
    payPalDriver.viewControllerPresentingDelegate = self;
    payPalDriver.appSwitchDelegate = self; // Optional

    // Specify the transaction amount here. "2.32" is used in this example.
    BTPayPalRequest *request= [[BTPayPalRequest alloc] initWithAmount:@"2.32"];
    request.currencyCode = @"USD"; // Optional; see BTPayPalRequest.h for other options

    [payPalDriver requestOneTimePayment:request completion:^(BTPayPalAccountNonce * _Nullable tokenizedPayPalAccount, NSError * _Nullable error) {
        if (tokenizedPayPalAccount) {
            NSLog(@"Got a nonce: %@", tokenizedPayPalAccount.nonce);

            // Access additional information
            NSString *email = tokenizedPayPalAccount.email;
            NSString *firstName = tokenizedPayPalAccount.firstName;
            NSString *lastName = tokenizedPayPalAccount.lastName;
            NSString *phone = tokenizedPayPalAccount.phone;

            // See BTPostalAddress.h for details
            BTPostalAddress *billingAddress = tokenizedPayPalAccount.billingAddress;
            BTPostalAddress *shippingAddress = tokenizedPayPalAccount.shippingAddress;
        } else if (error) {
            // Handle error here...
        } else {
            // Buyer canceled payment approval
        }
    }];
}

- (void)postNonceToServer:(NSString *)paymentMethodNonce {
    // Update URL with your server
    NSURL *paymentURL = [NSURL URLWithString:@"https://your-server.example.com/checkout"];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:paymentURL];
    request.HTTPBody = [[NSString stringWithFormat:@"payment_method_nonce=%@", paymentMethodNonce] dataUsingEncoding:NSUTF8StringEncoding];
    request.HTTPMethod = @"POST";

    [[[NSURLSession sharedSession] dataTaskWithRequest:request completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        // TODO: Handle success and failure
    }] resume];
}

@end
