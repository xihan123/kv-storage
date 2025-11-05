package website.xihan.xposed

import website.xihan.kv.IKVOwner
import website.xihan.kv.KVOwner

object ModuleConfig : IKVOwner by KVOwner("SHARED_SETTINGS") {
    var swithchEnable by kvBool()
    var textViewText by kvString()
}