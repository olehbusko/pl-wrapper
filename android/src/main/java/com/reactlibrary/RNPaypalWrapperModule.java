package com.reactlibrary;


import com.braintreepayments.api.interfaces.BraintreeListener;
import com.loopj.android.http.*;
import com.braintreepayments.api.PayPal;
import cz.msebera.android.httpclient.Header;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.GooglePayment;
import com.braintreepayments.api.BraintreeFragment;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PostalAddress;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.exceptions.BraintreeError;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;
import com.braintreepayments.api.models.GooglePaymentCardNonce;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;

import java.util.List;
import java.util.Arrays;
import android.app.Activity;
import java.util.Collections;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.AssertionException;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.JSApplicationIllegalArgumentException;

public class RNPaypalWrapperModule extends ReactContextBaseJavaModule {

  private String tokenKey;
  private String tokenUrl;
  private String merchantId;
  private String clientToken;
  private BraintreeFragment mBraintreeFragment;
  private final ReactApplicationContext reactContext;

  public RNPaypalWrapperModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @ReactMethod
  public void initWithTokenURL(final String url) {
    tokenUrl = url;
  }

  @ReactMethod
  public void createPayment(final String amount, final Promise promise) {
    AsyncHttpClient client = new AsyncHttpClient();
    client.get(tokenUrl, new TextHttpResponseHandler() {
      @Override
      public void onSuccess(int statusCode, Header[] headers, String clientToken) {
        try {
          mBraintreeFragment = BraintreeFragment.newInstance(getCurrentActivity(), clientToken);

          mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
              String nonce = paymentMethodNonce.getNonce();
              if (paymentMethodNonce instanceof PayPalAccountNonce) {
                PayPalAccountNonce payPalAccountNonce = (PayPalAccountNonce)paymentMethodNonce;

                String email = payPalAccountNonce.getEmail();
                String firstName = payPalAccountNonce.getFirstName();
                String lastName = payPalAccountNonce.getLastName();
                String phone = payPalAccountNonce.getPhone();

                PostalAddress billingAddress = payPalAccountNonce.getBillingAddress();
                PostalAddress shippingAddress = payPalAccountNonce.getShippingAddress();

                WritableMap params = Arguments.createMap();
                params.putString("nonce", nonce);
                params.putString("email", payPalAccountNonce.getEmail());
                params.putString("phone", payPalAccountNonce.getPhone());
                params.putString("lastName", payPalAccountNonce.getLastName());
                params.putString("firstName", payPalAccountNonce.getFirstName());

                promise.resolve(params);
              } else {
                promise.reject(new AssertionException("Something went wrong. Try again later."));
              }
            }
          });

          mBraintreeFragment.addListener(new BraintreeErrorListener() {
            public void onError(Exception error) {
              if (error instanceof ErrorWithResponse) {
                ErrorWithResponse errorWithResponse = (ErrorWithResponse) error;
                BraintreeError cardErrors = errorWithResponse.errorFor("creditCard");
                if (cardErrors != null) {
                  BraintreeError expirationMonthError = cardErrors.errorFor("expirationMonth");
                  if (expirationMonthError != null) {
                    promise.reject(new JSApplicationIllegalArgumentException("Issue with expiration month."));
                  } else {
                    promise.reject(new JSApplicationIllegalArgumentException("Issue with credit card."));
                  }
                }
              }
              promise.reject(new AssertionException("Something went wrong. Try again later"));
            }
          });

          mBraintreeFragment.addListener(new BraintreeCancelListener() {
            public void onCancel(int requestCode) {
              promise.reject(new AssertionException("User cancelled."));
            }
          });

          clientToken = clientToken;
          PayPalRequest request = new PayPalRequest(amount).currencyCode("GBP").intent(PayPalRequest.INTENT_AUTHORIZE);
          PayPal.requestOneTimePayment(mBraintreeFragment, request);
        } catch (InvalidArgumentException e) {
          promise.reject(new JSApplicationIllegalArgumentException("Something went wrong. Check amount or try again later."));
        }
      }

      @Override
      public void onFailure(int statusCode, Header[] headers, String responseBody, Throwable e) {
        promise.reject(new JSApplicationIllegalArgumentException("Something went wrong. Check amount or try again later."));
      }
    });
  }

  @ReactMethod
  public void setGoogleId(String id, final String key) {
    tokenKey = key;
    merchantId = id;
  }

  @ReactMethod
  public void isGPayReady(final Promise promise) {
    try {
      mBraintreeFragment = BraintreeFragment.newInstance(getCurrentActivity(), tokenKey);
      GooglePayment.isReadyToPay(mBraintreeFragment, new BraintreeResponseListener<Boolean>() {
        @Override
        public void onResponse(Boolean isReadyToPay) {
          WritableMap params = Arguments.createMap();
          params.putBoolean("isAvailable", isReadyToPay);
          promise.resolve(params);
        }
      });
    } catch (InvalidArgumentException e) {
      promise.reject(new JSApplicationIllegalArgumentException("Something went wrong with init."));
    }
  }

  @ReactMethod
  public void createPaymentGPay(final String amount, final Promise promise) {
    try {
      mBraintreeFragment = BraintreeFragment.newInstance(getCurrentActivity(), tokenKey);
      mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
        public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
          String nonce = paymentMethodNonce.getNonce();
          if (paymentMethodNonce instanceof GooglePaymentCardNonce) {
            GooglePaymentCardNonce googlePaymentCardNonce = (GooglePaymentCardNonce)paymentMethodNonce;
            WritableMap params = Arguments.createMap();
            params.putString("nonce", nonce);
            params.putString("email", googlePaymentCardNonce.getEmail());
            promise.resolve(params);
          } else {
            promise.reject(new AssertionException("Something went wrong. Try again later."));
          }
        }
      });

      mBraintreeFragment.addListener(new BraintreeErrorListener() {
        public void onError(Exception error) {
          if (error instanceof ErrorWithResponse) {
            ErrorWithResponse errorWithResponse = (ErrorWithResponse) error;
            BraintreeError cardErrors = errorWithResponse.errorFor("creditCard");
            if (cardErrors != null) {
              BraintreeError expirationMonthError = cardErrors.errorFor("expirationMonth");
              if (expirationMonthError != null) {
                promise.reject(new JSApplicationIllegalArgumentException("Issue with expiration month."));
              } else {
                promise.reject(new JSApplicationIllegalArgumentException("Issue with credit card."));
              }
            }
          }
          promise.reject(new AssertionException("Something went wrong. Try again later"));
        }
      });

      mBraintreeFragment.addListener(new BraintreeCancelListener() {
        public void onCancel(int requestCode) {
          promise.reject(new AssertionException("User cancelled."));
        }
      });

      GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
              .transactionInfo(TransactionInfo.newBuilder()
              .setTotalPrice(amount)
              .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
              .setCurrencyCode("GBP")
              .build());
      GooglePayment.requestPayment(mBraintreeFragment, googlePaymentRequest);
    } catch (InvalidArgumentException e) {
      promise.reject(new JSApplicationIllegalArgumentException("Something went wrong with init."));
    }
  }

  @Override
  public String getName() {
    return "RNPaypalWrapper";
  }
}
