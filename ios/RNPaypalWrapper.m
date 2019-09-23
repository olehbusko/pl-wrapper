
#import "RNPaypalWrapper.h"
#import "BraintreeCore.h"
#import "BraintreePayPal.h"

@interface RNPaypalWrapper () <BTViewControllerPresentingDelegate>

@property (nonatomic, strong) BTAPIClient *braintreeClient;
@property (nonatomic, strong) BTPayPalDriver *payPalDriver;

@end


@implementation RNPaypalWrapper {
    NSString *_tokenUrl;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(initWithTokenURL:(NSString *)url) {
    _tokenUrl = url;
}

RCT_EXPORT_METHOD(createPayment: (NSString *)amount
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
    NSURL *clientTokenURL = [NSURL URLWithString:_tokenUrl];
    NSMutableURLRequest *clientTokenRequest = [NSMutableURLRequest requestWithURL:clientTokenURL];
    [clientTokenRequest setValue:@"text/plain" forHTTPHeaderField:@"Accept"];

    [[[NSURLSession sharedSession] dataTaskWithRequest:clientTokenRequest completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        if (error) {
            reject(@"no_token", @"Enable to retrieve a client token from the server", nil);
        } else {
            NSString *clientToken = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
            [self startCheckout:clientToken amount:amount resolver:resolve rejecter:reject];
        }
    }] resume];
}

- (void)startCheckout:(NSString *)token
               amount:(NSString *)amount
             resolver:(RCTPromiseResolveBlock)resolve
             rejecter:(RCTPromiseRejectBlock)reject {
    self.braintreeClient = [[BTAPIClient alloc] initWithAuthorization:token];
    BTPayPalDriver *payPalDriver = [[BTPayPalDriver alloc] initWithAPIClient:self.braintreeClient];
    payPalDriver.viewControllerPresentingDelegate = self;

    BTPayPalRequest *request= [[BTPayPalRequest alloc] initWithAmount: amount];

    [payPalDriver requestOneTimePayment:request completion:^(BTPayPalAccountNonce * _Nullable tokenizedPayPalAccount, NSError * _Nullable error) {
        if (tokenizedPayPalAccount) {
            NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
            if (tokenizedPayPalAccount.nonce)
                [params setObject:tokenizedPayPalAccount.nonce forKey:@"nonce"];
            if (tokenizedPayPalAccount.email)
                [params setObject:tokenizedPayPalAccount.email forKey:@"email"];
            if (tokenizedPayPalAccount.firstName)
                [params setObject:tokenizedPayPalAccount.firstName forKey:@"firstName"];
            if (tokenizedPayPalAccount.lastName)
                [params setObject:tokenizedPayPalAccount.lastName forKey:@"lastName"];
            if (tokenizedPayPalAccount.phone)
                [params setObject:tokenizedPayPalAccount.phone forKey:@"phone"];
            resolve(params);
        } else if (error) {
            NSString *message = [NSString stringWithFormat:@"%@ %@", error.localizedDescription, error.localizedRecoverySuggestion];
            reject(@"Paypal error", message, nil);
        } else {
            reject(@"Paypal error", @"Customer cancelled checkout.", nil);
        }
    }];
}

- (void)startCheckout:(NSString *) token {
    self.braintreeClient = [[BTAPIClient alloc] initWithAuthorization:token];
    BTPayPalDriver *payPalDriver = [[BTPayPalDriver alloc] initWithAPIClient:self.braintreeClient];
    payPalDriver.viewControllerPresentingDelegate = self;

    BTPayPalRequest *request= [[BTPayPalRequest alloc] initWithAmount:@"2.32"];
    request.currencyCode = @"GBP";

    [payPalDriver requestOneTimePayment:request completion:^(BTPayPalAccountNonce * _Nullable tokenizedPayPalAccount, NSError * _Nullable error) {
        if (tokenizedPayPalAccount) {
            NSLog(@"Got a nonce: %@", tokenizedPayPalAccount.nonce);

            NSString *email = tokenizedPayPalAccount.email;
            NSString *firstName = tokenizedPayPalAccount.firstName;
            NSString *lastName = tokenizedPayPalAccount.lastName;
            NSString *phone = tokenizedPayPalAccount.phone;

            // See BTPostalAddress.h for details
            BTPostalAddress *billingAddress = tokenizedPayPalAccount.billingAddress;
            BTPostalAddress *shippingAddress = tokenizedPayPalAccount.shippingAddress;
        } else if (error) {
            NSLog(@"Paypal token: %@", token);
            NSLog(@"Paypal error: %@", error.localizedDescription);
            NSLog(@"Paypal error suggestions: %@", error.localizedRecoverySuggestion);
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



- (void)paymentDriver:(nonnull id)driver requestsDismissalOfViewController:(nonnull UIViewController *)viewController {

}

- (void)paymentDriver:(nonnull id)driver requestsPresentationOfViewController:(nonnull UIViewController *)viewController {

}

@end
