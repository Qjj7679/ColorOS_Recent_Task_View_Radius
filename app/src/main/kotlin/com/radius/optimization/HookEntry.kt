package com.radius.optimization

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

@InjectYukiHookWithXposed(
    sourcePath = "src/main",
    modulePackageName = "com.radius.optimization",
    entryClassName = "HookEntryXposed"
)
object HookEntry : IYukiHookXposedInit {
    override fun onHook() = encase(MainHook)
}
