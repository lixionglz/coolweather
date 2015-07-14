package com.coolweather.app.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.Country;
import com.coolweather.app.model.Province;

public class Utility {

	/**
	 * 解析返回的省市区xml数据
	 */
	public synchronized static boolean handleProvinceResponse(CoolWeatherDB coolWeatherDB, String response) {
		if (!TextUtils.isEmpty(response)) {
			String[] allProvinces = response.split(",");
			if (allProvinces != null && allProvinces.length > 0) {
				for (String p : allProvinces) {
					String[] array = p.split("\\|");
					if (array != null && array.length > 0) {
						Province province = new Province();
						province.setProvinceName(array[1]);
						province.setProvinceCode(array[0]);
						//保存省份表
						coolWeatherDB.saveProvince(province);
					}
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 解析返回城市数据
	 */
	public synchronized static boolean handleCitiesResponse(CoolWeatherDB coolWeatherDB, String response, int provinceId) {
		if (!TextUtils.isEmpty(response)) {
			String[] allCities = response.split(",");
			if (allCities != null && allCities.length > 0) {
				for (String c : allCities) {
					String[] array = c.split("\\|");
					if (array != null && array.length > 0) {
						City city = new City();
						city.setCityName(array[1]);
						city.setCityCode(array[0]);
						city.setProvinceId(provinceId);
						coolWeatherDB.saveCity(city);
					}
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 解析返回的县区数据
	 */
	public synchronized static boolean handleCountriesResponse(CoolWeatherDB coolWeatherDB, String response, int cityId) {
		if (!TextUtils.isEmpty(response)) {
			String[] allCountries = response.split(",");
			if (allCountries != null && allCountries.length > 0) {
				for (String cu : allCountries) {
					String[] array = cu.split("\\|");
					if (array != null && array.length > 0) {
						Country country = new Country();
						country.setCountryName(array[1]);
						country.setCountryCode(array[0]);
						country.setCityId(cityId);
						coolWeatherDB.saveCountry(country);
					}
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 解析从服务器返回的天气json数据，并存储到本地
	 */
	public static void handleWeatherResponse(Context context, String response) {
		try {
			JSONObject jsonObject = new JSONObject(response);
			JSONObject weatherInfo = jsonObject.getJSONObject("weatherinfo");
			String cityName = weatherInfo.getString("city");
			String weatherCode = weatherInfo.getString("cityid");
			String temp1 = weatherInfo.getString("temp1");
			String temp2 = weatherInfo.getString("temp2");
			String weatherDesp = weatherInfo.getString("weather");
			String publishTime = weatherInfo.getString("ptime");
			saveWeatherInfo(context, cityName, weatherCode, temp1, temp2, weatherDesp, publishTime);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 将服务器返回的天气信息保存到SharedPreferences文件中
	 * @param context
	 * @param cityName
	 * @param weatherCode
	 * @param temp1
	 * @param temp2
	 * @param weatherDesp
	 * @param publishTime
	 */
	private static void saveWeatherInfo(Context context,String cityName, String weatherCode, String temp1, String temp2, String weatherDesp, String publishTime) {
		//转换日期格式
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.CHINA);
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean("city_selected", true);
		editor.putString("city_name", cityName);
		editor.putString("weather_code", weatherCode);
		editor.putString("temp1", temp1);
		editor.putString("temp2", temp2);
		editor.putString("weatherDesp", weatherDesp);
		editor.putString("publishTime", publishTime);
		editor.putString("current_date", sdf.format(new Date()));
		editor.commit();
	}
}
