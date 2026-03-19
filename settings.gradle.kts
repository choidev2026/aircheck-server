rootProject.name = "aircheck-server"

// Core modules
include(":core:domain")
include(":core:service")
include(":core:airkorea-adapter")
include(":core:openmeteo-adapter")
include(":core:kma-adapter")
include(":core:persistence-adapter")
include(":core:fcm-adapter")

// Feature modules
include(":feature:weather")
include(":feature:admin")

// App (composition root)
include(":app")
