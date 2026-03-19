rootProject.name = "aircheck-server"

// Core modules
include(":core:domain")
include(":core:service")
include(":core:airkorea")
include(":core:openmeteo")
include(":core:persistence")
include(":core:fcm")

// Feature modules
include(":feature:weather")
include(":feature:admin")

// App (composition root)
include(":app")
