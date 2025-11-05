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

```gradle
dependencies {
    implementation("com.github.xihan123:kv-storage:1.1")
}
```

## 使用方法

### Xposed 模块集成

#### 声明

```xml

<provider android:name="website.xihan.kv.KVContentProvider" android:authorities="website.xihan.kv"
    android:enabled="true" android:exported="true" />
```

依赖Koin注入，在初始化的时候使用以下代码(具体参考模块Demo)，接下来就可以在任意地方使用=>大概:)

```kotlin
startKoin {
    androidContext(application)
    androidLogger()
}
HostKVManager.init()
```

### Kotlin属性委托用法

```kotlin
// 创建 KV 实例
object ModuleConfig : IKVOwner by KVOwner("SHARED_SETTINGS") {
    var swithchEnable by kvBool()
    var textViewText by kvString()
}

ModuleConfig.swithchEnable = true
ModuleConfig.textViewText = "123456"
```

### 直接操作

```kotlin
// 存储数据
KVStorage.putBoolean("SHARED_SETTINGS", "swithchEnable", true)
KVStorage.putString("SHARED_SETTINGS", "textViewText", "123456")

// 读取数据
val textViewText = HostKVManager.createKVHelper("SHARED_SETTINGS").getString("textViewText")
val swithchEnable = HostKVManager.createKVHelper("SHARED_SETTINGS").getBoolean("swithchEnable")

// 批量操作
KVStorage.putAll(
    "SHARED_SETTINGS", mapOf(
        "swithchEnable" to true,
        "textViewText" to "123456"
    )
)

// 清除所有数据
KVStorage.clearAll("SHARED_SETTINGS")
```

## 常见问题

### 1. Koin 依赖注入未初始化

**问题**: 使用时抛出 `KoinApplicationNotStartedException`

**解决方案**: 在使用前必须初始化 Koin

```kotlin
startKoin {
    androidContext(application)
    androidLogger()
    modules(module {
        single<Context> { androidContext() }
    })
}
```

### 2. 跨进程数据不同步

**问题**: 在 Xposed 模块中修改数据后，宿主应用未收到更新

**解决方案**:

- 确保已调用 `HostKVManager.init()` 初始化广播接收器
- 检查广播权限是否正常
- Android 12+ 需要 `RECEIVER_EXPORTED` 标志（已自动处理）

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

- ⚠️ 必须先初始化 Koin 才能使用 KVStorage 和 HostKVManager
- ⚠️ 跨进程通信依赖 ContentProvider，确保模块应用已安装和已打开
- ⚠️ Double 类型使用字符串存储，可能存在精度问题
- ⚠️ StringSet 在某些 Android 版本上可能返回不可变集合
- ⚠️ 不建议读写大量数据