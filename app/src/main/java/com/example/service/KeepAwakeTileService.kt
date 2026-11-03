package com.example.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.R
import com.example.data.ScreenAwakeManager

@RequiresApi(Build.VERSION_CODES.N)
class KeepAwakeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        ScreenAwakeManager.toggle(this)
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isActive = ScreenAwakeManager.isAwake(this)

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isActive) {
                val remaining = ScreenAwakeManager.remainingSeconds.value
                if (remaining > 0) {
                    val m = remaining / 60
                    val s = remaining % 60
                    String.format("%02d:%02d", m, s)
                } else {
                    "Attivo"
                }
            } else {
                "Spento"
            }
        }
        tile.updateTile()
    }
}
