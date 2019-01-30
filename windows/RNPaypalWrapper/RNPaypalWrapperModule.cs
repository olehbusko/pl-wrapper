using ReactNative.Bridge;
using System;
using System.Collections.Generic;
using Windows.ApplicationModel.Core;
using Windows.UI.Core;

namespace Paypal.Wrapper.RNPaypalWrapper
{
    /// <summary>
    /// A module that allows JS to share data.
    /// </summary>
    class RNPaypalWrapperModule : NativeModuleBase
    {
        /// <summary>
        /// Instantiates the <see cref="RNPaypalWrapperModule"/>.
        /// </summary>
        internal RNPaypalWrapperModule()
        {

        }

        /// <summary>
        /// The name of the native module.
        /// </summary>
        public override string Name
        {
            get
            {
                return "RNPaypalWrapper";
            }
        }
    }
}
