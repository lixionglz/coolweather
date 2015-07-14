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

	//������ӿڻ�ȡ��������
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
	 * ʡ�б�
	 */
	private List<Province> provinceList;
	
	/**
	 * ���б�
	 */
	private List<City> cityList;
	
	/**
	 * �����б�
	 */
	private List<Country> countryList;
	
	/**
	 * ѡ�е�ʡ��
	 */
	private Province selectedProvince;
	
	/**
	 * ѡ�е���
	 */
	private City selectedCity;
	
	/**
	 * ��ǰѡ�м���
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
					//��ȡʡ�ݣ���ѯʡ������ĳ���
					selectedProvince = provinceList.get(index);
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					//��ȡ�У���ѯ�����������
					selectedCity = cityList.get(index);
					queryCountries();
				} else if (currentLevel == LEVEL_COUNTRY) {
					//��ʾ����������������Ϣ
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
		queryProvinces();//����ʡ������
	}
	
	/**
	 * ��ѯȫ��ʡ�����ݣ����ȴ����ݿ��ѯ
	 * ������ݿ�û�У���ôȥ�ӿڲ�ѯ
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
			titleText.setText("�й�");
			currentLevel = LEVEL_PROVINCE;
		} else {
			queryFromServer(null, "province");
		}
	}
	
	/**
	 * ��ѯѡ��ʡ������ĳ��У����ȴ����ݿ��ѯ
	 * ���û�У��ӽӿڲ�ѯ
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
	 * ��ѯѡ������������أ����ȴ����ݿ��в�ѯ
	 * ������ݿ�û�У��ӽӿڻ�ȡ
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
	 * ���ݴ���ı���ȥ�ӿڲ�ѯ
	 * @param code ʡ�ݡ����С����ر���
	 * @param name ��ʲô
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
					//ͨ��runOnUiThread()�����ص����̴߳����߼�
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
				//ͨ��runOnUiThread()�����ص����̴߳����߼�
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "����ʧ��", Toast.LENGTH_SHORT).show();
					}
				}); 
			}
		
		});
	}
	
	/**
	 * ��ʾ��ѯ����
	 */
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("���ڼ��ء���");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	/**
	 * �رս���
	 */
	private void closeProgressDialog() {
		if (progressDialog != null)
			progressDialog.dismiss();
	}
	
	/**
	 * ����Back���������ݵ�ǰ����ȷ�����˵���һ����ʲô���𣬻���ֱ���˳�
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
