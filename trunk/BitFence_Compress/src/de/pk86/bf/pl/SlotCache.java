package de.pk86.bf.pl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.config.CacheConfiguration;
import de.pkjs.util.Convert;
import electric.xml.Element;

public class SlotCache {
  private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SlotCache.class);

  private String cacheName = "SlotCache";
  private boolean enabled;

  boolean isEnabled() {
    return enabled;
  }

  private Cache cache;
  
  boolean isEmpty() {
     if (cache == null) return true;
    return cache.getMemoryStoreSize() == 0;
  }

  SlotCache(Element ele) {
    if (ele != null) {
      String sEnabled = ele.getAttribute("enabled");
      enabled = Convert.toBoolean(sEnabled);
      try {
        // CacheConfig
        CacheConfiguration cfg = new CacheConfiguration();
        cfg.setName(cacheName);
        // Properties
        long maxElesMem = 10000;
        String maxEntriesLocalHeap = ele.getAttribute("maxEntriesLocalHeap");
        if (maxEntriesLocalHeap != null) {
          maxElesMem = Convert.toLong(maxEntriesLocalHeap);
        }
        cfg.setMaxEntriesLocalHeap(maxElesMem);
        //
        String maxEntriesLocalDisk = ele.getAttribute("maxEntriesLocalDisk");
        if (maxEntriesLocalDisk != null) {
          cfg.setMaxEntriesLocalDisk(Convert.toLong(maxEntriesLocalDisk));
        }
        //
        String overflowToDisk = ele.getAttribute("overflowToDisk");
        if (overflowToDisk != null) {
          cfg.setOverflowToDisk(Convert.toBoolean(overflowToDisk));
        }
        //
        String eternal = ele.getAttribute("eternal");
        if (eternal != null) {
          cfg.setEternal(Convert.toBoolean(eternal));
        }
        //
        long time2idl = 600;
        String timeToIdleSeconds = ele.getAttribute("timeToIdleSeconds");
        if (timeToIdleSeconds != null) {
          time2idl = Convert.toLong(timeToIdleSeconds);
        }
        cfg.setTimeToIdleSeconds(time2idl);
        //
        long time2live = 600;
        String timeToLiveSeconds = ele.getAttribute("timeToLiveSeconds");
        if (timeToLiveSeconds != null) {
          time2live = Convert.toLong(timeToLiveSeconds);
        }
        cfg.setTimeToLiveSeconds(time2live);
        // Create Cache
        cache = new Cache(cfg);
        // add Cache
        BfPL.getCacheManager().addCache(cache);
        if (enabled) {
          cache.setStatisticsEnabled(true);
          //ManagementService.registerMBeans(BfPL.getCacheManager(), getMBeanServer(), true, true, true, true);
        }
      } catch (Exception ex) {
        logger.error(ex.getMessage(), ex);
      }
    }
  }

  void put(Slot s) {
    if (s == null || s.itemname == null)
      return;
    net.sf.ehcache.Element cele = new net.sf.ehcache.Element(s.itemname, s);
    cache.put(cele);
  }

  Slot get(String key) {
    if (isEmpty()) return null;
    net.sf.ehcache.Element cele = cache.get(key);
    return this.get(cele);
  }

  private Slot get(net.sf.ehcache.Element cele) {
     if (isEmpty()) return null;
     if (cele != null) {
      Object val = cele.getValue();
      Slot s = (Slot) val;
      return s;
    } else {
      return null;
    }
  }

  void removeAll() {
     if (isEmpty()) return;
    cache.removeAll();
  }

  public String toString() {
    return cacheName;
  }
}
