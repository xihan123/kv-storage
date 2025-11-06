![kv-storage](https://socialify.git.ci/xihan123/kv-storage/image?description=1&forks=1&issues=1&language=1&name=1&owner=1&pulls=1&stargazers=1&theme=Auto)

![above](https://img.shields.io/badge/Android-8.0%20or%20above-brightgreen.svg)
[![Android CI](https://github.com/xihan123/kv-storage/actions/workflows/build.yml/badge.svg)](https://github.com/xihan123/kv-storage/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/release/xihan123/kv-storage.svg)](https://github.com/xihan123/kv-storage/releases)
![downloads](https://img.shields.io/github/downloads/xihan123/kv-storage/total)

# KV Storage

一个基于 ContentProvider 的轻量级 Android KV 存储库，支持跨进程通信和 Xposed 模块集成。

## 特性

- 简洁的 Kotlin 属性委托 API
- 支持跨进程数据同步
- 支持基本数据类型（String、Int、Long、Boolean、Float、Double、StringSet）
- 支持 Xposed 模块集成
- 基于 Koin 依赖注入
- 基于 SharedPreferences 实现

## 模块说明

- **library**: 核心 KV 存储库
- **app**: 示例应用
- **xposed**: Xposed 模块实现

## 依赖

[![](https://jitpack.io/v/xihan123/kv-storage.svg)](https://jitpack.io/#xihan123/kv-storage)

```gradle
dependencies {
    implementation("com.github.xihan123:kv-storage:<JitPack-Version>")
}
```

## 使用方法

### 初始化

#### 模块端有UI（推荐方式）

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApp)
            androidLogger()
        }
        // 目标应用包名
        KVSyncManager.setTargetPackages("website.xihan.kv.storage")
    }
}
```

#### 宿主端

```kotlin
// 在初始化位置获取
val application = param.args[0] as? Application ?: return
startKoin {
    androidContext(application)
    androidLogger()
}
HostKVManager.init(enableSharedPreferencesCache = true, modulePackageName = BuildConfig.APPLICATION_ID)
```

### Xposed 模块集成

#### AndroidManifest.xml 声明

```xml
<provider 
    android:name="website.xihan.kv.KVContentProvider" 
    android:authorities="website.xihan.kv"
    android:enabled="true" 
    android:exported="true" />
```

### Kotlin属性委托用法

```kotlin
// 创建 KV 实例
object ModuleConfig : IKVOwner by KVOwner("SHARED_SETTINGS") {
    var switchEnable by kvBool()
    var textViewText by kvString("default")
    var count by kvInt(0)
    var ratio by kvDouble(1.0)
}

// 读写数据
ModuleConfig.switchEnable = true
ModuleConfig.textViewText = "123456"

// 检查键是否存在
if (ModuleConfig.containsKV("switchEnable")) { ... }

// 删除单个键
ModuleConfig.removeKV("count")

// 批量设置
ModuleConfig.putAllKV(mapOf(
    "switchEnable" to true,
    "textViewText" to "hello",
    "count" to 100
))

// 获取所有数据
val allData = ModuleConfig.getAllKV()

// 清空所有数据
ModuleConfig.clearAllKV()
```

### 直接操作

#### 模块端

```kotlin
// 存储数据
KVStorage.putBoolean("SHARED_SETTINGS", "switchEnable", true)
KVStorage.putString("SHARED_SETTINGS", "textViewText", "123456")
KVStorage.putDouble("SHARED_SETTINGS", "ratio", 3.14)

// 读取数据
val text = KVStorage.getString("SHARED_SETTINGS", "textViewText")
val enabled = KVStorage.getBoolean("SHARED_SETTINGS", "switchEnable")

// 检查和删除
if (KVStorage.contains("SHARED_SETTINGS", "textViewText")) {
    KVStorage.remove("SHARED_SETTINGS", "textViewText")
}

// 批量操作
KVStorage.putAll("SHARED_SETTINGS", mapOf(
    "switchEnable" to true,
    "textViewText" to "123456",
    "count" to 100,
    "ratio" to 3.14
))

// 清除所有数据
KVStorage.clearAll("SHARED_SETTINGS")
```

#### 宿主端

```kotlin
val helper = HostKVManager.createKVHelper("SHARED_SETTINGS")

// 读取数据
val textViewText = helper.getString("textViewText")
val switchEnable = helper.getBoolean("switchEnable")
val ratio = helper.getDouble("ratio")

// 写入数据
helper.putString("textViewText", "hello")
helper.putBoolean("switchEnable", false)

// 检查和删除
if (helper.contains("textViewText")) {
    helper.remove("textViewText")
}

// 批量操作
helper.putAll(mapOf(
    "switchEnable" to true,
    "textViewText" to "123456"
))

// 添加变化监听
helper.addChangeListener("switchEnable") { key, value ->
    Log.d("KV", "$key changed")
}

// 清除缓存
helper.clearCache()

// 清空所有数据
helper.clearAll()
```

### 批量查询

一次 IPC 调用查询多个键值，性能提升 10-100 倍：

```kotlin
// 宿主端
val keys = setOf("swithchEnable", "textViewText")
val map = HostKVManager.createKVHelper().getBatch(keys)
```

## 常见问题

### 1. Koin 依赖注入未初始化

**问题**: 使用时抛出 `KoinApplicationNotStartedException`

**解决方案**: 在使用前必须初始化

### 2. 跨进程数据不同步

**问题**: 在 Xposed 模块中修改数据后，宿主应用未收到更新

**解决方案**:

- 确保已调用 `HostKVManager.init()` 初始化广播接收器
- 设置目标包名: `KVSyncManager.setTargetPackages("com.example.host")`
- 检查广播权限是否正常
- 检查目标应用是否已安装且运行

### 3. ContentProvider 无法访问

**问题**: 宿主端无法通过 ContentProvider 读取数据

**解决方案**:

- 确保模块端已在 AndroidManifest.xml 中注册 `KVContentProvider`
- 检查 authority 是否为 `website.xihan.kv`
- 确保模块应用已安装且运行
- 清除目标应用数据

### 4. 数据类型不匹配

**问题**: 读取数据时类型转换异常

**解决方案**: 确保读写使用相同的数据类型方法

```kotlin
// 错误示例
KVStorage.putInt("settings", "value", 123)
val result = KVStorage.getString("settings", "value") // 类型不匹配

// 正确示例
KVStorage.putInt("settings", "value", 123)
val result = KVStorage.getInt("settings", "value")
```

## 注意事项

- ⚠️ 必须先初始化才能使用 KVStorage 和 HostKVManager
- ⚠️ 跨进程通信依赖 ContentProvider，确保模块应用已安装和已打开
- ⚠️ 不建议读写大量数据（建议单个 KV < 1MB）
- ⚠️ 所有操作已线程安全，但频繁跨进程调用仍有性能开销
