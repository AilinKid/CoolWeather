package com.coolweather.app.activity;

import com.coolweather.app.R;
import com.coolweather.app.service.AutoUpdateService;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class WeatherActivity extends Activity implements OnClickListener{

	private LinearLayout weatherInfoLayout;
	/**
	 * 用于显示城市名
	 */
	private TextView cityNameText;
	/**
	 * 用于显示发布时间
	 */
	private TextView publishText;
	/**
	 * 用于显示天气描述信息
	 */
	private TextView weatherDespText;
	/**
	 * 用于显示气温1
	 */
	private TextView temp1Text;
	/**
	 * 用于显示气温2
	 */
	private TextView temp2Text;
	/**
	 * 用于显示当前日期
	 */
	private TextView currentDateText;
	/**
	 * 切换城市按钮
	 */
	private Button switchCity;
	/**
	 * 更新天气按钮
	 */
	private Button refreshWeather;
    /**
     * 图片view
     */
    private ImageView imgView;
    /**
     * 图片数组
     */
    private Bitmap bitMap = null;
    /**
     * 进度条对话框
     */
    private ProgressDialog progressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		// 初始化各控件
		weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);
		cityNameText = (TextView) findViewById(R.id.city_name);
		publishText = (TextView) findViewById(R.id.publish_text);
		weatherDespText = (TextView) findViewById(R.id.weather_desp);
		temp1Text = (TextView) findViewById(R.id.temp1);
		temp2Text = (TextView) findViewById(R.id.temp2);
		currentDateText = (TextView) findViewById(R.id.current_date);
		switchCity = (Button) findViewById(R.id.switch_city);
		refreshWeather = (Button) findViewById(R.id.refresh_weather);
        imgView = (ImageView)findViewById(R.id.weather_img);

		String countyCode = getIntent().getStringExtra("county_code");
		if (!TextUtils.isEmpty(countyCode)) {
			// 有县级代号时就去查询天气
			publishText.setText("同步中...");
			//title和weatherInfo不可见
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
            imgView.setVisibility(View.INVISIBLE);
			queryWeatherCode(countyCode);
		} else {
			// 没有县级代号时就直接显示本地天气
			showWeather();
		}
        //Activity实现了OnClickListener接口
		switchCity.setOnClickListener(this);
		refreshWeather.setOnClickListener(this);
	}
    //Activity实现了OnClickListener接口，重写onclick方法，根据id判断是那个按钮
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.switch_city:
			Intent intent = new Intent(this, ChooseAreaActivity.class);
			intent.putExtra("from_weather_activity", true);
			startActivity(intent);
			finish();     //我觉得这边默认压栈，没有必要override onBackPressed()
			break;
		case R.id.refresh_weather:
			publishText.setText("同步中...");
            //getDefaultSharedPreferences与getSharedPreferences取得是context的preference（有自定义的区别，共享）
            //getPreferences获得是activity的perference（尽在activity中使用）
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String weatherCode = prefs.getString("weather_code", "");
			//有天气代码的话，直接查询，没有的话只能等选择城市了
			if (!TextUtils.isEmpty(weatherCode)) {
				queryWeatherInfo(weatherCode);
			}
			break;
		default:
			break;
		}
	}
	
	/**
	 * 查询县级代号所对应的天气代号。
	 */
	private void queryWeatherCode(String countyCode) {
		String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
		queryFromServer(address, "countyCode");
	}

	/**
	 * 查询天气代号所对应的天气。
	 */
	private void queryWeatherInfo(String weatherCode) {
		String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
		queryFromServer(address, "weatherCode");
	}
	
	/**
	 * 根据传入的地址和类型去向服务器查询天气代号或者天气信息。
	 */
	private void queryFromServer(final String address, final String type) {
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(final String response) {
                if ("countyCode".equals(type)) {
                    if (!TextUtils.isEmpty(response)) {
                        // 从服务器返回的数据中解析出天气代号
                        String[] array = response.split("\\|");
                        if (array != null && array.length == 2) {
                            String weatherCode = array[1];
                            queryWeatherInfo(weatherCode);
                        }
                    }
                } else if ("weatherCode".equals(type)) {
                    // 处理服务器返回的天气信息
                    Utility.handleWeatherResponse(WeatherActivity.this, response);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showWeather();
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        publishText.setText("同步失败");
                    }
                });
            }
        });
	}
	
	/**
	 * 从SharedPreferences文件中读取存储的天气信息，并显示到界面上。
	 */
	private void showWeather() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		cityNameText.setText( prefs.getString("city_name", ""));
		temp1Text.setText(prefs.getString("temp1", ""));
		temp2Text.setText(prefs.getString("temp2", ""));
		weatherDespText.setText(prefs.getString("weather_desp", ""));
		publishText.setText("今天" + prefs.getString("publish_time", "") + "发布");
		currentDateText.setText(prefs.getString("current_date", ""));
        //准备图片
        //final String imgaddress = "http://www.weather.com.cn/m/i/weatherpic/29x20/"+prefs.getString("img1", "");
        final String imgaddress = "http://m.weather.com.cn/img/"+prefs.getString("img1", "");
        //prepareImg(prefs.getString("img1",""));
        //Bitmap mBitmap = BitmapFactory.decodeByteArray(img1byte, 0, img1byte.length);
        showProgressDialog();
        new Thread(){
            public void run(){
                loadimg(imgaddress);
                WeatherActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(WeatherActivity.this,
                                "加载图片成功，回到主线程", Toast.LENGTH_SHORT).show();
                        imgView.setImageBitmap(bitMap);
                    }
                });
            }
        }.start();
        //Bitmap tempMap= toBig(bitMap);
        //imgView.setImageBitmap(bitMap);
        imgView.requestLayout();
        imgView.setVisibility(View.VISIBLE);
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
		Intent intent = new Intent(this, AutoUpdateService.class);
		startService(intent);
	}

//    private void prepareImg(String img1){
//        String img1Address = "http://www.weather.com.cn/m/i/weatherpic/29x20/"+img1;
//
//
//        HttpUtil.sendHttpRequest(img1Address, new HttpCallbackListener(){
//
//            @Override
//            public void onFinish(String response) {
//                img1byte = response.getBytes();
//                publishText.setText(img1byte.toString());
//            }
//
//            @Override
//            public void onError(Exception e) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        publishText.setText("img1下载失败");
//                    }
//                });
//            }
//        });
//    }

    private void loadimg(String imgaddress){
        int totalSize=0;
        int size = 0;
        URL url = null;
        try{
            url = new URL(imgaddress);
        }
        catch(MalformedURLException e){
            System.out.print(e.getStackTrace());
        }
        try{
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setDoInput(true);
            conn.connect();
            InputStream is  = conn.getInputStream();
            int length = (int)conn.getContentLength();
            totalSize = length;
            if(length!=-1){
                byte[] imgdata = new byte[length];
                byte[] buffer = new byte[512];
                int readLen = 0;
                int destPos = 0;
                while((readLen = is.read(buffer))>0){
                    System.arraycopy(buffer, 0, imgdata, destPos, readLen);
                    destPos += readLen;
                    size = destPos;
                    //Thread.sleep(100);
                }
                bitMap = BitmapFactory.decodeByteArray(imgdata, 0,imgdata.length);
                bitMap = toBig(bitMap);
            }

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public static Bitmap toBig(Bitmap bitmap)
    {
        Matrix matrix = new Matrix();
        matrix.postScale(8f,8f); //长和宽放大缩小的比例
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        return resizeBmp;
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }


}