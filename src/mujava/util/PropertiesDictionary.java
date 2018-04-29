package mujava.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.tools.javac.util.Pair;

public class PropertiesDictionary
{
	private volatile ConcurrentHashMap<String, String> propertiesMap = new ConcurrentHashMap<>();

	
	public String getProperty(String propertyName)
	{
		return propertiesMap.get(propertyName.trim().toLowerCase());
	}
	
	/**
	 * Allows to get a property with default value, if the property is null
	 * @param propertyName
	 * @param defaultValue
	 * @return
	 */
	public String getProperty(String propertyName,String defaultValue)
	{
		String property=propertiesMap.get(propertyName.trim().toLowerCase());
		return (property!=null?property:defaultValue);
	}
	
	public void setProperty(String propertyName, String propertyValue)
	{
		propertiesMap.put(propertyName.trim().toLowerCase(),propertyValue.trim());
	}
	
	public ConcurrentHashMap<String, String> getPropertiesMap()
	{
		return propertiesMap;
	}

	@Override
	public String toString()
	{
		StringBuilder str=new StringBuilder();
		for(Entry<String,String> entry:propertiesMap.entrySet())
		{
			str.append("\""+entry.getKey()+"\":\""+entry.getValue()+"\"\t");
		}
		return "PropertiesDictionary [propertiesMap=" + str + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((propertiesMap == null) ? 0 : propertiesMap.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PropertiesDictionary other = (PropertiesDictionary) obj;
		if (propertiesMap == null)
		{
			if (other.propertiesMap != null)
				return false;
		}
		else if (!propertiesMap.equals(other.propertiesMap))
			return false;
		return true;
	}

	public void readPropertiesFromFile(String file) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		while ((line = br.readLine()) != null)
		{
			Pair<String, String> localPair = parseProperty(line);
			if (localPair != null)
			{
				propertiesMap.put(localPair.fst, localPair.snd);
			}
		}
		br.close();

	}

	private static Pair<String, String> parseProperty(String property)
	{
		int dividerPosition = property.indexOf('=');
		if (dividerPosition > 0)// if the property starts from = - we ignore it
		{
			return new Pair<String, String>(property.substring(0, dividerPosition).trim().toLowerCase(),
					property.substring(dividerPosition + 1, property.length()).trim());
		}

		return null;
	}

	public void parseProperties(Collection<String> lines)
	{
		for (String property : lines)
		{
			int dividerPosition = property.indexOf('=');
			if (dividerPosition > 0)// if the property starts from = - we ignore it
			{
				propertiesMap.put(property.substring(0, dividerPosition),
						property.substring(dividerPosition + 1, property.length()));
			}
		}
	}
}
