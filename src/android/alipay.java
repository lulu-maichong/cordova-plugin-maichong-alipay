package it.maichong.alipay;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.alipay.sdk.app.PayTask;
import com.alipay.sdk.app.AuthTask;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Map;

import android.widget.Toast;
import android.text.TextUtils;
import android.annotation.SuppressLint;

/**
 * This class echoes a string called from JavaScript.
 */
public class alipay extends CordovaPlugin {

    private static final int SDK_PAY_FLAG = 1;
    private static final int SDK_AUTH_FLAG = 2;
  
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      if (action.equals("payment")) {
        String orderInfo = args.getString(0);
        this.payment(orderInfo, callbackContext);
        return true;
      } else if (action.equals("auth")) {
        String authInfo = args.getString(0);
        this.auth(authInfo, callbackContext);
        return true;
      }
      return false;
    }
  
    private void auth(String authInfo, final CallbackContext callbackContext) {
      final String authedInfo = authInfo;
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          AuthTask authTask = new AuthTask(cordova.getActivity());
          Map<String, String> result = authTask.authV2(authedInfo, true);
          Log.i("msp", result.toString());
  
          Message msg = new Message();
          msg.what = SDK_AUTH_FLAG;
          msg.obj = result;
          mHandler.sendMessage(msg);
  
          AuthResult authResult = new AuthResult(result, true);
          String resultStatus = authResult.getResultStatus();
          // 判断resultStatus 为“9000”且result_code
          // 为“200”则代表授权成功，具体状态码代表含义可参考授权接口文档
          if (TextUtils.equals(resultStatus, "9000") && TextUtils.equals(authResult.getResultCode(), "200")) {
            // 获取alipay_open_id，调支付时作为参数extern_token 的value
            // 传入，则支付账户为该授权账户
            callbackContext.success(new JSONObject(result));
          } else {
            // 其他状态值则为授权失败
            callbackContext.error(new JSONObject(result));
          }
        }
      });
    }
  
    private void payment(String orderInfo, final CallbackContext callbackContext) {
  
      final String payInfo = orderInfo;
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          PayTask alipay = new PayTask(cordova.getActivity());
          Map<String, String> result = alipay.payV2(payInfo, true);
          Log.i("msp", result.toString());
  
          Message msg = new Message();
          msg.what = SDK_PAY_FLAG;
          msg.obj = result;
          mHandler.sendMessage(msg);
  
          PayResult payResult = new PayResult(result);
          String resultInfo = payResult.getResult();// 同步返回需要验证的信息
          String resultStatus = payResult.getResultStatus();
          // 判断resultStatus 为9000则代表支付成功
          if (TextUtils.equals(resultStatus, "9000")) {
            // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
            callbackContext.success(new JSONObject(result));
          } else {
            // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
            callbackContext.error(new JSONObject(result));
          }
        }
      });
  
    }
  
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
      @SuppressWarnings("unused")
      public void handleMessage(Message msg) {
        switch (msg.what) {
        case SDK_PAY_FLAG: {
          @SuppressWarnings("unchecked")
          PayResult payResult = new PayResult((Map<String, String>) msg.obj);
          /**
           * 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
           */
          String resultInfo = payResult.getResult();// 同步返回需要验证的信息
          String resultStatus = payResult.getResultStatus();
          // 判断resultStatus 为9000则代表支付成功
          // 判断resultStatus 为9000则代表支付成功
          if (TextUtils.equals(resultStatus, "9000")) {
            // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
            Toast.makeText(cordova.getActivity(), "支付成功" + resultStatus, Toast.LENGTH_SHORT);
          } else {
            // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
            Toast.makeText(cordova.getActivity(), "支付失败" + resultStatus, Toast.LENGTH_SHORT);
          }
          break;
        }
        case SDK_AUTH_FLAG: {
          @SuppressWarnings("unchecked")
          AuthResult authResult = new AuthResult((Map<String, String>) msg.obj, true);
          String resultStatus = authResult.getResultStatus();
          // 判断resultStatus 为“9000”且result_code
          // 为“200”则代表授权成功，具体状态码代表含义可参考授权接口文档
          if (TextUtils.equals(resultStatus, "9000") && TextUtils.equals(authResult.getResultCode(), "200")) {
            // 获取alipay_open_id，调支付时作为参数extern_token 的value
            // 传入，则支付账户为该授权账户
            Toast.makeText(cordova.getActivity(), "授权成功\n" + String.format("authCode:%s", authResult.getAuthCode()),
                Toast.LENGTH_SHORT);
          } else {
            // 其他状态值则为授权失败
            Toast.makeText(cordova.getActivity(), "授权失败" + String.format("authCode:%s", authResult.getAuthCode()),
                Toast.LENGTH_SHORT);
          }
          break;
        }
        default:
          break;
        }
      };
    };
  }
  