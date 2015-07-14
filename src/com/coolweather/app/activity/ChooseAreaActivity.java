package com.coolweather.app.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.*;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;
import com.coolweather.app.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity {

	//从气象接口获取气象数据
	public static final int LEVEL_PROVINCE = 0;
	
	public static final int LEVEL_CITY = 1;
	
	public static final int LEVEL_COUNTRY = 2;
	
	private ProgressDialog progressDialog;
	
	private TextView titleText;
	
	private ListView listView;
	
	private ArrayAdapter<String> adapter;
	
	private CoolWeatherDB coolWeatherDB;
	
	private List<String> dataList = new ArrayList<String>();
	
	/**
	 * 省列表
	 */
	private List<Province> provinceList;
	
	/**
	 * 市列表
	 */
	private List<City> cityList;
	
	/**
	 * 县区列表
	 */
	private List<Country> countryList;
	
	/**
	 * 选中的省份
	 */
	private Province selectedProvince;
	
	/**
	 * 选中的市
	 */
	private City selectedCity;
	
	/**
	 * 当前选中级别
	 */
	private int currentLevel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("city_selected", false)) {
			Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(com.coolweather.app.R.layout.choose_area);
		
		listView = (ListView)findViewById(R.id.list_view);
		titleText = (TextView)findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);
		
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index, long arg3) {
				if (currentLevel == LEVEL_PROVINCE) {
					//获取省份，查询省份下面的城市
					selectedProvince = provinceList.get(index);
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					//获取市，查询市下面的县区
					selectedCity = cityList.get(index);
					queryCountries();
				} else if (currentLevel == LEVEL_COUNTRY) {
					//显示具体县区的天气信息
					String countryCode = countryList.get(index).getCountryCode();
					if (countryCode != null) {
						Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
						intent.putExtra("country_code", countryCode);
						startActivity(intent);
						finish();
					}
				}
			}
		});
		queryProvinces();//加载省级数据
	}
	
	/**
	 * 查询全国省份数据，优先从数据库查询
	 * 如果数据库没有，那么去接口查询
	 */
	private void queryProvinces() {
		provinceList = coolWeatherDB.loadProvinces();
		if (provinceList.size() > 0) {
			dataList.clear();
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel = LEVEL_PROVINCE;
		} else {
			queryFromServer(null, "province");
		}
	}
	
	/**
	 * 查询选中省份下面的城市，优先从数据库查询
	 * 如果没有，从接口查询
	 */
	private void queryCities() {
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());
		if (cityList.size() > 0) {
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		} else {
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}
	}
	
	/**
	 * 查询选中市下面的区县，优先从数据库中查询
	 * 如果数据库没有，从接口获取
	 */
	private void queryCountries() {
		countryList = coolWeatherDB.loadCountries(selectedCity.getId());
		if (countryList.size() > 0) {
			dataList.clear();
			for (Country country : countryList) {
				dataList.add(country.getCountryName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTRY;
		} else {
			queryFromServer(selectedCity.getCityCode(), "country");
		}
	}
	
	/**
	 * 根据传入的编码去接口查询
	 * @param code 省份、城市、区县编码
	 * @param name 查什么
	 */
	private void queryFromServer(final String code, final String type) {
		String address;
		if (!TextUtils.isEmpty(code)) {
			address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
		} else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			@Override
			public void onFinish(String response) {
				boolean result = false;
				if (type.toLowerCase(Locale.getDefault()).equals("province")) {
					result = Utility.handleProvinceResponse(coolWeatherDB, response);
				} else if (type.toLowerCase(Locale.getDefault()).equals("city")) {
					result = Utility.handleCitiesResponse(coolWeatherDB, response, selectedProvince.getId());
				} else if (type.toLowerCase(Locale.getDefault()).equals("country")) {
					result = Utility.handleCountriesResponse(coolWeatherDB, response, selectedCity.getId());
				}
				if (result) {
					//通过runOnUiThread()方法回到主线程处理逻辑
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							closeProgressDialog();
							if (type.toLowerCase(Locale.getDefault()).equals("province")) {
								queryProvinces();
							} else if (type.toLowerCase(Locale.getDefault()).equals("city")) {
								queryCities();
							} else if (type.toLowerCase(Locale.getDefault()).equals("country")) {
								queryCountries();
							}
						}
					});
				}
			}
			
			@Override
			public void onError(Exception e) {
				//通过runOnUiThread()方法回到主线程处理逻辑
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
					}
				}); 
			}
		
		});
	}
	
	/**
	 * 显示查询进度
	 */
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载……");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	/**
	 * 关闭进度
	 */
	private void closeProgressDialog() {
		if (progressDialog != null)
			progressDialog.dismiss();
	}
	
	/**
	 * 捕获Back按键，根据当前级别，确定会退到上一级是什么级别，还是直接退出
	 */
	@Override
	public void onBackPressed() {
		if (currentLevel == LEVEL_COUNTRY) {
			queryCities();
		} else if (currentLevel == LEVEL_CITY) {
			queryProvinces();
		} else if (currentLevel == LEVEL_PROVINCE) {
			finish();
		}
	}
}
