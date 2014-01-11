package de.pk86.bf.pl;

import java.util.BitSet;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheManagerEventListener;

/**
 * Allows implementers to register callback methods that will be executed when a
 * cache event occurs. The events include:
 * <ol>
 * <li>put Element
 * <li>update Element
 * <li>remove Element
 * <li>an Element expires, either because timeToLive or timeToIdle has been
 * reached.
 * </ol>
 * <p/>
 * Callbacks to these methods are synchronous and unsynchronized. It is the
 * responsibility of the implementer to safely handle the potential performance
 * and thread safety issues depending on what their listener is doing.
 * <p/>
 * Events are guaranteed to be notified in the order in which they occurred.
 * <p/>
 * Cache also has putQuiet and removeQuiet methods which do not notify
 * listeners.
 * 
 * @author Greg Luck
 * @version $Id: cache_event_listeners.apt 4369 2011-07-15 19:59:14Z ilevy $
 * @see CacheManagerEventListener
 * @since 1.2
 */
public class BfCacheEventListener implements CacheEventListener {
	  private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BfCacheEventListener.class);
	  private int removed;
	  private int expired;
	  private int evicted;
	
	  public BfCacheEventListener() {
		  super();
	  }
	/**
	 * Called immediately after an element has been removed. The remove method
	 * will block until this method returns.
	 * <p/>
	 * Ehcache does not chech for
	 * <p/>
	 * As the {@link net.sf.ehcache.Element} has been removed, only what was the
	 * key of the element is known.
	 * <p/>
	 * 
	 * @param cache
	 *           the cache emitting the notification
	 * @param element
	 *           just deleted
	 */
	public void notifyElementRemoved(final Ehcache cache, final Element element) throws CacheException {
		removed++;
		if (removed % 100 == 0) {
			System.out.println("removed: " + removed);
		}
		this.updateItem(element);
	}

	/**
	 * Called immediately after an element has been put into the cache. The
	 * {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method will block
	 * until this method returns.
	 * <p/>
	 * Implementers may wish to have access to the Element's fields, including
	 * value, so the element is provided. Implementers should be careful not to
	 * modify the element. The effect of any modifications is undefined.
	 * 
	 * @param cache
	 *           the cache emitting the notification
	 * @param element
	 *           the element which was just put into the cache.
	 */
	public void notifyElementPut(final Ehcache cache, final Element element) throws CacheException {
		Item item = get(element);
		BitSet bs = item.getBitset();
	}

	/**
	 * Called immediately after an element has been put into the cache and the
	 * element already existed in the cache. This is thus an update.
	 * <p/>
	 * The {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method will
	 * block until this method returns.
	 * <p/>
	 * Implementers may wish to have access to the Element's fields, including
	 * value, so the element is provided. Implementers should be careful not to
	 * modify the element. The effect of any modifications is undefined.
	 * 
	 * @param cache
	 *           the cache emitting the notification
	 * @param element
	 *           the element which was just put into the cache.
	 */
	public void notifyElementUpdated(final Ehcache cache, final Element element) throws CacheException {
		Item item = get(element);
		BitSet bs = item.getBitset();
	}

	/**
	 * Called immediately after an element is <i>found</i> to be expired. The
	 * {@link net.sf.ehcache.Cache#remove(Object)} method will block until this
	 * method returns.
	 * <p/>
	 * As the {@link Element} has been expired, only what was the key of the
	 * element is known.
	 * <p/>
	 * Elements are checked for expiry in Ehcache at the following times:
	 * <ul>
	 * <li>When a get request is made
	 * <li>When an element is spooled to the diskStore in accordance with a
	 * MemoryStore eviction policy
	 * <li>In the DiskStore when the expiry thread runs, which by default is
	 * {@link net.sf.ehcache.Cache#DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS}
	 * </ul>
	 * If an element is found to be expired, it is deleted and this method is
	 * notified.
	 * 
	 * @param cache
	 *           the cache emitting the notification
	 * @param element
	 *           the element that has just expired
	 *           <p/>
	 *           Deadlock Warning: expiry will often come from the
	 *           <code>DiskStore</code> expiry thread. It holds a lock to the
	 *           DiskStore at the time the notification is sent. If the
	 *           implementation of this method calls into a synchronized
	 *           <code>Cache</code> method and that subsequently calls into
	 *           DiskStore a deadlock will result. Accordingly implementers of
	 *           this method should not call back into Cache.
	 */
	public void notifyElementExpired(final Ehcache cache, final Element element) {
		this.expired++;
		if (expired % 100 == 0) {
			System.out.println("expired: " + expired);
		}
		this.updateItem(element);
	}

	/**
	 * Give the replicator a chance to cleanup and free resources when no longer
	 * needed
	 */
	public void dispose() {

	}

	/**
	 * Creates a clone of this listener. This method will only be called by
	 * Ehcache before a cache is initialized.
	 * <p/>
	 * This may not be possible for listeners after they have been initialized.
	 * Implementations should throw CloneNotSupportedException if they do not
	 * support clone.
	 * 
	 * @return a clone
	 * @throws CloneNotSupportedException
	 *            if the listener could not be cloned.
	 */
	public Object clone() throws CloneNotSupportedException {
		
		//throw new CloneNotSupportedException();
		return new BfCacheEventListener();
	}

	@Override
   public void notifyElementEvicted(Ehcache arg0, Element element) {
		this.evicted++;
		if (evicted % 100 == 0) {
			System.out.println("evicted: " + evicted);
		}
		this.updateItem(element);
   }

	@Override
   public void notifyRemoveAll(Ehcache cache) {
	   // Cache leeren
		try {
	      BfPL.getInstance().writeAll(null);
      } catch (Exception e) {
	      e.printStackTrace();
         logger.error(e.getMessage(), e);
      }
   }
	
	private void updateItem(net.sf.ehcache.Element cele) {
		Item item = this.get(cele);
		if (item == null) { // wie das?
			logger.warn("item is null?");
			return; 
		}
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