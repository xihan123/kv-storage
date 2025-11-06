package website.xihan.kv

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Thread-safe LRU cache with size limit
 */
class LruCache<K, V>(private val maxSize: Int) {
    private val cache = LinkedHashMap<K, V>(16, 0.75f, true)
    private val lock = ReentrantReadWriteLock()

    fun get(key: K): V? = lock.read {
        cache[key]
    }

    fun put(key: K, value: V) = lock.write {
        cache[key] = value
        if (cache.size > maxSize) {
            val iterator = cache.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
    }

    fun remove(key: K): V? = lock.write {
        cache.remove(key)
    }

    fun clear() = lock.write {
        cache.clear()
    }

    fun size(): Int = lock.read {
        cache.size
    }

    fun keys(): Set<K> = lock.read {
        cache.keys.toSet()
    }
}
