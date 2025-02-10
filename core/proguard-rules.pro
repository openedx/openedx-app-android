# Prevent shrinking, optimization, and obfuscation of the library when consumed by other modules.
# This ensures that all classes and methods remain available for use by the consumer of the library.
# Disabling these steps at the library level is important because the main app module will handle
# shrinking, optimization, and obfuscation for the entire application, including this library.
-dontshrink
-dontoptimize
-dontobfuscate
