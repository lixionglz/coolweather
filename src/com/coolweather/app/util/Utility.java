package com.coolweather.app.util;

import android.text.TextUtils;

import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.Country;
import com.coolweather.app.model.Province;

public class Utility {

	/**
	 * �������ص�ʡ����xml����
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
						//����ʡ�ݱ�
						coolWeatherDB.saveProvince(province);
					}
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * �������س�������
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
	 * �������ص���������
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
}
