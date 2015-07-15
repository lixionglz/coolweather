package com.coolweather.app.activity;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
//import android.view.View;
import android.view.Window;
import android.widget.Button;
//import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coolweather.app.R;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

public class WeatherActivity extends Activity implements OnClickListener {

	private LinearLayout weatherInfoLayout;
	
	/**
	 * 显示城市名称
	 */
	private TextView cityNameText;
	
	/**
	 * 显示发布时间
	 */
	private TextView publishText;
	
	/**
	 * 显示当前日期
	 */
	private TextView currentDateText;
	
	/**
	 * 显示天气描述
	 */
	private TextView weatherDespText;
	
	/**
	 * 显示气温1
	 */
	private TextView temp1Text;
	
	/**
	 * 显示气温2
	 */
	private TextView temp2Text;
	
	/**
	 * 切换城市按钮
	 */
	private Button switchCity;
	
	/**
	 * 更新天气按钮
	 */
	private Button refreshWeather;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		//初始化各控件
		weatherInfoLayout = (LinearLayout)findViewById(R.id.weather_info_layout);
		cityNameText = (TextView)findViewById(R.id.city_name);
		publishText = (TextView)findViewById(R.id.publish_text);
		currentDateText = (TextView)findViewById(R.id.current_date);
		weatherDespText = (TextView)findViewById(R.id.weather_desp);
		temp1Text = (TextView)findViewById(R.id.temp1);
		temp2Text = (TextView)findViewById(R.id.temp2);
		switchCity = (Button)findViewById(R.id.switch_city);
		refreshWeather = (Button)findViewById(R.id.refresh_weather);
		
		switchCity.setOnClickListener(this);
		refreshWeather.setOnClickListener(this);
		
		//从选择城市界面传递而来
		String countryCode = getIntent().getStringExtra("country_code");
		if (!TextUtils.isEmpty(countryCode)) {
			//有县级代号就去查询县级天气信息
			publishText.setText("同步中……");
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			queryWeatherCode(countryCode);
		} else {
			//没有县级代号就直接显示天气
			showWeather();
		}
	}
	
	/**
	 * 按钮操作，返回选择城市界面和手动刷新天气信息
	 */
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.switch_city:
			//切换会城市选择界面
			Intent intent = new Intent(WeatherActivity.this, ChooseAreaActivity.class);
			intent.putExtra("from_weather_activity", true);
			startActivity(intent);
			finish();
			break;
		case R.id.refresh_weather:
			publishText.setText("同步中……");
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String weatherCode = prefs.getString("weather_code", "");
			if (!TextUtils.isEmpty(weatherCode)) {
				queryWeatherInfo(weatherCode);
			}
			break;
		default:
			break;
		}
	}
	
	/**
	 * 根据代号查询天气信息
	 * @param countryCode
	 */
	private void queryWeatherCode(String countryCode) {
		String address = "http://www.weather.com.cn/data/list3/city" + countryCode + ".xml";
		queryFromServer(address, "countryCode");
	}
	
	/**
	 * 根据天气代号查询所对应的天气
	 * @param weatherCode
	 */
	private void queryWeatherInfo(String weatherCode) {
		String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
		queryFromServer(address, "weatherCode");
	}
	
	/**根据传入的地址和类型去服务器查询天气代号或者天气信息
	 * @param address
	 * @param code
	 */
	private void queryFromServer(final String address, final String type) {
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			@Override
			public void onFinish(final String response) {
				if (type.toLowerCase(Locale.getDefault()).equals("countrycode")) {
					if (!TextUtils.isEmpty(response)) {
						//从服务器返回的数据中解析天气代号
						String[] array = response.split("\\|");
						if (array != null && array.length == 2) {
							String weatherCode = array[1];
							queryWeatherInfo(weatherCode);
						}
					}
				} else if (type.toLowerCase(Locale.getDefault()).equals("weathercode")) {
					//处理服务器返回的天气信息
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
	 * 从SharedPreferences从读取存储的天气信息，并显示到界面上
	 */
	private void showWeather() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs != null) {
			cityNameText.setText(prefs.getString("city_name", ""));
			temp1Text.setText(prefs.getString("temp1", ""));
			temp2Text.setText(prefs.getString("temp2", ""));
			weatherDespText.setText(prefs.getString("weatherDesp", ""));
			publishText.setText("今天" + prefs.getString("publishTime", "") + "发布");
			currentDateText.setText(prefs.getString("current_date", ""));
			weatherInfoLayout.setVisibility(View.VISIBLE);
			cityNameText.setVisibility(View.VISIBLE);
		}
	}

}
