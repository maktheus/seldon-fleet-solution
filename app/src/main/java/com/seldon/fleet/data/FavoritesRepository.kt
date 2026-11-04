package com.seldon.fleet.data

import android.content.Context
import com.seldon.fleet.domain.FavoriteRepository

class FavoritesRepository(context: Context) : FavoriteRepository {
    private val preferences = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    override fun load(): Set<String> = preferences.getStringSet("ids", emptySet()).orEmpty()
    override fun save(ids: Set<String>) { preferences.edit().putStringSet("ids", ids).apply() }
}
