/**
 * 
 * @ClassName: JsonConverter
 * @Description: 
 * @author Jacky ZHANG
 * @date 9 Aug 2014
 */
package luna.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import luna.web.model.PlannerOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class JsonConverter
{
	/**
	 * 
	 * @Title: jsonToMap
	 * @Description:
	 * @param json
	 * @return Map<String,String>
	 * @throws
	 */
	public static Map<String, String> jsonToMap(String json)
	{
		Map<String, String> map = new HashMap<String, String>();
		try
		{
			if (json == null)
				return null;

			GsonBuilder gb = new GsonBuilder();
			Gson g = gb.create();
			map = g.fromJson(json, new TypeToken<Map<String, String>>()
			{
			}.getType());
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return map;
	}

	public static String listToJson(List list)
	{
		Gson gson = new Gson();
		String jsonString = gson.toJson(list);
		return jsonString;
	}

	/**
	 * 
	 * @Title: objectListToMapList
	 * @Description:
	 * @param @param objectList
	 * @param @return
	 * @return List
	 * @throws
	 */
	public static List objectListToMapList(List objectList)
	{
		List<Map<String, String>> mapList = new ArrayList<Map<String, String>>();
		Gson gson = new Gson();
		try
		{
			for (int i = 0; i < objectList.size(); i++)
			{
				String s = gson.toJson(objectList.get(i));
				Map<String, String> tempMap = gson.fromJson(s,
						new TypeToken<Map<String, String>>()
						{
						}.getType());
				mapList.add(tempMap);
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		return mapList;
	}

	public static <T> T jsonToBean(String jsonString, Class<T> clazz)
	{
		try
		{
			if (jsonString != null)
			{
				Gson gson = new Gson();
				return gson.fromJson(jsonString, clazz);
			}
			else {
				return null;
			}
			
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	// for test:
	public static void main(String[] args)
	{
		System.out.println("-----------------GSONTest-------------");
		PlannerOption option = new PlannerOption();
		option.setSearchAlgorithm("-laostar");
		option.setHeuristics("-ff");
		Gson gson = new Gson();
		String jsonsString = gson.toJson(option);
		
		System.out.println(jsonsString);
		
	}

}
