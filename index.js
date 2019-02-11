import { NativeModules } from 'react-native';

const { RNPaypalWrapper } = NativeModules;

const Paypal = {
  initWithTokenURL(url) {
    RNPaypalWrapper.initWithTokenURL(url);
  },
  createPayment(amount) {
    return RNPaypalWrapper.createPayment(amount);
  }
}

export default Paypal;
