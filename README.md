# ColorOS Recent Task View Radius

为 ColorOS 桌面（Launcher）的最近任务卡片圆角提供可调节的 Xposed/YukiHookAPI 模块。UI 通过 ContentProvider 写入配置，Hook 在 Launcher 中读取并替换 `recent_task_view_radius`。

## 功能
- 调节最近任务卡片圆角（dp）
- 取值范围：16–130 dp
- 调整后需重启桌面或重启设备生效

## 项目结构
```
app/src/main/kotlin/com/radius/optimization
├─ RadiusConfig.kt          # 常量与配置 URI
├─ RadiusConfigProvider.kt  # Provider 存取
├─ MainHook.kt              # Launcher Hook
└─ MainActivity.kt          # Compose UI
```

## 配置通道
- authority: `com.radius.optimization.config`
- path: `radius_dp`
- URI: `content://com.radius.optimization.config/radius_dp`

## 构建
```bash
./gradlew assembleDebug --no-daemon
```
APK 输出：
```
app/build/outputs/apk/debug/app-debug.apk
```

## 说明
- Hook 侧已做进程内缓存，首次读取后缓存；重启桌面会重新读取。
- 使用方法级 Hook（`Resources.getDimensionPixelSize(int)`）替换 `recent_task_view_radius`。
