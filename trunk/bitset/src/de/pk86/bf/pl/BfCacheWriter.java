package de.pk86.bf.pl;

import java.util.Collection;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.writebehind.operations.SingleOperationType;


public class BfCacheWriter implements CacheWriter {
	  private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BfCacheWriter.class);

	public BfCacheWriter() {
		
	}

	@Override
   public CacheWriter clone(Ehcache arg0) throws CloneNotSupportedException {
		return new BfCacheWriter();
	}

	@Override
   public void delete(CacheEntry arg0) throws CacheException {
		System.out.println(arg0);
	   
   }

	@Override
   public void deleteAll(Collection<CacheEntry> arg0) throws CacheException {
		System.out.println(arg0);
	   
   }

	@Override
   public void dispose() throws CacheException {
	   
   }

	@Override
   public void init() {
	   
   }

	@Override
   public void throwAway(Element arg0, SingleOperationType arg1, RuntimeException arg2) {
		System.out.println(arg0);
   }

	@Override
   public void write(Element cele) throws CacheException {
	   Item item = this.get(cele);
		if (item.isModified() || item.isInserted()) {
			try {
	         BfPL.getInstance().insertOrUpdateItem(item);
	         logger.debug("Item updated: " + item.itemname + "/" + item.countBits());
         } catch (Exception e) {
	         e.printStackTrace();
	         logger.error(e.getMessage(), e);
         }
		} else {
         logger.debug("Item removed from cache; not modified/inserted: " + item.itemname + "/" + item.countBits());
		}
   }

	@Override
   public void writeAll(Collection<Element> arg0) throws CacheException {
		System.out.println(arg0);	   
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

}
