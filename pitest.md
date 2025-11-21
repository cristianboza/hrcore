# PIT Mutation Testing Configuration
# This file provides additional configuration for mutation testing

# Mutators to use (DEFAULTS is a good starting point)
# Available mutator groups:
# - DEFAULTS: Standard set of mutators (recommended)
# - STRONGER: More aggressive mutations
# - ALL: All available mutators (very slow)

# Target mutation score thresholds
# mutationThreshold: 70% (minimum mutation score)
# coverageThreshold: 60% (minimum line coverage)

# Excluded classes (automatically excluded):
# - Generated JPA metamodel classes (*_)
# - QueryDSL query classes (Q*)
# - Configuration classes
# - Main application class

# To run mutation testing:
# mvn org.pitest:pitest-maven:mutationCoverage

# To run with specific test:
# mvn org.pitest:pitest-maven:mutationCoverage -DtargetTests=ProfileControllerIntegrationTest

# Reports are generated in:
# target/pit-reports/YYYYMMDDHHMI/index.html
