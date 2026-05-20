# Add project specific ProGuard rules here.
# By default, AGP will keep all classes/methods referenced by AndroidManifest.xml.

# Keep TTS engine callbacks
-keep class android.speech.tts.** { *; }

# Keep our notification listener and accessibility classes – referenced by
# system, not by app code, so R8 might otherwise strip them.
-keep class com.nbjiragale.upispeaker.service.UpiNotificationListener { *; }
-keep class com.nbjiragale.upispeaker.service.SpeakerForegroundService { *; }
-keep class com.nbjiragale.upispeaker.alarm.** { *; }
