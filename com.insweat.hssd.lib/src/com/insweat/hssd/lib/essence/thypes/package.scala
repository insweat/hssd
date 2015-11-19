package com.insweat.hssd.lib.essence

import scala.collection.immutable.HashMap

package object thypes {
    private var _dynamicCompanions: List[DynamicThypeCompanion] = 
        List(ReferenceThype, ArrayThype, MapThype)

    def register(dtc: DynamicThypeCompanion) {
        _dynamicCompanions ::= dtc
    }

    def dynamicCompanions: Traversable[DynamicThypeCompanion] =
        _dynamicCompanions
}
