import { NativeModules, Platform } from 'react-native';

const { RNPaypalWrapper } = NativeModules;

const Paypal = {
  initWithTokenURL(url) {
    RNPaypalWrapper.initWithTokenURL(url);
  },
  createPayment(amount) {
    return RNPaypalWrapper.createPayment(amount);
  },
  setGoogleId(id, key) {
    if (Platform.OS == 'android')
      RNPaypalWrapper.setGoogleId(id, key);
  },
  isGPayReady() {
    if (Platform.OS == 'android')
      return RNPaypalWrapper.isGPayReady();
    else
      return undefined;
  },
  createPaymentGPay(amount) {
    if (Platform.OS == 'android')
      return RNPaypalWrapper.createPaymentGPay(amount);
    else
      return undefined;
  }
}

export default Paypal;
