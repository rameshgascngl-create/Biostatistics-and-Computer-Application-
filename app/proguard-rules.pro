# kotlinx.serialization models are looked up by generated serializers.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.gasc.zoology.biostat.**$$serializer { *; }
-keepclassmembers class com.gasc.zoology.biostat.** { *** Companion; }
-keepclasseswithmembers class com.gasc.zoology.biostat.** { kotlinx.serialization.KSerializer serializer(...); }
