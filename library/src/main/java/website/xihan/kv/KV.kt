package website.xihan.kv

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * KV属性委托实现，参考MMKV-KTX的MMKVProperty设计
 */
class KVProperty<V>(
    internal val decode: (String) -> V, internal val encode: Pair<String, V>.() -> Boolean
) : ReadWriteProperty<IKVOwner, V> {

    override fun getValue(thisRef: IKVOwner, property: KProperty<*>): V =
        decode(property.name)

    override fun setValue(
        thisRef: IKVOwner, property: KProperty<*>, value: V
    ) {
        encode((property.name) to value)
    }
}

/**
 * KV属性委托接口，参考MMKV-KTX的IMMKVOwner设计
 */
interface IKVOwner {
    val kvId: String

    fun kvInt(default: Int = 0) = KVProperty(
        { KVStorage.getInt(kvId, it, default) },
        { KVStorage.putInt(kvId, first, second) })

    fun kvLong(default: Long = 0L) = KVProperty(
        { KVStorage.getLong(kvId, it, default) },
        { KVStorage.putLong(kvId, first, second) })

    fun kvBool(default: Boolean = false) = KVProperty(
        { KVStorage.getBoolean(kvId, it, default) },
        { KVStorage.putBoolean(kvId, first, second) })

    fun kvFloat(default: Float = 0f) = KVProperty(
        { KVStorage.getFloat(kvId, it, default) },
        { KVStorage.putFloat(kvId, first, second) })

    fun kvDouble(default: Double = 0.0) = KVProperty(
        { KVStorage.getDouble(kvId, it, default) },
        { KVStorage.putDouble(kvId, first, second) })

    fun kvString() = KVProperty(
        { KVStorage.getString(kvId, it, "") },
        { KVStorage.putString(kvId, first, second) })

    fun kvString(default: String) = KVProperty(
        { KVStorage.getString(kvId, it, default) },
        { KVStorage.putString(kvId, first, second) })

    fun kvStringSet() = KVProperty(
        { KVStorage.getStringSet(kvId, it, emptySet()) },
        { KVStorage.putStringSet(kvId, first, second) })

    fun kvStringSet(default: Set<String>) = KVProperty(
        { KVStorage.getStringSet(kvId, it, default) },
        { KVStorage.putStringSet(kvId, first, second) })

    fun clearAllKV() = KVStorage.clearAll(kvId)
}

/**
 * KV属性委托基类，参考MMKV-KTX的MMKVOwner设计
 */
open class KVOwner(override val kvId: String) : IKVOwner

/**
 * 获取所有KV数据
 */
fun IKVOwner.getAllKV(): Map<String, Any?> = KVStorage.getAllKV(kvId)