package de.pk86.bf.pl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.management.CacheStatistics;
import de.pkjs.util.Convert;
import electric.xml.Element;

class ItemCache {
	private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ItemCache.class);

	private final String cacheName = "ItemCache";
	private boolean enabled;

	boolean isEnabled() {
		return enabled;
	}

	private Cache cache;

   ItemCache(Element ele) {
		if (ele != null) {
			String sEnabled = ele.getAttribute("enabled");
			enabled = Convert.toBoolean(sEnabled);
			try {
				// get Cache (Voraussetzung: Cache mit diesem Namen ist in ehcache.xml definiert
				cache = BfPL.getCacheManager().getCache(cacheName);
				CacheConfiguration cfg = cache.getCacheConfiguration();
				logger.debug(cfg);
			} catch (Exception ex) {
				ex.printStackTrace();
				logger.error(ex.getMessage(), ex);
			}
		}
	}
   
   public CacheStatistics getStatistics() {
   	CacheStatistics cs = new CacheStatistics(cache);
   	return cs;
   }

	void put(Item item) {
		if (item == null || item.itemname == null)
			return;
		net.sf.ehcache.Element cele = new net.sf.ehcache.Element(item.itemname, item);
		if (item.isModified()) {
			cache.putWithWriter(cele);				
		} else {
			cache.put(cele);				
		}
	}

	Item get(String key) {
		net.sf.ehcache.Element cele = cache.get(key);
		return this.get(cele);
	}

	private Item get(net.sf.ehcache.Element cele) {
		if (cele != null) {
			Object val = cele.getObjectValue();
			Item item = (Item) val;
			return item;
		} else {
			return null;
		}
	}
	
	boolean remove(String itemname) {
		boolean b = cache.remove(itemname);
		return b;
	}

	void removeAll() {
		cache.removeAll();
	}

	List<Item> getAll() {
		List<String> keys = cache.getKeys();
		Map<Object, net.sf.ehcache.Element> map = cache.getAll(keys);
		List<Item> list = new ArrayList<Item>();
		for (net.sf.ehcache.Element ele : map.values()) {
			list.add((Item) ele.getObjectValue());
		}

		return list;
	}

	public String toString() {
		return cacheName;
	}
}
