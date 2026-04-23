# ColorOS Recent Task View Radius

![License](https://img.shields.io/github/license/Qjj7679/ColorOS_Recent_Task_View_Radius?style=flat-square)

为 ColorOS 桌面（Launcher）的最近任务卡片圆角提供可调节的 Xposed 模块。UI 通过 ContentProvider 写入配置，Hook 在 Launcher 中读取并替换 `recent_task_view_radius`。

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

## 更新日志
- **v2.3**: 修复重启后仍可能回默认值的问题；Hook 增加本地 DPS 兜底读取（provider -> local backup -> default）；配置变更自动失效缓存并刷新。
- **v2.2**: 修复系统重启后圆角值恢复默认的问题；支持 Direct Boot；迁移配置到设备保护存储。
- **v2.1**: 调整最近任务卡片圆角（dp），取值范围 16–130 dp。

## 构建
```bash
./gradlew assembleDebug --no-daemon
```
APK 输出：
```
app/build/outputs/apk/debug/app-debug.apk
```

## 说明
- **重要**：升级 v2.2 后，请在 App 中重新保存一次配置以迁移到新存储区。
- Hook 侧已做进程内缓存，只有成功读取配置后才会缓存；重启桌面会重新读取。
- 使用方法级 Hook（`Resources.getDimensionPixelSize(int)`）替换 `recent_task_view_radius`。

## License
MIT
