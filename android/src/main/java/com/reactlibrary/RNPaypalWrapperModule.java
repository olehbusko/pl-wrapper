package com.reactlibrary;


import com.loopj.android.http.*;
import com.braintreepayments.api.PayPal;
import cz.msebera.android.httpclient.Header;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PostalAddress;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.exceptions.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;
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

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.AssertionException;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.JSApplicationIllegalArgumentException;

public class RNPaypalWrapperModule extends ReactContextBaseJavaModule implements
        PaymentMethodNonceCreatedListener, BraintreeErrorListener, BraintreeCancelListener {

  private String tokenUrl;
  private Promise callback;
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
  public void createPayment(final String amount, Promise promise) {
    AsyncHttpClient client = new AsyncHttpClient();
    callback = promise;
    client.get(tokenUrl, new TextHttpResponseHandler() {
      @Override
      public void onSuccess(int statusCode, Header[] headers, String clientToken) {
        try {
          mBraintreeFragment = BraintreeFragment.newInstance(getCurrentActivity(), clientToken);
          clientToken = clientToken;
          startCheckout(amount);
        } catch (InvalidArgumentException e) {
          callback.reject(new JSApplicationIllegalArgumentException("Something went wrong. Check amount or try again later."));
          callback = null;
        }
      }

      @Override
      public void onFailure(int statusCode, Header[] headers, String responseBody, Throwable e) {
        callback.reject(new JSApplicationIllegalArgumentException("Something went wrong. Check amount or try again later."));
        callback = null;
      }
    });
  }

  public void startCheckout(String amount) {
    PayPalRequest request = new PayPalRequest(amount).currencyCode("GBP").intent(PayPalRequest.INTENT_AUTHORIZE);
    PayPal.requestOneTimePayment(mBraintreeFragment, request);
  }

  @Override
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
      params.putString("email", payPalAccountNonce.getEmail());
      params.putString("phone", payPalAccountNonce.getPhone());
      params.putString("lastName", payPalAccountNonce.getLastName());
      params.putString("firstName", payPalAccountNonce.getFirstName());

      callback.resolve(params);
      callback = null;
    } else {
      callback.reject(new AssertionException("Something went wrong. Try again later."));
      callback = null;
    }
  }

  @Override
  public void onCancel(int requestCode) {
    callback.reject(new AssertionException("User cancelled."));
    callback = null;
  }

  @Override
  public void onError(Exception error) {
    if (error instanceof ErrorWithResponse) {
      ErrorWithResponse errorWithResponse = (ErrorWithResponse) error;
      BraintreeError cardErrors = errorWithResponse.errorFor("creditCard");
      if (cardErrors != null) {
        BraintreeError expirationMonthError = cardErrors.errorFor("expirationMonth");
        if (expirationMonthError != null) {
          callback.reject(new JSApplicationIllegalArgumentException("Issue with expiration month."));
        } else {
          callback.reject(new JSApplicationIllegalArgumentException("Issue with credit card."));
        }
      }
    }
    callback.reject(new AssertionException("Something went wrong. Try again later"));
    callback = null;
  }

  @Override
  public String getName() {
    return "RNPaypalWrapper";
  }


}
