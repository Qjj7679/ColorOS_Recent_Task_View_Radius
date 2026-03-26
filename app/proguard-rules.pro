-keep class java.lang.reflect.AnnotatedType { *; }
-keep class java.lang.reflect.Type { *; }

-keep class com.highcapable.yukihookapi.** { *; }
-keep class com.highcapable.kavaref.** { *; }
-keep class com.highcapable.kavaref.resolver.** { *; }
-keep class com.highcapable.kavaref.resolver.processor.** { *; }

-keep class **.toClass { *; }
-keep class **.hook { *; }
-keep class **.result { *; }

-dontwarn com.highcapable.**
-dontwarn java.lang.reflect.AnnotatedType