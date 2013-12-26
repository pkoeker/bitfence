package de.pk86.bf.pl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.config.CacheConfiguration;
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
				// Create Cache
				//BfPL.getCacheManager().addCache(cacheName);
				cache = BfPL.getCacheManager().getCache(cacheName);
				CacheConfiguration cfg = cache.getCacheConfiguration();
				int xxx =0;
//				cache.getCacheEventNotificationService().registerListener(new BfCacheRemoveListener());
//				// CacheConfig
//				CacheConfiguration cfg = cache.getCacheConfiguration();
//				// Properties
//				// HEAP
//				try {
//					String maxEntriesLocalHeap = ele.getAttribute("maxEntriesLocalHeap");
//					if (maxEntriesLocalHeap != null) {
//						long maxElesMem = Convert.toLong(maxEntriesLocalHeap);
//						cfg.setMaxEntriesLocalHeap(maxElesMem);
//					} else {
//						String maxBytesLocalHeap = ele.getAttribute("maxBytesLocalHeap");
//						if (maxBytesLocalHeap != null) {
//							cfg.setMaxBytesLocalHeap(maxBytesLocalHeap);
//						}
//					}
//				} catch (Exception ex) {
//					ex.printStackTrace();
//					logger.error(ex.getMessage(), ex);
//				}
//				// Disk
//				try {
//					String maxBytesLocalDisk = ele.getAttribute("maxBytesLocalDisk");
//					if (maxBytesLocalDisk != null) {
//						cfg.setMaxBytesLocalDisk(maxBytesLocalDisk);
//					}
//				} catch (Exception ex) {
//					ex.printStackTrace();
//					logger.error(ex.getMessage(), ex);
//				}
//				//
//				long time2idl = 300; // 5 Minuten
//				String timeToIdleSeconds = ele.getAttribute("timeToIdleSeconds");
//				if (timeToIdleSeconds != null) {
//					time2idl = Convert.toLong(timeToIdleSeconds);
//				}
//				cfg.setTimeToIdleSeconds(time2idl);
//				//
//				long time2live = 1200; // 20 Minuten
//				String timeToLiveSeconds = ele.getAttribute("timeToLiveSeconds");
//				if (timeToLiveSeconds != null) {
//					time2live = Convert.toLong(timeToLiveSeconds);
//				}
//				cfg.setTimeToLiveSeconds(time2live);
			} catch (Exception ex) {
				ex.printStackTrace();
				logger.error(ex.getMessage(), ex);
			}
		}
	}

	void put(Item item) {
		if (item == null || item.itemname == null)
			return;
		net.sf.ehcache.Element cele = new net.sf.ehcache.Element(item.itemname, item);
		cache.put(cele);
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
