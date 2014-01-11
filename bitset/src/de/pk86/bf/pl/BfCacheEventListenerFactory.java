package de.pk86.bf.pl;

import java.util.Properties;

import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;

public class BfCacheEventListenerFactory extends CacheEventListenerFactory {

	@Override
   public CacheEventListener createCacheEventListener(Properties arg0) {
		return new BfCacheEventListener();
   }

}
