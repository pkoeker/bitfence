package de.pk86.bf.pl;

import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterFactory;



public class BfCacheWriterFactory extends CacheWriterFactory {

	@Override
   public CacheWriter createCacheWriter(Ehcache arg0, Properties arg1) {
		return new BfCacheWriter();
   }
}
