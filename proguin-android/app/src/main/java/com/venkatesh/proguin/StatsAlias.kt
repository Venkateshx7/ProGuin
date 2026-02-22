package com.venkatesh.proguin

// ✅ Fixes "Unresolved reference: StatsStore" in MainActivity WITHOUT changing your huge file.
// MainActivity can keep calling StatsStore(context).levelInfo()
typealias StatsStore = com.venkatesh.proguin.data.StatsStore
